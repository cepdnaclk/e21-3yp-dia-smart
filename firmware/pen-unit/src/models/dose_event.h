#pragma once

#include <stdint.h>

// =============================================================================
// DoseEvent — internal queue message passed from doseDetectionTask to
// bleTransferTask. Represents one confirmed injection detected by the pen unit.
// =============================================================================

struct DoseEvent {
    // Calculated insulin units: abs(angleDelta) / DEGREES_PER_UNIT
    float    doseUnits;

    // Raw AS5600 angle delta (degrees) that produced this dose reading
    float    angleDegrees;

    // Confidence level (0–100 %). Based on whether angle crossed the
    // threshold cleanly and button was explicitly pressed.
    float    confidencePercent;

    // millis() timestamp at the moment the button was pressed (detection time)
    uint32_t timestampMs;
};
