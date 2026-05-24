#ifndef MQTT_SERVICE_H
#define MQTT_SERVICE_H

#include <Arduino.h>

void setupMQTT();
void connectMQTT();
void publishTelemetry(String payload);
void mqttLoop();
bool isMqttConnected();

#endif