#include <Arduino.h>
#include <WiFi.h>
#include <esp_wifi.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include "config/app_config.h"

// Forward declaration for task defined in tasks/
void sensorSamplingTask(void* pvParams);

// ---- WiFi channel lock --------------------------------------------------- //
// Inner unit does not use WiFi for data. WiFi is started only to lock the
// radio onto ESPNOW_CHANNEL so ESP-NOW broadcasts reach the outer unit.
// After locking the channel, WiFi is disconnected but the radio stays in
// STA mode — required for ESP-NOW to operate.

static void lockEspNowChannel() {
    WiFi.mode(WIFI_STA);
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    Serial.printf("[WiFi] Connecting to lock channel %d", ESPNOW_CHANNEL);

    uint32_t start = millis();
    while (WiFi.status() != WL_CONNECTED) {
        if ((millis() - start) >= WIFI_CONNECT_TIMEOUT_MS) {
            Serial.println("\n[WiFi] Timeout — setting channel manually");
            // Fallback: force channel without being associated to an AP
            esp_wifi_set_channel(ESPNOW_CHANNEL, WIFI_SECOND_CHAN_NONE);
            WiFi.disconnect(false);   // disconnect but keep STA mode alive
            return;
        }
        vTaskDelay(pdMS_TO_TICKS(250));
        Serial.print(".");
    }

    Serial.printf("\n[WiFi] Connected on channel %d — disconnecting\n",
                  WiFi.channel());
    // Disconnect from AP but keep radio in STA mode for ESP-NOW
    WiFi.disconnect(false);
}

// -------------------------------------------------------------------------- //

void setup() {
    Serial.begin(SERIAL_BAUD);
    delay(200);
    Serial.println("=== Dia-Smart Inner Unit Starting ===");

    lockEspNowChannel();

    // Sensor sampling + ESP-NOW TX — runs on Core 1 (sensor-heavy)
    xTaskCreatePinnedToCore(
        sensorSamplingTask,
        "SensorSample",
        8192,
        nullptr,
        1,
        nullptr,
        1    // Core 1
    );

    Serial.println("[Main] Sensor task started");
}

// FreeRTOS takes over — loop() intentionally idle.
void loop() {
    vTaskDelay(pdMS_TO_TICKS(10000));
}
