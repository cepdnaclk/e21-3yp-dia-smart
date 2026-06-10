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
    next.dosePromptActive = latestState.dosePromptActive;
    next.dosePromptEditing = latestState.dosePromptEditing;
    next.pendingDoseUnits = latestState.pendingDoseUnits;
    next.originalDoseUnits = latestState.originalDoseUnits;
    next.dosePromptRemainingSec = latestState.dosePromptRemainingSec;
    strncpy(next.doseEditBuffer, latestState.doseEditBuffer, sizeof(next.doseEditBuffer) - 1);
    latestState = next;
    portEXIT_CRITICAL(&displayStateMux);
}

void updateDisplayDosePrompt(bool active,
                             bool editing,
                             int pendingDoseUnits,
                             int originalDoseUnits,
                             uint8_t remainingSec,
                             const char* editBuffer) {
    portENTER_CRITICAL(&displayStateMux);
    latestState.dosePromptActive = active;
    latestState.dosePromptEditing = editing;
    latestState.pendingDoseUnits = pendingDoseUnits;
    latestState.originalDoseUnits = originalDoseUnits;
    latestState.dosePromptRemainingSec = remainingSec;
    latestState.doseEditBuffer[0] = '\0';
    if (editBuffer != nullptr) {
        strncpy(latestState.doseEditBuffer, editBuffer, sizeof(latestState.doseEditBuffer) - 1);
    }
    portEXIT_CRITICAL(&displayStateMux);
}

DisplayState getDisplayStateSnapshot() {
    DisplayState snapshot = {};
    portENTER_CRITICAL(&displayStateMux);
    snapshot = latestState;
    portEXIT_CRITICAL(&displayStateMux);
    return snapshot;
}

