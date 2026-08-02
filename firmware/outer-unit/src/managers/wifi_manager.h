#ifndef WIFI_MANAGER_H
#define WIFI_MANAGER_H

#include <Arduino.h>
#include "../../../common/models/wifi_configuration.h"
#include "../../../common/services/wifi_credential_store.h"

void setupWiFi();
bool isWiFiConnected();
bool connectUsingWifiConfiguration(
    const WifiConfiguration& configuration,
    uint32_t timeoutMs);
bool loadActiveWifiConfiguration(
    WifiConfiguration& configuration,
    WifiCredentialSource& source);
WifiCredentialStore& wifiCredentialStore();

#endif
