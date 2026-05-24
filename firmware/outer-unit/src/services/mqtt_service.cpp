#include "mqtt_service.h"
#include <WiFiClientSecure.h>
#include <PubSubClient.h>
#include "config/app_config.h"
#include "config/aws_certs.h"

// Secure Wi-Fi client for TLS 1.2
WiFiClientSecure secureClient;

// MQTT Client instance
PubSubClient mqttClient(secureClient);

void setupMQTT()
{
    // Load the AWS certificates into the secure client
    secureClient.setCACert(AWS_CERT_CA);
    secureClient.setCertificate(AWS_CERT_CRT);
    secureClient.setPrivateKey(AWS_CERT_PRIVATE);

    // Configure the MQTT broker endpoint and port (8883 for MQTTS)
    mqttClient.setServer(AWS_IOT_ENDPOINT, AWS_IOT_PORT);
    
    // Ensure the buffer is large enough for our nested JSON payload
    mqttClient.setBufferSize(1024);
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

    // Publish the JSON payload to the defined topic
    if (mqttClient.publish(AWS_IOT_PUBLISH_TOPIC, payload.c_str()))
    {
        Serial.println("[MQTT] SUCCESS: Payload delivered to AWS.");
    }
    else
    {
        Serial.println("[MQTT] ERROR: Failed to deliver payload.");
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