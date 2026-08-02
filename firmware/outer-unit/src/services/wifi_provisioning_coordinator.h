#pragma once

#include <Arduino.h>

enum class WifiProvisioningRuntimeState : uint8_t {
    IDLE = 0,
    WAITING_FOR_INNER,
    STAGING_INNER,
    APPLYING,
    OUTER_CONNECTED,
    SUCCESS,
    FAILED
};

struct WifiProvisioningRuntimeStatus {
    WifiProvisioningRuntimeState state;
    char outerStatus[16];
    char innerStatus[16];
    char message[64];
    uint32_t configurationVersion;
};

bool setupWifiProvisioningCoordinator();
void observeInnerSensorMac(const uint8_t senderMac[6]);
void handleOuterWifiProvisioningPacket(
    const uint8_t senderMac[6],
    const uint8_t* data,
    size_t length);
void getWifiProvisioningRuntimeStatus(
    WifiProvisioningRuntimeStatus& status);
