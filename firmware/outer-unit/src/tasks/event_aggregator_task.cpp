#include <Arduino.h>
#include "models/telemetry_event.h"
#include "services/event_builder_service.h"

extern QueueHandle_t telemetryQueue;

void eventAggregatorTask(void *parameter)
{
    while (true)
    {
        TelemetryEvent event = buildMockDoseEvent();

        xQueueSend(telemetryQueue, &event, portMAX_DELAY);

        vTaskDelay(pdMS_TO_TICKS(5000));
    }
}