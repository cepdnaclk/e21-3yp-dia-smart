#pragma once

#include <stdint.h>
#include "../../../common/models/event_identity.h"

// Fixed-size flash/NVS record model for confirmed pen doses.
// Runtime persistence is added in a later step; this file defines the contract.

enum DoseRecordStatus : uint8_t {
    DOSE_RECORD_EMPTY = 0,
    DOSE_RECORD_PENDING = 1,
    DOSE_RECORD_SENT = 2,
    DOSE_RECORD_ACKED = 3,
    DOSE_RECORD_INVALID = 4
};

struct PersistentDoseRecord {
    EventIdentity identity;
    SourceTiming timing;
    float doseUnits;
    float angleDegrees;
    float confidencePercent;
    uint8_t status;       // DoseRecordStatus
    uint8_t retryCount;
    uint16_t reserved;
};

static const uint8_t PEN_DOSE_RECORD_CAPACITY = 16;
static const uint8_t PEN_DOSE_RECORD_FORMAT_VERSION = 1;
