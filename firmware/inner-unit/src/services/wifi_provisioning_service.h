#pragma once

#include <Arduino.h>

bool setupInnerWifiProvisioningService();
bool sendInnerSensorPacket(const uint8_t* data, size_t length);
bool isInnerWifiSwitchInProgress();
