#include <Arduino.h>
#include <math.h>
#include <string.h>
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

static OneWire oneWire(TEMP_SENSOR_PIN);
static DallasTemperature sensors(&oneWire);
static HX711 scale;

// Broadcast address: outer unit receives without us knowing its MAC.
static uint8_t broadcastMac[6] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};

static uint8_t readDoorOpen() {
    return (digitalRead(DOOR_SENSOR_PIN) == HIGH) ? 1 : 0;
}

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

static float inventoryPercentFromWeight(float weightG) {
    float estimatedPercent = (weightG / FULL_BOTTLE_WEIGHT_G) * 100.0f;
    if (estimatedPercent > 100.0f) estimatedPercent = 100.0f;
    if (estimatedPercent < 0.0f) estimatedPercent = 0.0f;
    return estimatedPercent;
}

static bool sendInnerPacket(uint32_t& seq,
                            uint8_t doorOpen,
                            float temperatureC,
                            float weightG,
                            float estimatedPercent,
                            float batteryVoltageV,
                            uint8_t batteryPercent,
                            const char* reason) {
    InnerPacket pkt = {};
    pkt.magic = INNER_MAGIC;
    pkt.seq = seq++;
    pkt.doorOpen = doorOpen;
    pkt.temperatureC = temperatureC;
    pkt.weightG = weightG;
    pkt.estimatedPercent = estimatedPercent;
    pkt.batteryVoltageV = batteryVoltageV;
    pkt.batteryPercent = batteryPercent;

    const uint8_t burstCount = (strcmp(reason, "door") == 0)
                                   ? ESPNOW_DOOR_BURST_COUNT
                                   : ESPNOW_SAMPLE_BURST_COUNT;
    bool sentAny = false;
    esp_err_t lastResult = ESP_OK;

    for (uint8_t i = 0; i < burstCount; ++i) {
        lastResult = esp_now_send(
            broadcastMac,
            reinterpret_cast<uint8_t*>(&pkt),
            sizeof(InnerPacket));

        if (lastResult == ESP_OK) {
            sentAny = true;
        }

        if ((i + 1) < burstCount) {
            vTaskDelay(pdMS_TO_TICKS(ESPNOW_BURST_GAP_MS));
        }
    }

    if (sentAny) {
        Serial.printf("[Sensors] Sent seq=%u reason=%s burst=%u temp=%.2fC weight=%.1fg "
                      "percent=%.1f%% door=%s battery=%.2fV/%u%%\n",
                      pkt.seq,
                      reason,
                      burstCount,
                      isnan(pkt.temperatureC) ? 0.0f : pkt.temperatureC,
                      pkt.weightG,
                      pkt.estimatedPercent,
                      pkt.doorOpen ? "OPEN" : "CLOSED",
                      pkt.batteryVoltageV,
                      pkt.batteryPercent);
        return true;
    }

    Serial.printf("[Sensors] esp_now_send error: %d\n", lastResult);
    return false;
}

static void onEspNowSent(const uint8_t* mac, esp_now_send_status_t status) {
    (void)mac;
    if (status != ESP_NOW_SEND_SUCCESS) {
        Serial.println("[ESP-NOW] Send failed");
    }
}

static void initEspNow() {
    if (esp_now_init() != ESP_OK) {
        Serial.println("[ESP-NOW] Init failed - halting");
        while (true) {
            vTaskDelay(pdMS_TO_TICKS(1000));
        }
    }
    esp_now_register_send_cb(onEspNowSent);

    esp_now_peer_info_t peer = {};
    memcpy(peer.peer_addr, broadcastMac, 6);
    peer.channel = 0; // Follow the WiFi radio channel locked in main.cpp.
    peer.encrypt = false;
    if (esp_now_add_peer(&peer) != ESP_OK) {
        Serial.println("[ESP-NOW] Add broadcast peer failed - halting");
        while (true) {
            vTaskDelay(pdMS_TO_TICKS(1000));
        }
    }
    Serial.printf("[ESP-NOW] Initialised on channel %d, broadcast peer registered\n",
                  (int)WiFi.channel());
}

