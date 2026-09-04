#include "mqtt_service.h"
#include <WiFi.h>
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include <ArduinoJson.h>
#include <esp_system.h>
#include <time.h>
#include "config/app_config.h"
#include "config/aws_certs.h"
#include "services/care_plan_service.h"
#include "services/wifi_command_service.h"

// Secure Wi-Fi client for TLS 1.2
WiFiClientSecure secureClient;

// MQTT Client instance
PubSubClient mqttClient(secureClient);

namespace {
String pendingCarePlanAck;
bool carePlanAckPending = false;
bool carePlanSubscribed = false;
bool commandSubscribed = false;
String pendingDeviceSync;
bool deviceSyncPending = false;

void currentTimestamp(char* output, size_t outputLength)
{
    struct tm timeInfo;
    if (getLocalTime(&timeInfo)) {
        strftime(output, outputLength, "%Y-%m-%dT%H:%M:%SZ", &timeInfo);
    } else {
        strncpy(output, "1970-01-01T00:00:00Z", outputLength - 1);
        output[outputLength - 1] = '\0';
    }
}

void queueCarePlanAck(const CarePlanApplyResult& result)
{
    JsonDocument document;
    document["carePlanId"] = result.carePlanId;
    document["version"] = result.version;
    document["status"] = result.accepted ? "APPLIED" : "REJECTED";
    document["outerDeviceId"] = DEVICE_UID_OUTER;
    document["message"] = result.message;

    char timestamp[32];
    currentTimestamp(timestamp, sizeof(timestamp));
    document["timestamp"] = timestamp;

    String payload;
    serializeJson(document, payload);
    pendingCarePlanAck = payload;
    carePlanAckPending = true;
}

void queueDeviceSyncRequest()
{
    JsonDocument document;
    char timestamp[32];
    currentTimestamp(timestamp, sizeof(timestamp));

    char eventId[48];
    snprintf(eventId,
             sizeof(eventId),
             "SYNC-%lu-%08lX",
             (unsigned long)time(nullptr),
             (unsigned long)esp_random());

    document["eventId"] = eventId;
    document["eventType"] = "DEVICE_SYNC_REQUEST";
    document["outerDeviceId"] = DEVICE_UID_OUTER;
    document["timestamp"] = timestamp;

    pendingDeviceSync = "";
    serializeJson(document, pendingDeviceSync);
    deviceSyncPending = true;
}

void mqttMessageCallback(char* topic, byte* payload, unsigned int length)
{
    if (strcmp(topic, AWS_IOT_COMMAND_TOPIC) == 0) {
        if (!enqueueWifiCommandPayload(payload, length)) {
            Serial.printf(
                "[MQTT] Wi-Fi command dropped. bytes=%u\n",
                length);
        }
        return;
    }

    if (strcmp(topic, AWS_IOT_CARE_PLAN_TOPIC) == 0) {
        Serial.printf("[MQTT] Care Plan received. bytes=%u\n", length);
        CarePlanApplyResult result = applyCarePlanPayload(payload, length);
        Serial.printf("[MQTT] Care Plan %s: %s\n",
                      result.accepted ? "accepted" : "rejected",
                      result.message);
        queueCarePlanAck(result);
    }
}

bool subscribeDeviceTopics()
{
    carePlanSubscribed = mqttClient.subscribe(AWS_IOT_CARE_PLAN_TOPIC, 1);
    commandSubscribed = mqttClient.subscribe(AWS_IOT_COMMAND_TOPIC, 1);
    Serial.printf("[MQTT] Care Plan subscription %s: %s\n",
                  carePlanSubscribed ? "ready" : "failed",
                  AWS_IOT_CARE_PLAN_TOPIC);
    Serial.printf("[MQTT] Command subscription %s: %s\n",
                  commandSubscribed ? "ready" : "failed",
                  AWS_IOT_COMMAND_TOPIC);
    if (carePlanSubscribed && commandSubscribed) {
        queueDeviceSyncRequest();
    }
    return carePlanSubscribed && commandSubscribed;
}
}

