#pragma once

#include <Arduino.h>

void prepareInnerWifiChannel();
bool setupInnerWifiProvisioningService();
bool sendInnerSensorPacket(const uint8_t* data, size_t length);
