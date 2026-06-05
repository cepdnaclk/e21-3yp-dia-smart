#pragma once

#include <Arduino.h>
#include "../config/event_types.h"

struct TelemetryEvent {
    // Root
    char         eventId[40];
    uint32_t     sequenceNumber;
    EventTrigger trigger;
    char         timestamp[32];
    bool         replayedEvent;

    // Storage
    float        temperatureC;
    bool         doorOpen;

    // Inventory
    float        inventoryWeightG;
    float        estimatedPercent;

    // Glucose
    int          glucoseMgDl;
    int          glucometerSequenceNumber;

    // Dose
    float        doseUnits;
    char         injectedAt[32];

    // Battery / System
    int          innerBatteryPercent;
    int          penBatteryPercent;
    int          outerBatteryPercent;
    int          wifiRssiDbm;
    int          bleRssiDbm;
    uint32_t     freeHeapBytes;
};
