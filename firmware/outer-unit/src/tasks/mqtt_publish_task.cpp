#include <Arduino.h>
#include "models/telemetry_event.h"
#include "services/json_serializer_service.h"
#include "services/mqtt_service.h"

extern QueueHandle_t telemetryQueue;

void mqttPublishTask(void *parameter)
{
    TelemetryEvent receivedEvent;
    uint32_t lastConnectAttemptMs = 0;

    // 1. Initialize TLS and connect to AWS IoT Core
    setupMQTT();
    connectMQTT();

    while (true)
    {
        // 2. Keep the MQTT connection alive
        mqttLoop();
        
        // Reconnect without blocking this task forever while offline.
        if (!isMqttConnected() && (millis() - lastConnectAttemptMs) >= 5000) {
            lastConnectAttemptMs = millis();
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
            
            // 4. Send to AWS. Offline queue handling is added in the next step.
            bool published = publishTelemetry(jsonPayload);
            if (!published) {
                Serial.println("[MQTTPublishTask] Publish failed; offline queue not enabled yet");
            }
        }
    }
}
