#pragma once

#include <Arduino.h>

bool setupWifiCommandService();
bool enqueueWifiCommandPayload(
    const uint8_t* payload,
    size_t payloadLength);
bool queueWifiCommandStatus(
    const char* commandId,
    uint32_t configurationVersion,
    const char* status,
    const char* message);
bool queueInnerWifiConfigurationResult(
    const char* commandId,
    const char* status,
    const uint8_t ipAddress[4],
    const char* message);
void processPendingWifiCommand();
