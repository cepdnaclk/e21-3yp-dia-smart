#include "json_serializer_service.h"
#include <ArduinoJson.h>
#include <esp_system.h>
#include "config/app_config.h"
#include "models/care_plan.h"

// ---- Helpers -------------------------------------------------------------- //

static const char* triggerToString(EventTrigger trigger) {
    switch (trigger) {
        case DOSE_EVENT:        return "DOSE_EVENT";
        case GLUCOSE_EVENT:     return "GLUCOSE_EVENT";
        case TEMPERATURE_ALERT: return "TEMPERATURE_ALERT";
        case INVENTORY_LOW:     return "INVENTORY_LOW";
        case BATTERY_LOW:       return "BATTERY_LOW";
        case DEVICE_HEALTH:     return "DEVICE_HEALTH";
        default:                return "DEVICE_EVENT";
    }
}

static const char* doorStatus(bool open) {
    return open ? "OPEN" : "CLOSED";
}

static const char* temperatureStatus(float tempC) {
    if (isnan(tempC))    return "UNKNOWN";
    if (tempC < TEMP_MIN_C) return "LOW";
    if (tempC > TEMP_MAX_C) return "HIGH";
    return "SAFE";
}

static const char* inventoryStatus(float pct) {
    if (pct <= 0.0f)  return "EMPTY";
    if (pct < 20.0f)  return "LOW";
    return "OK";
}

// ---- Main serialiser ------------------------------------------------------ //

String serializeTelemetryEvent(const TelemetryEvent& event) {
    JsonDocument doc;

    // ---- Root ------------------------------------------------------------ //
    doc["eventId"]        = event.eventId;
    doc["eventType"]      = "COMBINED_TELEMETRY";
    doc["trigger"]        = triggerToString(event.trigger);
    doc["timestamp"]      = event.timestamp;
    doc["schemaVersion"]  = 1;
    doc["sequenceNumber"] = event.sequenceNumber;
    doc["replayedEvent"]  = event.replayedEvent;

    // ---- patient --------------------------------------------------------- //
    // PATIENT_ID is a #define int — backend DTO expects Long (numeric), NOT String
    doc["patient"]["patientId"] = PATIENT_ID;

    // ---- gateway --------------------------------------------------------- //
    doc["gateway"]["deviceUid"]       = DEVICE_UID_OUTER;
    doc["gateway"]["firmwareVersion"] = FIRMWARE_VERSION;

    // ---- storage --------------------------------------------------------- //
    doc["storage"]["deviceUid"]         = DEVICE_UID_INNER;
    // NaN is not valid JSON — omit the field if DS18B20 errored
    if (!isnan(event.temperatureC)) {
        doc["storage"]["temperatureC"]  = event.temperatureC;
    }
    doc["storage"]["doorStatus"]        = doorStatus(event.doorOpen);
    doc["storage"]["temperatureStatus"] = temperatureStatus(event.temperatureC);

    // ---- inventory ------------------------------------------------------- //
    doc["inventory"]["deviceUid"]                  = DEVICE_UID_INNER;
    doc["inventory"]["weightG"]                    = event.inventoryWeightG;
    doc["inventory"]["estimatedRemainingPercent"]  = event.estimatedPercent;
    doc["inventory"]["inventoryStatus"]            = inventoryStatus(event.estimatedPercent);

    // ---- glucose --------------------------------------------------------- //
    if (event.hasGlucose) {
        doc["glucose"]["deviceUid"]      = DEVICE_UID_GLUCOMETER;
        doc["glucose"]["valueMgDl"]      = event.glucoseMgDl;
        doc["glucose"]["source"]         = "BLE_GLUCOMETER";
        doc["glucose"]["sequenceNumber"] = event.glucometerSequenceNumber;
        if (event.hasGlucoseMeasuredAt) {
            doc["glucose"]["measuredAt"] = event.glucoseMeasuredAt;
        }
    }

    // ---- dose ------------------------------------------------------------ //
    if (event.hasDose) {
        doc["dose"]["deviceUid"]       = DEVICE_UID_PEN;
        doc["dose"]["doseUnits"]       = event.doseUnits;
        doc["dose"]["detectionMethod"] = "AS5600";
        doc["dose"]["injectedAt"]      = event.injectedAt;
        doc["dose"]["eventStatus"]     = "CONFIRMED";
    }

    // ---- battery --------------------------------------------------------- //
    doc["battery"]["innerUnitDeviceUid"]  = DEVICE_UID_INNER;
    doc["battery"]["penUnitDeviceUid"]    = DEVICE_UID_PEN;
    doc["battery"]["outerUnitDeviceUid"]  = DEVICE_UID_OUTER;
    doc["battery"]["innerUnitPercent"]    = event.innerBatteryPercent;
    doc["battery"]["penUnitPercent"]      = event.penBatteryPercent;
    doc["battery"]["outerUnitPercent"]    = event.outerBatteryPercent;
    doc["battery"]["wifiRssiDbm"]         = event.wifiRssiDbm;
    doc["battery"]["bleRssiDbm"]          = event.bleRssiDbm;
    doc["battery"]["freeHeapBytes"]       = (uint32_t)event.freeHeapBytes;
    doc["battery"]["powerSource"]         = "BATTERY";
    doc["battery"]["status"]              = "ONLINE";

    // ---- Serialise ------------------------------------------------------- //
    // Use serializeJson (compact) — NOT serializeJsonPretty.
    // serializeJsonPretty is ~3x larger and will overflow the 1024-byte MQTT buffer.
    String output;
    serializeJson(doc, output);
    return output;
}

String serializeCarePlanTelemetryEvent(const CarePlanTelemetryEvent& event) {
    JsonDocument doc;
    doc["eventId"] = event.eventId;
    doc["eventType"] = event.eventType;
    doc["outerDeviceId"] = DEVICE_UID_OUTER;
    doc["scheduleId"] = event.scheduleId;
    doc["carePlanVersion"] = event.carePlanVersion;
    if (event.repeatNumber > 0) {
        doc["repeatNumber"] = event.repeatNumber;
    }
    doc["windowStart"] = event.windowStart;
    doc["targetTime"] = event.targetTime;
    doc["windowEnd"] = event.windowEnd;
    doc["timestamp"] = event.timestamp;

    String output;
    serializeJson(doc, output);
    return output;
}
