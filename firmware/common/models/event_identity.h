#pragma once

#include <stdint.h>

// Shared identity/timing metadata for internal firmware event tracking.
// This is introduced first on internal models only so later protocol work can
// adopt it safely without changing current live packet layouts immediately.

struct SourceIdentity {
    char     deviceUid[24];
    uint32_t bootSessionId;
};

struct SourceTiming {
    uint32_t sourceTimestampMs;
    uint32_t receivedAtMs;
};

struct EventIdentity {
    char     eventUid[40];
    uint32_t sourceSequence;
};