void sensorSamplingTask(void* pvParams) {
    (void)pvParams;

    sensors.begin();
    sensors.setResolution(12);

    scale.begin(HX711_DOUT_PIN, HX711_CLK_PIN);
    scale.set_scale(LOAD_CELL_CALIBRATION);
    scale.tare();

    pinMode(DOOR_SENSOR_PIN, INPUT_PULLUP);
    pinMode(BATTERY_ADC_PIN, INPUT);
    analogSetPinAttenuation(BATTERY_ADC_PIN, ADC_11db);

    uint32_t seq = 0;
    float lastTempC = NAN;
    float lastValidWeight = 0.0f;
    float lastEstimatedPercent = 0.0f;
    float lastBatteryVoltageV = readBatteryVoltageV();
    uint8_t lastBatteryPercent = batteryPercentFromVoltage(lastBatteryVoltageV);

    uint8_t stableDoorOpen = readDoorOpen();
    uint8_t rawDoorOpen = stableDoorOpen;
    uint32_t rawDoorChangedAtMs = millis();
    uint32_t lastFullSampleAtMs = 0;
    uint32_t lastHeartbeatAtMs = 0;

    initEspNow();

    Serial.println("[Sensors] Task started with fast door trigger");
    sendInnerPacket(seq,
                    stableDoorOpen,
                    lastTempC,
                    lastValidWeight,
                    lastEstimatedPercent,
                    lastBatteryVoltageV,
                    lastBatteryPercent,
                    "boot");

    for (;;) {
        uint32_t now = millis();

        uint8_t currentRawDoorOpen = readDoorOpen();
        if (currentRawDoorOpen != rawDoorOpen) {
            rawDoorOpen = currentRawDoorOpen;
            rawDoorChangedAtMs = now;
        }

        if (rawDoorOpen != stableDoorOpen &&
            (now - rawDoorChangedAtMs) >= DOOR_EVENT_DEBOUNCE_MS) {
            stableDoorOpen = rawDoorOpen;
            lastBatteryVoltageV = readBatteryVoltageV();
            lastBatteryPercent = batteryPercentFromVoltage(lastBatteryVoltageV);

            if (sendInnerPacket(seq,
                                stableDoorOpen,
                                lastTempC,
                                lastValidWeight,
                                lastEstimatedPercent,
                                lastBatteryVoltageV,
                                lastBatteryPercent,
                                "door")) {
                lastHeartbeatAtMs = millis();
            }
        }

        now = millis();
        if (lastFullSampleAtMs == 0 || (now - lastFullSampleAtMs) >= SAMPLE_INTERVAL_MS) {
            sensors.requestTemperatures();
            lastTempC = sensors.getTempCByIndex(0);
            if (lastTempC == DS18B20_ERROR_TEMP || lastTempC == DEVICE_DISCONNECTED_C) {
                lastTempC = NAN;
                Serial.println("[Sensors] DS18B20 error - reporting NAN");
            }

            if (scale.is_ready()) {
                float reading = scale.get_units(HX711_AVERAGES);
                lastValidWeight = (reading < 0.0f) ? 0.0f : reading;
            }

            lastEstimatedPercent = inventoryPercentFromWeight(lastValidWeight);
            lastBatteryVoltageV = readBatteryVoltageV();
            lastBatteryPercent = batteryPercentFromVoltage(lastBatteryVoltageV);
            stableDoorOpen = readDoorOpen();
            rawDoorOpen = stableDoorOpen;
            rawDoorChangedAtMs = millis();

            if (sendInnerPacket(seq,
                                stableDoorOpen,
                                lastTempC,
                                lastValidWeight,
                                lastEstimatedPercent,
                                lastBatteryVoltageV,
                                lastBatteryPercent,
                                "sample")) {
                lastHeartbeatAtMs = millis();
            }
            lastFullSampleAtMs = millis();
        }

        now = millis();
        if ((now - lastHeartbeatAtMs) >= INNER_HEARTBEAT_MS) {
            if (sendInnerPacket(seq,
                                stableDoorOpen,
                                lastTempC,
                                lastValidWeight,
                                lastEstimatedPercent,
                                lastBatteryVoltageV,
                                lastBatteryPercent,
                                "heartbeat")) {
                lastHeartbeatAtMs = millis();
            }
        }

        vTaskDelay(pdMS_TO_TICKS(DOOR_POLL_INTERVAL_MS));
    }
}
