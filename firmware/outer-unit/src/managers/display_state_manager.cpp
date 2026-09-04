#include "display_state_manager.h"

#include <Arduino.h>
#include <string.h>

namespace {
DisplayState latestState = {};
portMUX_TYPE displayStateMux = portMUX_INITIALIZER_UNLOCKED;
}

void updateDisplayStateFromTelemetry(const TelemetryEvent& event) {
    DisplayState next = {};
    next.hasTelemetry = true;
    next.doorOpen = event.doorOpen;
    next.temperatureC = event.temperatureC;
    next.inventoryWeightG = event.inventoryWeightG;
    next.estimatedPercent = event.estimatedPercent;
    next.glucoseMgDl = event.glucoseMgDl;
    next.glucometerSequenceNumber = event.glucometerSequenceNumber;
    next.doseUnits = event.doseUnits;
    next.innerBatteryPercent = event.innerBatteryPercent;
    next.wifiRssiDbm = event.wifiRssiDbm;
    next.bleRssiDbm = event.bleRssiDbm;
    next.freeHeapBytes = event.freeHeapBytes;
    next.lastUpdateMs = millis();
    strncpy(next.timestamp, event.timestamp, sizeof(next.timestamp) - 1);
    strncpy(next.injectedAt, event.injectedAt, sizeof(next.injectedAt) - 1);

    portENTER_CRITICAL(&displayStateMux);
    next.activePage = latestState.activePage;
    next.wifiConnected = latestState.wifiConnected;
    next.mqttConnected = latestState.mqttConnected;
    next.mqttRetrying = latestState.mqttRetrying;
    next.offlineQueueReady = latestState.offlineQueueReady;
    next.offlineQueueCount = latestState.offlineQueueCount;
    next.offlineQueueOldestMs = latestState.offlineQueueOldestMs;
    next.lastPublishOk = latestState.lastPublishOk;
    next.mqttState = latestState.mqttState;
    next.lastPublishMs = latestState.lastPublishMs;
    next.lastInnerPacketMs = latestState.lastInnerPacketMs;
    next.lastGlucoseMs = latestState.lastGlucoseMs;
    next.lastDoseMs = latestState.lastDoseMs;
    next.dosePromptActive = latestState.dosePromptActive;
    next.dosePromptEditing = latestState.dosePromptEditing;
    next.promptPenDoseUnits = latestState.promptPenDoseUnits;
    next.pendingDoseUnits = latestState.pendingDoseUnits;
    next.originalDoseUnits = latestState.originalDoseUnits;
    next.dosePromptRemainingSec = latestState.dosePromptRemainingSec;
    strncpy(next.doseEditBuffer, latestState.doseEditBuffer, sizeof(next.doseEditBuffer) - 1);
    next.noticeType = latestState.noticeType;
    next.noticeDoseUnits = latestState.noticeDoseUnits;
    next.noticeStartedMs = latestState.noticeStartedMs;
    latestState = next;
    portEXIT_CRITICAL(&displayStateMux);
}

void updateDisplayConnectivity(bool wifiConnected,
                               bool mqttConnected,
                               bool mqttRetrying,
                               int mqttState,
                               bool lastPublishOk) {
    portENTER_CRITICAL(&displayStateMux);
    latestState.wifiConnected = wifiConnected;
    latestState.mqttConnected = mqttConnected;
    latestState.mqttRetrying = mqttRetrying;
    latestState.mqttState = mqttState;
    latestState.lastPublishOk = lastPublishOk;
    latestState.lastPublishMs = millis();
    portEXIT_CRITICAL(&displayStateMux);
}

void updateDisplayOfflineQueue(bool ready, uint16_t count) {
    uint32_t now = millis();
    portENTER_CRITICAL(&displayStateMux);
    latestState.offlineQueueReady = ready;
    if (count == 0) {
        latestState.offlineQueueOldestMs = 0;
    } else if (latestState.offlineQueueCount == 0 || latestState.offlineQueueOldestMs == 0) {
        latestState.offlineQueueOldestMs = now;
    }
    latestState.offlineQueueCount = count;
    portEXIT_CRITICAL(&displayStateMux);
}

void updateDisplayPage(uint8_t page) {
    portENTER_CRITICAL(&displayStateMux);
    latestState.activePage = page;
    portEXIT_CRITICAL(&displayStateMux);
}

void updateDisplayActivity(bool innerPacket, bool glucose, bool dose) {
    uint32_t now = millis();
    portENTER_CRITICAL(&displayStateMux);
    if (innerPacket) latestState.lastInnerPacketMs = now;
    if (glucose) latestState.lastGlucoseMs = now;
    if (dose) latestState.lastDoseMs = now;
    portEXIT_CRITICAL(&displayStateMux);
}

void updateDisplayDosePrompt(bool active,
                             bool editing,
                             float penDoseUnits,
                             int pendingDoseUnits,
                             int originalDoseUnits,
                             uint8_t remainingSec,
                             const char* editBuffer) {
    portENTER_CRITICAL(&displayStateMux);
    latestState.dosePromptActive = active;
    latestState.dosePromptEditing = editing;
    latestState.promptPenDoseUnits = penDoseUnits;
    latestState.pendingDoseUnits = pendingDoseUnits;
    latestState.originalDoseUnits = originalDoseUnits;
    latestState.dosePromptRemainingSec = remainingSec;
    latestState.doseEditBuffer[0] = '\0';
    if (editBuffer != nullptr) {
        strncpy(latestState.doseEditBuffer, editBuffer, sizeof(latestState.doseEditBuffer) - 1);
    }
    portEXIT_CRITICAL(&displayStateMux);
}

void showDisplayNotice(uint8_t noticeType, float doseUnits) {
    portENTER_CRITICAL(&displayStateMux);
    latestState.noticeType = noticeType;
    latestState.noticeDoseUnits = doseUnits;
    latestState.noticeStartedMs = millis();
    portEXIT_CRITICAL(&displayStateMux);
}

DisplayState getDisplayStateSnapshot() {
    DisplayState snapshot = {};
    portENTER_CRITICAL(&displayStateMux);
    snapshot = latestState;
    portEXIT_CRITICAL(&displayStateMux);
    return snapshot;
}

