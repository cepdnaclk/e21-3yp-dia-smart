#pragma once

#include <stdint.h>

// One glucose measurement received from the BLE glucometer.
struct GlucoseReading {
    int      valueMgDl;
    int      sequenceNumber;
    uint32_t timestampMs;
};

// One dose event received from the pen unit over BLE.
struct DoseReading {
    float    doseUnits;
    float    angleDegrees;
    char     injectedAt[32];
    uint32_t timestampMs;
    uint8_t  penRecordSlot;
    uint32_t penTakenEpochSec;
    bool     hasPenTakenEpoch;
};
