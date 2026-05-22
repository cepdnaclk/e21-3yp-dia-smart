#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

#include "include/system_queues.h"
#include "models/telemetry_event.h"

QueueHandle_t telemetryQueue;

void eventAggregatorTask(void *pvParameters);
void mqttPublishTask(void *pvParameters);

void setup() {

    Serial.begin(115200);

    delay(2000);

    Serial.println("\n=== Dia-Smart Outer Unit Starting ===");

    telemetryQueue = xQueueCreate(
        10,
        sizeof(TelemetryEvent)
    );

    if (telemetryQueue == NULL) {

        Serial.println("Failed to create telemetry queue");

        while (true);
    }

    xTaskCreatePinnedToCore(
        eventAggregatorTask,
        "EventAggregatorTask",
        4096,
        NULL,
        1,
        NULL,
        1
    );

    xTaskCreatePinnedToCore(
        mqttPublishTask,
        "MQTTPublishTask",
        4096,
        NULL,
        1,
        NULL,
        1
    );

    Serial.println("Tasks started successfully");
}

void loop() {

    vTaskDelay(portMAX_DELAY);
}