void setupMQTT()
{
    // Load the AWS certificates into the secure client
    secureClient.setCACert(AWS_CERT_CA);
    secureClient.setCertificate(AWS_CERT_CRT);
    secureClient.setPrivateKey(AWS_CERT_PRIVATE);

    // Configure the MQTT broker endpoint and port (8883 for MQTTS)
    mqttClient.setServer(AWS_IOT_ENDPOINT, AWS_IOT_PORT);
    mqttClient.setCallback(mqttMessageCallback);

    // Increase limits for full COMBINED_TELEMETRY payloads over TLS.
    mqttClient.setBufferSize(MQTT_BUFFER_BYTES);
    mqttClient.setKeepAlive(30);
    mqttClient.setSocketTimeout(10);

    if (!setupWifiCommandService()) {
        Serial.println("[MQTT] Wi-Fi command service unavailable");
    }
}

bool connectMQTT()
{
    if (mqttClient.connected()) {
        return (carePlanSubscribed && commandSubscribed) ||
               subscribeDeviceTopics();
    }

    if (WiFi.status() != WL_CONNECTED) {
        Serial.println("[MQTT] WiFi offline; MQTT connect skipped");
        return false;
    }

    Serial.print("Connecting to AWS IoT Core... ");
    if (mqttClient.connect(DEVICE_UID)) {
        Serial.println("CONNECTED!");
        return subscribeDeviceTopics();
    }

    Serial.print("FAILED, Return Code: ");
    Serial.println(mqttClient.state());
    carePlanSubscribed = false;
    commandSubscribed = false;
    return false;
}

bool publishTelemetry(const String& payload)
{
    return publishMqttMessage(AWS_IOT_PUBLISH_TOPIC, payload);
}

bool publishMqttMessage(const char* topic, const String& payload, bool retained)
{
    if (!mqttClient.connected() && !connectMQTT()) {
        return false;
    }

    // Pump client once before publish to reduce stale-socket failures.
    mqttClient.loop();

    size_t payloadLen = payload.length();
    if (payloadLen >= MQTT_BUFFER_BYTES)
    {
        Serial.print("[MQTT] ERROR: Payload too large for buffer. len=");
        Serial.print(payloadLen);
        Serial.print(", buffer=");
        Serial.println(MQTT_BUFFER_BYTES);
        return false;
    }

    // Publish the JSON payload to the defined topic
    if (mqttClient.publish(topic, payload.c_str(), retained))
    {
        Serial.println("[MQTT] SUCCESS: Payload delivered to AWS.");
        return true;
    }

    Serial.print("[MQTT] ERROR: Failed to deliver payload. state=");
    Serial.print(mqttClient.state());
    Serial.print(", connected=");
    Serial.print(mqttClient.connected() ? "true" : "false");
    Serial.print(", topic=");
    Serial.print(topic);
    Serial.print(", len=");
    Serial.println(payloadLen);
    return false;
}

void mqttLoop()
{
    // This must be called frequently to maintain the MQTT keep-alive ping
    mqttClient.loop();
    processPendingWifiCommand();

    // Publish outside the inbound callback to avoid re-entering PubSubClient.
    if (carePlanAckPending && mqttClient.connected()) {
        if (mqttClient.publish(AWS_IOT_COMMAND_ACK_TOPIC,
                               pendingCarePlanAck.c_str(),
                               false)) {
            carePlanAckPending = false;
            pendingCarePlanAck = "";
            Serial.println("[MQTT] Care Plan ACK delivered");
        }
    }

    if (deviceSyncPending && mqttClient.connected()) {
        if (mqttClient.publish(AWS_IOT_DEVICE_TELEMETRY_TOPIC,
                               pendingDeviceSync.c_str(),
                               false)) {
            deviceSyncPending = false;
            pendingDeviceSync = "";
            Serial.println("[MQTT] Device sync request delivered");
        }
    }

    if (!mqttClient.connected()) {
        carePlanSubscribed = false;
        commandSubscribed = false;
    }
}

bool isMqttConnected()
{
    return mqttClient.connected();
}

int mqttState()
{
    return mqttClient.state();
}
