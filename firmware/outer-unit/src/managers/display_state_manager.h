#pragma once

#include "models/display_state.h"
#include "models/telemetry_event.h"

void updateDisplayStateFromTelemetry(const TelemetryEvent& event);
DisplayState getDisplayStateSnapshot();

