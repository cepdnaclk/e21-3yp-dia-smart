#pragma once

#include <stdint.h>

struct DisplayState {
    bool hasTelemetry;
    bool doorOpen;
    float temperatureC;
    float inventoryWeightG;
    float estimatedPercent;
    int glucoseMgDl;
    int glucometerSequenceNumber;
    float doseUnits;
    int wifiRssiDbm;
    int bleRssiDbm;
    uint32_t freeHeapBytes;
    uint32_t lastUpdateMs;
    char timestamp[32];
    char injectedAt[32];
};

