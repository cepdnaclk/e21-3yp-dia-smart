#ifndef MQTT_SERVICE_H
#define MQTT_SERVICE_H

#include <Arduino.h>

void setupMQTT();
bool connectMQTT();
bool publishTelemetry(const String& payload);
bool publishMqttMessage(const char* topic, const String& payload, bool retained = false);
void mqttLoop();
bool isMqttConnected();
int mqttState();

#endif
