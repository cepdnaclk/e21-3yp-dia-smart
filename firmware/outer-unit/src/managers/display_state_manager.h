#pragma once

#include "models/display_state.h"
#include "models/telemetry_event.h"

void updateDisplayStateFromTelemetry(const TelemetryEvent& event);
void updateDisplayDosePrompt(bool active,
                             bool editing,
                             float penDoseUnits,
                             int pendingDoseUnits,
                             int originalDoseUnits,
                             uint8_t remainingSec,
                             const char* editBuffer);
void updateDisplayConnectivity(bool wifiConnected,
                               bool mqttConnected,
                               bool mqttRetrying,
                               int mqttState,
                               bool lastPublishOk);
void updateDisplayOfflineQueue(bool ready, uint16_t count);
void updateDisplayPage(uint8_t page);
void updateDisplayActivity(bool innerPacket, bool glucose, bool dose);
DisplayState getDisplayStateSnapshot();

