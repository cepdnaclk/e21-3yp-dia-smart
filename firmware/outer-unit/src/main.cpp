#include <Arduino.h>
#include "models/telemetry_event.h"

QueueHandle_t telemetryQueue;

void eventAggregatorTask(void *parameter);
void mqttPublishTask(void *parameter);

void setup()
{
    Serial.begin(115200);

    delay(1000);

    Serial.println("\n=== Dia-Smart Outer Unit Starting ===");

    telemetryQueue = xQueueCreate(
        10,
        sizeof(TelemetryEvent));

    if (telemetryQueue == NULL)
    {
        Serial.println("Queue creation failed!");

        while (true);
    }

    xTaskCreatePinnedToCore(
        eventAggregatorTask,
        "EventAggregatorTask",
        8192,
        NULL,
        2,
        NULL,
        1);

    xTaskCreatePinnedToCore(
        mqttPublishTask,
        "MQTTPublishTask",
        8192,
        NULL,
        1,
        NULL,
        0);

    Serial.println("Tasks started successfully");

    vTaskDelay(pdMS_TO_TICKS(1000));
}

void loop()
{
    vTaskDelay(portMAX_DELAY);
}

// void setup()
// {
//     Serial.begin(115200);

//     delay(3000);

//     Serial.println("BOOT SUCCESS");
// }

// void loop()
// {
//     Serial.println("RUNNING...");
//     delay(2000);
// }