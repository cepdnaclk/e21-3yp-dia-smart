#include "wifi_manager.h"
#include <WiFi.h>
#include <esp_wifi.h>
#include "config/app_config.h"
#include "../../../common/services/wifi_credential_manager.h"

namespace {
WifiCredentialManager credentialManager(
    "diasmart-wifi",
    WIFI_SSID,
    WIFI_PASSWORD);
volatile bool usingDevelopmentFallback = false;

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

bool connectUsingWifiConfiguration(
    const WifiConfiguration& configuration,
    uint32_t timeoutMs,
    bool enableAutoReconnectOnFailure
) {
    if (validateStoredWifiConfiguration(configuration) !=
        WifiValidationResult::VALID) {
        Serial.println("[WiFi] Credential validation failed");
        return false;
    }

    const wifi_mode_t currentMode = WiFi.getMode();
    const bool preserveSoftAp =
        currentMode == WIFI_AP ||
        currentMode == WIFI_AP_STA;
    WiFi.setAutoReconnect(false);
    WiFi.disconnect(false);
    WiFi.mode(preserveSoftAp ? WIFI_AP_STA : WIFI_STA);
    WiFi.setHostname("Dia-Smart-Outer-Unit");

    if (configuration.openNetwork != 0) {
        WiFi.begin(configuration.ssid);
    } else {
        WiFi.begin(configuration.ssid, configuration.password);
    }

    const uint32_t startMs = millis();
    while (WiFi.status() != WL_CONNECTED &&
           (millis() - startMs) < timeoutMs) {
        delay(250);
        Serial.print(".");
    }

    if (WiFi.status() == WL_CONNECTED) {
        Serial.println("\n--- Wi-Fi Connected Successfully ---");
        Serial.print("SSID: ");
        Serial.println(WiFi.SSID());
        Serial.print("IP Address: ");
        Serial.println(WiFi.localIP());
        Serial.print("RSSI (Signal Strength): ");
        Serial.print(WiFi.RSSI());
        Serial.println(" dBm");
        Serial.println("------------------------------------");
        WiFi.setAutoReconnect(true);
        return true;
    } else {
        Serial.println("\n[ERROR] Wi-Fi connection failed");
        esp_wifi_set_channel(ESPNOW_CHANNEL, WIFI_SECOND_CHAN_NONE);
        Serial.printf("[WiFi] Forced ESP-NOW fallback channel %d\n", ESPNOW_CHANNEL);
    }
    WiFi.setAutoReconnect(enableAutoReconnectOnFailure);
    return false;
}

bool loadActiveWifiConfiguration(
    WifiConfiguration& configuration,
    WifiCredentialSource& source
) {
    return credentialManager.loadActive(configuration, source);
}

WifiCredentialStore& wifiCredentialStore() {
    return credentialManager.store();
}

bool connectUsingActiveWifiConfiguration(
    bool enableAutoReconnectOnFailure
) {
    WifiConfiguration configuration = {};
    WifiCredentialSource source = WifiCredentialSource::NONE;
    if (!loadActiveWifiConfiguration(configuration, source)) {
        Serial.println("[WiFi] No valid credentials available");
        esp_wifi_set_channel(ESPNOW_CHANNEL, WIFI_SECOND_CHAN_NONE);
        usingDevelopmentFallback = false;
        return false;
    }

    Serial.printf(
        "\n[WiFi] Connecting with %s credentials (version=%lu)",
        credentialSourceName(source),
        static_cast<unsigned long>(configuration.configurationVersion));

    bool connected = connectUsingWifiConfiguration(
        configuration,
        WIFI_CONNECT_TIMEOUT_MS,
        enableAutoReconnectOnFailure);
    clearWifiConfiguration(configuration);

    if (!connected && source == WifiCredentialSource::NVS_CURRENT) {
        Serial.println(
            "[WiFi] Saved credentials failed; trying development fallback");
        if (credentialManager.loadDevelopmentFallback(configuration)) {
            connected = connectUsingWifiConfiguration(
                configuration,
                WIFI_CONNECT_TIMEOUT_MS,
                enableAutoReconnectOnFailure);
        }
        clearWifiConfiguration(configuration);
        usingDevelopmentFallback = connected;
        return connected;
    }

    usingDevelopmentFallback =
        source == WifiCredentialSource::DEVELOPMENT_FALLBACK;
    return connected;
}

bool isWifiUsingDevelopmentFallback() {
    return usingDevelopmentFallback;
}

void setupWiFi()
{
    connectUsingActiveWifiConfiguration();
    Serial.printf("[WiFi] Current radio channel: %d\n", WiFi.channel());
}

bool isWiFiConnected()
{
    return WiFi.status() == WL_CONNECTED;
}
