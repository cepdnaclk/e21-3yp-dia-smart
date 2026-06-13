#include <Arduino.h>
#include <WiFi.h>
#include "config/app_config.h"
#include "managers/display_state_manager.h"
#include "models/telemetry_event.h"
#include "services/json_serializer_service.h"
#include "services/mqtt_service.h"
#include "services/offline_json_queue_service.h"

extern QueueHandle_t telemetryQueue;

namespace {
void refreshDisplayMqttStatus(bool retrying, bool lastPublishOk) {
    updateDisplayConnectivity(WiFi.status() == WL_CONNECTED,
                              isMqttConnected(),
                              retrying,
                              mqttState(),
                              lastPublishOk);
    updateDisplayOfflineQueue(offlineJsonQueue.ready(), offlineJsonQueue.count());
}
}

void mqttPublishTask(void *parameter)
{
    TelemetryEvent receivedEvent;
    uint32_t lastConnectAttemptMs = 0;
    bool lastPublishOk = false;

    // 1. Initialize TLS and connect to AWS IoT Core
    offlineJsonQueue.begin();
    refreshDisplayMqttStatus(false, lastPublishOk);
    setupMQTT();
    bool connected = connectMQTT();
    refreshDisplayMqttStatus(!connected, lastPublishOk);

    while (true)
    {
        // 2. Keep the MQTT connection alive
        mqttLoop();
        
        // Reconnect without blocking this task forever while offline.
        if (!isMqttConnected() && (millis() - lastConnectAttemptMs) >= 5000) {
            lastConnectAttemptMs = millis();
            refreshDisplayMqttStatus(true, lastPublishOk);
            connected = connectMQTT();
            refreshDisplayMqttStatus(!connected, lastPublishOk);
        }

        // Queued payloads are older than live telemetry, so retry them first.
        if (isMqttConnected() &&
            offlineJsonQueue.ready() &&
            offlineJsonQueue.count() > 0 &&
            (millis() - lastConnectAttemptMs) >= OFFLINE_QUEUE_RETRY_INTERVAL_MS) {
            lastConnectAttemptMs = millis();
            String queuedPayload;
            if (offlineJsonQueue.peek(queuedPayload)) {
                Serial.printf("[MQTTPublishTask] Retrying queued JSON. queued=%u\n",
                              offlineJsonQueue.count());
                if (publishTelemetry(queuedPayload)) {
                    offlineJsonQueue.pop();
                    lastPublishOk = true;
                    Serial.printf("[MQTTPublishTask] Queued JSON delivered. remaining=%u\n",
                                  offlineJsonQueue.count());
                    refreshDisplayMqttStatus(false, lastPublishOk);
                } else {
                    lastPublishOk = false;
                    Serial.println("[MQTTPublishTask] Queued publish failed; retry later");
                    refreshDisplayMqttStatus(true, lastPublishOk);
                }
            }
        }

        // 3. Check for new events (Wait max 100ms so the loop can repeat and keep MQTT alive)
        if (xQueueReceive(telemetryQueue, &receivedEvent, pdMS_TO_TICKS(100)) == pdTRUE)
        {
            // Serialize the C++ struct into JSON
            String jsonPayload = serializeTelemetryEvent(receivedEvent);

            Serial.print("\n[MQTTPublishTask] Publishing Event: ");
            Serial.println(receivedEvent.eventId);
            Serial.println(jsonPayload);
            
            // Preserve ordering: if anything is queued, store this new payload too.
            bool shouldQueue = offlineJsonQueue.ready() && offlineJsonQueue.count() > 0;
            bool published = false;
            if (!shouldQueue) {
                published = publishTelemetry(jsonPayload);
                lastPublishOk = published;
                refreshDisplayMqttStatus(!published && !isMqttConnected(), lastPublishOk);
            }

            if (!published) {
                if (offlineJsonQueue.enqueue(jsonPayload)) {
                    Serial.printf("[MQTTPublishTask] Payload queued for retry. queued=%u\n",
                                  offlineJsonQueue.count());
                    refreshDisplayMqttStatus(!isMqttConnected(), lastPublishOk);
                } else {
                    Serial.println("[MQTTPublishTask] ERROR: publish failed and offline queue save failed");
                    refreshDisplayMqttStatus(!isMqttConnected(), false);
                }
            } else {
                refreshDisplayMqttStatus(false, lastPublishOk);
            }
        }
    }
}
