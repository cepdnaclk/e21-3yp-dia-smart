#include <Arduino.h>
#include "models/telemetry_event.h"
#include "include/system_queues.h"

void eventAggregatorTask(void *pvParameters) {

    int counter = 0;

    while (true) {

        TelemetryEvent event;

        event.eventId = "EVT-" + String(counter++);

        event.type = EVENT_DOSE;

        event.priority = PRIORITY_HIGH;

        event.timestamp = millis();

        event.insulinUnits = random(2, 15);

        event.temperatureC = 5.4;

        event.glucoseMgDl = 120;

        event.inventoryWeight = 40.5;

        event.batteryPercent = 87;

        event.replayed = false;

        Serial.println("[EVENT TASK] Creating mock event");

        xQueueSend(telemetryQueue, &event, portMAX_DELAY);

        vTaskDelay(pdMS_TO_TICKS(5000));
    }
}