#include "wifi_manager.h"
#include <WiFi.h>
#include <esp_wifi.h>
#include "config/app_config.h"

void setupWiFi()
{
    Serial.print("\nConnecting to Wi-Fi: ");
    Serial.println(WIFI_SSID);

    // Set ESP32 to Station mode (client)
    WiFi.mode(WIFI_STA);
    
    // Set a custom hostname for easier identification on your router
    WiFi.setHostname("Dia-Smart-Outer-Unit");

    // Initiate connection
    WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

    // Wait for connection
    int attempts = 0;
    while (WiFi.status() != WL_CONNECTED && attempts < 20)
    {
        delay(500);
        Serial.print(".");
        attempts++;
    }

    if (WiFi.status() == WL_CONNECTED)
    {
        Serial.println("\n--- Wi-Fi Connected Successfully ---");
        Serial.print("IP Address: ");
        Serial.println(WiFi.localIP());
        Serial.print("RSSI (Signal Strength): ");
        Serial.print(WiFi.RSSI());
        Serial.println(" dBm");
        Serial.println("------------------------------------");
    }
    else
    {
        Serial.println("\n[ERROR] Wi-Fi Connection Failed. Will retry automatically.");
        esp_wifi_set_channel(ESPNOW_CHANNEL, WIFI_SECOND_CHAN_NONE);
        Serial.printf("[WiFi] Forced ESP-NOW fallback channel %d\n", ESPNOW_CHANNEL);
    }
    Serial.printf("[WiFi] Current radio channel: %d\n", WiFi.channel());
    
    // Enable auto-reconnect in the background if the signal drops
    WiFi.setAutoReconnect(true);
}

bool isWiFiConnected()
{
    return WiFi.status() == WL_CONNECTED;
}
