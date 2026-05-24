#include "json_serializer_service.h"
#include <ArduinoJson.h>

// Helper to map the C++ Enum to the String required by the MQTT Contract
const char* triggerToString(EventTrigger trigger) {
    switch (trigger) {
        case DOSE_EVENT: return "DOSE_EVENT";
        case GLUCOSE_EVENT: return "GLUCOSE_EVENT";
        case TEMPERATURE_ALERT: return "TEMP_EVENT";
        case INVENTORY_LOW: return "INVENTORY_EVENT";
        case BATTERY_LOW: return "BATTERY_EVENT";
        default: return "DEVICE_EVENT";
    }
}

String serializeTelemetryEvent(const TelemetryEvent& event) {
    // ArduinoJson v7 automatically sizes the document memory
    JsonDocument doc;

    // Root elements matching the API contract
    doc["eventId"] = event.eventId;
    doc["eventType"] = "COMBINED_TELEMETRY";
    doc["trigger"] = triggerToString(event.trigger);
    doc["timestamp"] = event.timestamp;
    doc["replayedEvent"] = event.replayedEvent;

    // Static identifiers (To be moved to a config file later)
    doc["patient"]["patientId"] = "P001";
    doc["gateway"]["deviceUid"] = "DS-OUTER-0001";
    doc["gateway"]["firmwareVersion"] = "v1.0.0";

    // Storage nesting
    doc["storage"]["temperatureC"] = event.temperatureC;
    doc["storage"]["doorStatus"] = event.doorOpen ? "OPEN" : "CLOSED";

    // Glucose nesting
    doc["glucose"]["valueMgDl"] = event.glucoseMgDl;
    doc["glucose"]["source"] = "BLE_GLUCOMETER";

    // Dose nesting
    doc["dose"]["insulinDoseUnits"] = event.insulinDoseUnits;
    doc["dose"]["detectionMethod"] = "AS5600";

    // Inventory nesting
    doc["inventory"]["weightG"] = event.inventoryWeightG;

    // Battery nesting
    doc["battery"]["innerUnitPercent"] = event.innerBatteryPercent;
    doc["battery"]["penUnitPercent"] = event.penBatteryPercent;
    doc["battery"]["outerUnitPercent"] = event.outerBatteryPercent;

    String output;
    // We use serializeJsonPretty for debugging. 
    // Later, we will change this to serializeJson() to save bandwidth over MQTT.
    serializeJsonPretty(doc, output);
    
    return output;
}