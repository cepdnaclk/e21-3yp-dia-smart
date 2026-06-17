#include "event_builder_service.h"

TelemetryEvent buildMockDoseEvent()
{
    static int counter = 0;

    TelemetryEvent event = {};

    snprintf(
    event.eventId,
    sizeof(event.eventId),
    "EVT-%d",
    counter++);

    event.trigger = DOSE_EVENT;

    strncpy(
    event.timestamp,
    "2026-05-25T10:00:00Z",
    sizeof(event.timestamp));
    event.replayedEvent = false;

    event.temperatureC = 5.4;
    event.doorOpen = false;

    event.hasGlucose = true;
    event.glucoseMgDl = 118;

    event.hasDose = true;
    event.doseUnits = (float)random(4, 15);

    event.inventoryWeightG = 41.8;

    event.innerBatteryPercent = 87;
    event.penBatteryPercent = 76;
    event.outerBatteryPercent = 94;

    return event;
}
