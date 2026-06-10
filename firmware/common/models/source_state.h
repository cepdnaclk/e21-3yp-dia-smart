#pragma once

#include <stdint.h>

// Shared source-state model for future outer-unit cache/freshness decisions.
// This header does not change current packet formats or JSON serialization.

enum SourceAvailability : uint8_t {
    SOURCE_AVAILABILITY_UNKNOWN = 0,
    SOURCE_AVAILABILITY_AVAILABLE = 1,
    SOURCE_AVAILABILITY_STALE = 2,
    SOURCE_AVAILABILITY_UNAVAILABLE = 3
};

enum SourceDataQuality : uint8_t {
    SOURCE_DATA_UNKNOWN = 0,
    SOURCE_DATA_VALID = 1,
    SOURCE_DATA_PARTIAL = 2,
    SOURCE_DATA_INVALID = 3
};

enum SourceKind : uint8_t {
    SOURCE_KIND_UNKNOWN = 0,
    SOURCE_KIND_INNER_UNIT = 1,
    SOURCE_KIND_PEN_UNIT = 2,
    SOURCE_KIND_GLUCOMETER = 3,
    SOURCE_KIND_OUTER_GATEWAY = 4
};

struct SourceState {
    SourceKind kind;
    SourceAvailability availability;
    SourceDataQuality dataQuality;
    uint32_t lastSeenMs;
    uint32_t lastFreshMs;
    uint32_t staleAfterMs;
    uint32_t lastSequence;
};

static const uint32_t SOURCE_STALE_FAST_MS = 15000;
static const uint32_t SOURCE_STALE_DEFAULT_MS = 60000;
static const uint32_t SOURCE_STALE_SLOW_MS = 300000;
