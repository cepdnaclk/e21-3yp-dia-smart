#ifndef JSON_SERIALIZER_SERVICE_H
#define JSON_SERIALIZER_SERVICE_H

#include <Arduino.h>
#include "models/care_plan.h"
#include "models/telemetry_event.h"

// Takes a raw C++ struct and returns a formatted JSON string
String serializeTelemetryEvent(const TelemetryEvent& event);
String serializeCarePlanTelemetryEvent(const CarePlanTelemetryEvent& event);

#endif
