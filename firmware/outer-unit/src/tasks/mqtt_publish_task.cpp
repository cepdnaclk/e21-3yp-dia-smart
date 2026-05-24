// #include <Arduino.h>
// #include "models/telemetry_event.h"

// extern QueueHandle_t telemetryQueue;

// void mqttPublishTask(void *parameter)
// {
//     TelemetryEvent receivedEvent;

//     while (true)
//     {
//         if (xQueueReceive(
//         telemetryQueue,
//         &receivedEvent,
//         portMAX_DELAY) == pdTRUE)
//     {
//     Serial.println();
//     Serial.println("=== EVENT RECEIVED ===");

//     Serial.print("Event ID: ");
//     Serial.println(receivedEvent.eventId);

//     Serial.print("Dose Units: ");
//     Serial.println(receivedEvent.insulinDoseUnits);

//     Serial.print("Glucose: ");
//     Serial.println(receivedEvent.glucoseMgDl);

//     Serial.print("Temperature: ");
//     Serial.println(receivedEvent.temperatureC, 2);

//     Serial.print("Inventory: ");
//     Serial.println(receivedEvent.inventoryWeightG, 2);

//     Serial.print("Inner Battery: ");
//     Serial.println(receivedEvent.innerBatteryPercent);

//     Serial.println("========================");
//     }
//     }
// }

#include <Arduino.h>
#include "models/telemetry_event.h"

extern QueueHandle_t telemetryQueue;

void mqttPublishTask(void *parameter)
{
    TelemetryEvent receivedEvent;
    
    // Allocate a buffer large enough to hold the entire text block
    char printBuffer[512]; 

    while (true)
    {
        // Block indefinitely until an event arrives
        if (xQueueReceive(telemetryQueue, &receivedEvent, portMAX_DELAY) == pdTRUE)
        {
            // Bundle the entire output into one single string
            snprintf(printBuffer, sizeof(printBuffer),
                     "\n=== EVENT RECEIVED ===\n"
                     "Event ID: %s\n"
                     "Dose Units: %d\n"
                     "Glucose: %d\n"
                     "Temperature: %.2f\n"
                     "Inventory: %.2f\n"
                     "Inner Battery: %d\n"
                     "========================\n",
                     receivedEvent.eventId,
                     receivedEvent.insulinDoseUnits,
                     receivedEvent.glucoseMgDl,
                     receivedEvent.temperatureC,
                     receivedEvent.inventoryWeightG,
                     receivedEvent.innerBatteryPercent);

            // Send to Native USB in one clean transaction
            Serial.print(printBuffer);
        }
    }
}