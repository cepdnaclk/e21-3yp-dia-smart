#pragma once

#include <Arduino.h>
#include "../config/event_types.h"
#include "event_identity.h"

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
    bool         hasGlucose;
    int          glucoseMgDl;
    int          glucometerSequenceNumber;

    // Dose
    bool         hasDose;
    float        doseUnits;
    char         injectedAt[32];

    // Battery / System
    int          innerBatteryPercent;
    int          penBatteryPercent;
    int          outerBatteryPercent;
    int          wifiRssiDbm;
    int          bleRssiDbm;
    uint32_t     freeHeapBytes;

    // Internal metadata for future inter-device reliability work.
    EventIdentity identity;
    SourceIdentity source;
    SourceTiming timing;
};
