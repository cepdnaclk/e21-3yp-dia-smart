#include <Arduino.h>
#include "models/telemetry_event.h"
#include "services/json_serializer_service.h"
#include "services/mqtt_service.h"

extern QueueHandle_t telemetryQueue;

void mqttPublishTask(void *parameter)
{
    TelemetryEvent receivedEvent;

    // 1. Initialize TLS and connect to AWS IoT Core
    setupMQTT();
    connectMQTT();

    while (true)
    {
        // 2. Keep the MQTT connection alive
        mqttLoop();
        
        // Reconnect if network drops (USING OUR NEW HELPER FUNCTION)
        if (!isMqttConnected()) {
            connectMQTT();
        }

        // 3. Check for new events (Wait max 100ms so the loop can repeat and keep MQTT alive)
        if (xQueueReceive(telemetryQueue, &receivedEvent, pdMS_TO_TICKS(100)) == pdTRUE)
        {
            // Serialize the C++ struct into JSON
            String jsonPayload = serializeTelemetryEvent(receivedEvent);

            Serial.print("\n[MQTTPublishTask] Publishing Event: ");
            Serial.println(receivedEvent.eventId);
            Serial.println(jsonPayload);
            
            // 4. Send to AWS!
            publishTelemetry(jsonPayload);
        }
    }
}