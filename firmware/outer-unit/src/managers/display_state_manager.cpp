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
    latestState = next;
    portEXIT_CRITICAL(&displayStateMux);
}

DisplayState getDisplayStateSnapshot() {
    DisplayState snapshot = {};
    portENTER_CRITICAL(&displayStateMux);
    snapshot = latestState;
    portEXIT_CRITICAL(&displayStateMux);
    return snapshot;
}

