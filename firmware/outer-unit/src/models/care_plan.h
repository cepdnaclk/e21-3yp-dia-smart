#pragma once

#include <stdint.h>

enum CarePlanScheduleStatus : uint8_t {
    CARE_PLAN_STATUS_NONE = 0,
    CARE_PLAN_STATUS_UPCOMING,
    CARE_PLAN_STATUS_DUE,
    CARE_PLAN_STATUS_TAKEN,
    CARE_PLAN_STATUS_MISSED
};

struct CarePlanView {
    bool available;
    uint32_t revision;
    uint32_t version;
    uint8_t scheduleCount;
    uint8_t selectedScheduleIndex;
    float doseUnits;
    uint16_t buzzerDurationMinutes;
    uint16_t repeatIntervalMinutes;
    bool manualStopAllowed;
    bool reminderSilenced;
    CarePlanScheduleStatus status;
    char carePlanId[32];
    char effectiveFrom[12];
    char timezone[32];
    char scheduleId[24];
    char period[16];
    char insulinType[48];
    char windowStart[6];
    char targetTime[6];
    char windowEnd[6];
};

struct CarePlanTelemetryEvent {
    uint32_t carePlanVersion;
    uint16_t repeatNumber;
    char eventId[48];
    char eventType[32];
    char scheduleId[24];
    char timestamp[32];
    char windowStart[6];
    char targetTime[6];
    char windowEnd[6];
};

struct CarePlanApplyResult {
    bool accepted;
    uint32_t version;
    char carePlanId[32];
    char message[96];
};
