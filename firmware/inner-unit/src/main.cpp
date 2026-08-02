#include <Arduino.h>
#include <Preferences.h>
#include <WiFi.h>
#include <esp_wifi.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>
#include "config/app_config.h"
#include "services/wifi_provisioning_service.h"
#include "../../common/services/wifi_credential_manager.h"

// Forward declaration for task defined in tasks/
void sensorSamplingTask(void* pvParams);

// ---- WiFi channel lock --------------------------------------------------- //
// Inner unit does not use WiFi for data. WiFi is started only to lock the
// radio onto ESPNOW_CHANNEL so ESP-NOW broadcasts reach the outer unit.
// After locking the channel, WiFi is disconnected but the radio stays in
// STA mode — required for ESP-NOW to operate.

namespace {
WifiCredentialManager credentialManager(
    "diasmart-wifi",
    WIFI_SSID,
    WIFI_PASSWORD);

const char* credentialSourceName(WifiCredentialSource source) {
    switch (source) {
        case WifiCredentialSource::NVS_CURRENT:
            return "saved";
        case WifiCredentialSource::DEVELOPMENT_FALLBACK:
            return "development fallback";
        default:
            return "unavailable";
    }
}
}

static void lockEspNowChannel() {
    WifiConfiguration configuration = {};
    WifiCredentialSource source = WifiCredentialSource::NONE;
    if (!credentialManager.loadActive(configuration, source)) {
        Serial.println("[WiFi] No valid credentials - using recovery channel");
        WiFi.mode(WIFI_STA);
        esp_wifi_set_channel(ESPNOW_CHANNEL, WIFI_SECOND_CHAN_NONE);
        return;
    }

    WiFi.mode(WIFI_STA);
    if (configuration.openNetwork != 0) {
        WiFi.begin(configuration.ssid);
    } else {
        WiFi.begin(configuration.ssid, configuration.password);
    }

    Serial.printf(
        "[WiFi] Connecting with %s credentials (version=%lu)",
        credentialSourceName(source),
        static_cast<unsigned long>(configuration.configurationVersion));
    clearWifiConfiguration(configuration);

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

    uint8_t lockedChannel = WiFi.channel();
    Serial.printf("\n[WiFi] Connected on channel %d - locking ESP-NOW radio\n",
                  lockedChannel);
    // Disconnect from AP but keep radio in STA mode for ESP-NOW
    WiFi.disconnect(false);
    esp_wifi_set_channel(lockedChannel, WIFI_SECOND_CHAN_NONE);
    Serial.printf("[WiFi] ESP-NOW channel locked to %d\n", lockedChannel);
}

// -------------------------------------------------------------------------- //

void setup() {
    Serial.begin(SERIAL_BAUD);
    delay(200);
    Serial.println("=== Dia-Smart Inner Unit Starting ===");

    lockEspNowChannel();

    if (!setupInnerWifiProvisioningService()) {
        Serial.println("[Main] Wi-Fi provisioning service failed - halting");
        while (true) {
            vTaskDelay(pdMS_TO_TICKS(1000));
        }
    }

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
