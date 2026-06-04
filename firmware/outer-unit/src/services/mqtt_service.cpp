#include "mqtt_service.h"
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include "config/app_config.h"
#include "config/aws_certs.h"

// Secure Wi-Fi client for TLS 1.2
WiFiClientSecure secureClient;

// MQTT Client instance
PubSubClient mqttClient(secureClient);

static constexpr uint16_t MQTT_BUFFER_BYTES = 2048;

void setupMQTT()
{
    // Load the AWS certificates into the secure client
    secureClient.setCACert(AWS_CERT_CA);
    secureClient.setCertificate(AWS_CERT_CRT);
    secureClient.setPrivateKey(AWS_CERT_PRIVATE);

    // Configure the MQTT broker endpoint and port (8883 for MQTTS)
    mqttClient.setServer(AWS_IOT_ENDPOINT, AWS_IOT_PORT);

    // Increase limits for full COMBINED_TELEMETRY payloads over TLS.
    mqttClient.setBufferSize(MQTT_BUFFER_BYTES);
    mqttClient.setKeepAlive(30);
    mqttClient.setSocketTimeout(10);
}

void connectMQTT()
{
    // Loop until we are connected
    while (!mqttClient.connected())
    {
        Serial.print("Connecting to AWS IoT Core... ");
        
        // Connect using the Device UID as the unique Client ID
        if (mqttClient.connect(DEVICE_UID))
        {
            Serial.println("CONNECTED!");
        }
        else
        {
            Serial.print("FAILED, Return Code: ");
            Serial.print(mqttClient.state());
            Serial.println(" - Retrying in 3 seconds...");
            delay(3000);
        }
    }
}

void publishTelemetry(String payload)
{
    // Reconnect if the connection dropped
    if (!mqttClient.connected())
    {
        connectMQTT();
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
        return;
    }

    // Publish the JSON payload to the defined topic
    if (mqttClient.publish(AWS_IOT_PUBLISH_TOPIC, payload.c_str()))
    {
        Serial.println("[MQTT] SUCCESS: Payload delivered to AWS.");
    }
    else
    {
        Serial.print("[MQTT] ERROR: Failed to deliver payload. state=");
        Serial.print(mqttClient.state());
        Serial.print(", connected=");
        Serial.print(mqttClient.connected() ? "true" : "false");
        Serial.print(", topic=");
        Serial.print(AWS_IOT_PUBLISH_TOPIC);
        Serial.print(", len=");
        Serial.println(payloadLen);
    }
}

void mqttLoop()
{
    // This must be called frequently to maintain the MQTT keep-alive ping
    mqttClient.loop();
}

bool isMqttConnected()
{
    return mqttClient.connected();
}