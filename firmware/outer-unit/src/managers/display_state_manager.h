#pragma once

#include "models/display_state.h"
#include "models/telemetry_event.h"

void updateDisplayStateFromTelemetry(const TelemetryEvent& event);
void updateDisplayDosePrompt(bool active,
                             bool editing,
                             int pendingDoseUnits,
                             int originalDoseUnits,
                             uint8_t remainingSec,
                             const char* editBuffer);
DisplayState getDisplayStateSnapshot();

