#pragma once

#include <Arduino.h>

enum EventType {
    EVENT_DOSE,
    EVENT_GLUCOSE,
    EVENT_TEMPERATURE,
    EVENT_INVENTORY,
    EVENT_BATTERY
};

enum EventPriority {
    PRIORITY_HIGH,
    PRIORITY_MEDIUM,
    PRIORITY_LOW
};

struct TelemetryEvent {
    String eventId;

    EventType type;
    EventPriority priority;

    uint64_t timestamp;

    float temperatureC;

    int glucoseMgDl;

    int insulinUnits;

    float inventoryWeight;

    int batteryPercent;

    bool replayed;
};