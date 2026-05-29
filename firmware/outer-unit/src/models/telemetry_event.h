#ifndef TELEMETRY_EVENT_H
#define TELEMETRY_EVENT_H

#include <Arduino.h>

enum EventTrigger {
    DOSE_EVENT,
    GLUCOSE_EVENT,
    TEMPERATURE_ALERT,
    INVENTORY_LOW,
    BATTERY_LOW
};

struct TelemetryEvent {
    // ---- Root ------------------------------------------------------------ //
    char         eventId[40];       // "EVT-DS-OUTER-0001-<seq>"
    uint32_t     sequenceNumber;    // root-level sequence counter
    EventTrigger trigger;
    char         timestamp[32];     // ISO-8601 e.g. "2026-05-29T10:30:00Z"
    bool         replayedEvent;

    // ---- Storage --------------------------------------------------------- //
    float        temperatureC;      // DS18B20 (NAN = sensor error)
    bool         doorOpen;          // true = door open

    // ---- Inventory ------------------------------------------------------- //
    float        inventoryWeightG;
    float        estimatedPercent;  // (weightG / FULL_BOTTLE_WEIGHT_G) * 100

    // ---- Glucose --------------------------------------------------------- //
    int          glucoseMgDl;
    int          glucometerSequenceNumber;

    // ---- Dose ------------------------------------------------------------ //
    float        doseUnits;         // Double in backend — float here is fine
    char         injectedAt[32];    // ISO-8601 timestamp of injection

    // ---- Battery / System ------------------------------------------------ //
    int          innerBatteryPercent;
    int          penBatteryPercent;
    int          outerBatteryPercent;
    int          wifiRssiDbm;
    int          bleRssiDbm;
    uint32_t     freeHeapBytes;
};

#endif