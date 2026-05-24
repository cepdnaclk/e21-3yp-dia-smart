#ifndef TELEMETRY_EVENT_H
#define TELEMETRY_EVENT_H

#include <Arduino.h>

enum EventTrigger
{
    DOSE_EVENT,
    GLUCOSE_EVENT,
    TEMPERATURE_ALERT,
    INVENTORY_LOW,
    BATTERY_LOW
};

struct TelemetryEvent
{
    char eventId[32];

    EventTrigger trigger;

    char timestamp[32];

    bool replayedEvent;

    // Storage
    float temperatureC;
    bool doorOpen;

    // Glucose
    int glucoseMgDl;

    // Dose
    int insulinDoseUnits;

    // Inventory
    float inventoryWeightG;

    // Battery
    int innerBatteryPercent;
    int penBatteryPercent;
    int outerBatteryPercent;
};

#endif