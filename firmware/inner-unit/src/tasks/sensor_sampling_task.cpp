#include <Arduino.h>
#include <math.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include <HX711.h>
#include <esp_now.h>
#include <WiFi.h>
#include <esp_wifi.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include "../config/app_config.h"
#include "../models/inner_event.h"

// ---- Sensor objects ------------------------------------------------------- //

static OneWire           oneWire(TEMP_SENSOR_PIN);
static DallasTemperature sensors(&oneWire);
static HX711             scale;

// ---- ESP-NOW -------------------------------------------------------------- //

// Broadcast address — outer unit receives without us knowing its MAC
static uint8_t broadcastMac[6] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

static float readBatteryVoltageV() {
    uint32_t totalMv = 0;
    for (uint8_t i = 0; i < BATTERY_ADC_SAMPLES; ++i) {
        totalMv += analogReadMilliVolts(BATTERY_ADC_PIN);
        delay(2);
    }

    float adcMv = (float)totalMv / BATTERY_ADC_SAMPLES;
    float dividerRatio = (BATTERY_DIVIDER_TOP_OHMS + BATTERY_DIVIDER_BOTTOM_OHMS) /
                         BATTERY_DIVIDER_BOTTOM_OHMS;
    return (adcMv * dividerRatio) / 1000.0f;
}

static uint8_t batteryPercentFromVoltage(float batteryVoltageV) {
    int batteryMv = (int)(batteryVoltageV * 1000.0f + 0.5f);
    if (batteryMv <= BATTERY_EMPTY_MV) return 0;
    if (batteryMv >= BATTERY_FULL_MV) return 100;

    return (uint8_t)(((batteryMv - BATTERY_EMPTY_MV) * 100) /
                     (BATTERY_FULL_MV - BATTERY_EMPTY_MV));
}

static void onEspNowSent(const uint8_t* mac, esp_now_send_status_t status) {
    // Optional: log send result
    if (status != ESP_NOW_SEND_SUCCESS) {
        Serial.println("[ESP-NOW] Send failed");
    }
}

static void initEspNow() {
    if (esp_now_init() != ESP_OK) {
        Serial.println("[ESP-NOW] Init failed — halting");
        while (true) { vTaskDelay(pdMS_TO_TICKS(1000)); }
    }
    esp_now_register_send_cb(onEspNowSent);

    // Register broadcast peer.
    // peer.channel = 0 means "use whatever channel the WiFi radio is on".
    // This is critical — if WiFi connected on ch9, a hardcoded ch1 peer will fail.
    esp_now_peer_info_t peer = {};
    memcpy(peer.peer_addr, broadcastMac, 6);
    peer.channel = 0;   // 0 = follow current WiFi channel automatically
    peer.encrypt = false;
    if (esp_now_add_peer(&peer) != ESP_OK) {
        Serial.println("[ESP-NOW] Add broadcast peer failed — halting");
        while (true) { vTaskDelay(pdMS_TO_TICKS(1000)); }
    }
    Serial.printf("[ESP-NOW] Initialised on channel %d, broadcast peer registered\n",
                  (int)WiFi.channel());
}

// ---- Task ----------------------------------------------------------------- //

void sensorSamplingTask(void* pvParams) {
    // ---- Sensor init ---------------------------------------------------- //
    sensors.begin();
    sensors.setResolution(12);   // 12-bit = 0.0625°C resolution

    scale.begin(HX711_DOUT_PIN, HX711_CLK_PIN);
    scale.set_scale(LOAD_CELL_CALIBRATION);
    scale.tare();                // zero the scale on startup

    pinMode(DOOR_SENSOR_PIN, INPUT_PULLUP);
    pinMode(BATTERY_ADC_PIN, INPUT);
    analogSetPinAttenuation(BATTERY_ADC_PIN, ADC_11db);

    // ---- Keep last valid weight in case HX711 temporarily not ready ----- //
    float lastValidWeight = 0.0f;

    // ---- Sequence counter ----------------------------------------------- //
    uint32_t seq = 0;

    // ESP-NOW must be initialised inside the task AFTER WiFi is connected
    // (main.cpp locks the channel first, then spawns this task).
    initEspNow();

    Serial.println("[Sensors] Task started");

    for (;;) {
        // ---- DS18B20 temperature ---------------------------------------- //
        sensors.requestTemperatures();
        float tempC = sensors.getTempCByIndex(0);

        // 85.0 = parasite power error, -127.0 = not found
        if (tempC == DS18B20_ERROR_TEMP || tempC == DEVICE_DISCONNECTED_C) {
            tempC = NAN;
            Serial.println("[Sensors] DS18B20 error — reporting NAN");
        }

        // ---- HX711 load cell -------------------------------------------- //
        if (scale.is_ready()) {
            float reading = scale.get_units(HX711_AVERAGES);
            // Clamp negatives to zero (tare drift)
            lastValidWeight = (reading < 0.0f) ? 0.0f : reading;
        }
        // If not ready, lastValidWeight carries over — never send garbage

        // ---- Reed switch ------------------------------------------------- //
        uint8_t doorOpen = (digitalRead(DOOR_SENSOR_PIN) == HIGH) ? 1 : 0;

        // ---- Inventory percent ------------------------------------------ //
        float estimatedPercent = (lastValidWeight / FULL_BOTTLE_WEIGHT_G) * 100.0f;
        if (estimatedPercent > 100.0f) estimatedPercent = 100.0f;
        if (estimatedPercent < 0.0f)   estimatedPercent = 0.0f;

        // ---- Battery monitor -------------------------------------------- //
        float batteryVoltageV = readBatteryVoltageV();
        uint8_t batteryPercent = batteryPercentFromVoltage(batteryVoltageV);

        // ---- Build and send InnerPacket ---------------------------------- //
        InnerPacket pkt;
        pkt.magic            = INNER_MAGIC;
        pkt.seq              = seq++;
        pkt.doorOpen         = doorOpen;
        pkt.temperatureC     = tempC;
        pkt.weightG          = lastValidWeight;
        pkt.estimatedPercent = estimatedPercent;
        pkt.batteryVoltageV  = batteryVoltageV;
        pkt.batteryPercent   = batteryPercent;

        esp_err_t result = esp_now_send(
            broadcastMac,
            reinterpret_cast<uint8_t*>(&pkt),
            sizeof(InnerPacket)
        );

        if (result == ESP_OK) {
            Serial.printf("[Sensors] Sent seq=%u  temp=%.2f°C  weight=%.1fg  "
                          "percent=%.1f%%  door=%s\n",
                          pkt.seq,
                          isnan(pkt.temperatureC) ? 0.0f : pkt.temperatureC,
                          pkt.weightG,
                          pkt.estimatedPercent,
                          pkt.doorOpen ? "OPEN" : "CLOSED");
            Serial.printf("[Sensors] Battery %.2fV (%u%%)\n",
                          pkt.batteryVoltageV,
                          pkt.batteryPercent);
        } else {
            Serial.printf("[Sensors] esp_now_send error: %d\n", result);
        }

        vTaskDelay(pdMS_TO_TICKS(SAMPLE_INTERVAL_MS));
    }
}
