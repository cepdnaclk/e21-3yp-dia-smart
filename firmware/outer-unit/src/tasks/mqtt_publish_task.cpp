#include <Arduino.h>
#include <ArduinoJson.h>
#include <WiFi.h>
#include "config/app_config.h"
#include "managers/display_state_manager.h"
#include "services/care_plan_service.h"
#include "models/telemetry_event.h"
#include "services/json_serializer_service.h"
#include "services/mqtt_service.h"
#include "services/offline_json_queue_service.h"

extern QueueHandle_t telemetryQueue;

namespace {
constexpr uint8_t OFFLINE_RECORD_VERSION = 1;

void refreshDisplayMqttStatus(bool retrying, bool lastPublishOk) {
    updateDisplayConnectivity(WiFi.status() == WL_CONNECTED,
                              isMqttConnected(),
                              retrying,
                              mqttState(),
                              lastPublishOk);
    updateDisplayOfflineQueue(offlineJsonQueue.ready(), offlineJsonQueue.count());
}

bool makeOfflineRecord(const char* topic,
                       const String& payload,
                       String& record) {
    if (strcmp(topic, AWS_IOT_PUBLISH_TOPIC) == 0) {
        record = payload;
        return true;
    }

    JsonDocument payloadDocument;
    if (deserializeJson(payloadDocument, payload)) {
        return false;
    }
    JsonDocument recordDocument;
    recordDocument["_queueVersion"] = OFFLINE_RECORD_VERSION;
    recordDocument["_queueTopic"] = topic;
    recordDocument["payload"].set(payloadDocument.as<JsonVariantConst>());
    record = "";
    serializeJson(recordDocument, record);
    return record.length() < OFFLINE_JSON_MAX_BYTES;
}

bool unpackOfflineRecord(const String& record,
                         String& topic,
                         String& payload) {
    JsonDocument document;
    if (deserializeJson(document, record)) {
        return false;
    }

    if ((document["_queueVersion"] | 0) == OFFLINE_RECORD_VERSION &&
        document["_queueTopic"].is<const char*>() &&
        !document["payload"].isNull()) {
        topic = document["_queueTopic"].as<const char*>();
        payload = "";
        serializeJson(document["payload"], payload);
        return true;
    }

    topic = AWS_IOT_PUBLISH_TOPIC;
    payload = record;
    return true;
}

bool enqueueForTopic(const char* topic, const String& payload) {
    String record;
    if (!makeOfflineRecord(topic, payload, record)) {
        Serial.println("[MQTTPublishTask] Failed to encode offline MQTT record");
        return false;
    }
    return offlineJsonQueue.enqueue(record);
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
                String queuedTopic;
                String decodedPayload;
                bool decoded = unpackOfflineRecord(
                    queuedPayload, queuedTopic, decodedPayload);
                if (decoded &&
                    publishMqttMessage(queuedTopic.c_str(), decodedPayload)) {
                    offlineJsonQueue.pop();
                    lastPublishOk = true;
                    Serial.printf("[MQTTPublishTask] Queued JSON delivered. remaining=%u\n",
                                  offlineJsonQueue.count());
                    refreshDisplayMqttStatus(false, lastPublishOk);
                } else {
                    lastPublishOk = false;
                    Serial.println(decoded
                        ? "[MQTTPublishTask] Queued publish failed; retry later"
                        : "[MQTTPublishTask] Queued record is invalid; retry paused");
                    refreshDisplayMqttStatus(true, lastPublishOk);
                }
            }
        }

        CarePlanTelemetryEvent carePlanEvent = {};
        if (takePendingCarePlanTelemetry(carePlanEvent)) {
            String jsonPayload =
                serializeCarePlanTelemetryEvent(carePlanEvent);
            Serial.printf("[MQTTPublishTask] Publishing %s: %s\n",
                          carePlanEvent.eventType,
                          carePlanEvent.eventId);

            bool shouldQueue =
                offlineJsonQueue.ready() && offlineJsonQueue.count() > 0;
            bool published = false;
            if (!shouldQueue) {
                published = publishMqttMessage(
                    AWS_IOT_DEVICE_TELEMETRY_TOPIC, jsonPayload);
                lastPublishOk = published;
                refreshDisplayMqttStatus(
                    !published && !isMqttConnected(), lastPublishOk);
            }

            if (!published) {
                if (enqueueForTopic(
                        AWS_IOT_DEVICE_TELEMETRY_TOPIC, jsonPayload)) {
                    Serial.printf(
                        "[MQTTPublishTask] Care Plan event queued. queued=%u\n",
                        offlineJsonQueue.count());
                    refreshDisplayMqttStatus(
                        !isMqttConnected(), lastPublishOk);
                } else {
                    Serial.println(
                        "[MQTTPublishTask] ERROR: Care Plan event queue failed");
                    refreshDisplayMqttStatus(!isMqttConnected(), false);
                }
            } else {
                refreshDisplayMqttStatus(false, lastPublishOk);
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
                if (enqueueForTopic(AWS_IOT_PUBLISH_TOPIC, jsonPayload)) {
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
