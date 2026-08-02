#pragma once

#include <Arduino.h>

bool setupWifiCommandService();
bool enqueueWifiCommandPayload(
    const uint8_t* payload,
    size_t payloadLength);
void processPendingWifiCommand();
