#include <Arduino.h>
#include "models/telemetry_event.h"
#include "include/system_queues.h"

void mqttPublishTask(void *pvParameters) {

    TelemetryEvent receivedEvent;

    while (true) {

        if (xQueueReceive(
                telemetryQueue,
                &receivedEvent,
                portMAX_DELAY
            ) == pdTRUE) {

            Serial.println("\n[MQTT TASK] Event received");

            Serial.println("Event ID: " + receivedEvent.eventId);

            Serial.println("Dose Units: " +
                           String(receivedEvent.insulinUnits));

            Serial.println("Temperature: " +
                           String(receivedEvent.temperatureC));

            Serial.println("Battery: " +
                           String(receivedEvent.batteryPercent));
        }
    }
}