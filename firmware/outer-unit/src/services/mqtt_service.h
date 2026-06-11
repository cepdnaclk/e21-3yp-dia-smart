#ifndef MQTT_SERVICE_H
#define MQTT_SERVICE_H

#include <Arduino.h>

void setupMQTT();
bool connectMQTT();
bool publishTelemetry(const String& payload);
void mqttLoop();
bool isMqttConnected();
int mqttState();

#endif
