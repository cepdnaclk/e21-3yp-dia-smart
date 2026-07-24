#include "care_plan_service.h"

#include <ArduinoJson.h>
#include <Preferences.h>
#include <esp_system.h>
#include <time.h>

#include "config/app_config.h"
#include "managers/display_state_manager.h"

namespace {
constexpr uint32_t CARE_PLAN_STORAGE_MAGIC = 0x43504C4EU;
constexpr uint16_t CARE_PLAN_STORAGE_VERSION = 3;
constexpr const char* CARE_PLAN_NVS_NAMESPACE = "care_plan";
constexpr const char* CARE_PLAN_NVS_KEY = "active";
constexpr uint32_t MANUAL_SELECTION_HOLD_MS = 30000;
constexpr uint8_t CARE_PLAN_EVENT_QUEUE_LENGTH = 8;

struct StoredCarePlanSchedule {
    float doseUnits;
    char scheduleId[24];
    char period[16];
    char insulinType[48];
    char windowStart[6];
    char targetTime[6];
    char windowEnd[6];
};

struct StoredCarePlan {
    uint32_t magic;
    uint16_t formatVersion;
    uint32_t version;
    uint8_t scheduleCount;
    uint16_t buzzerDurationMinutes;
    uint16_t repeatIntervalMinutes;
    bool manualStopAllowed;
    int32_t takenDateKeys[CARE_PLAN_MAX_SCHEDULES];
    int32_t silencedDateKeys[CARE_PLAN_MAX_SCHEDULES];
    int32_t missedDateKeys[CARE_PLAN_MAX_SCHEDULES];
    char carePlanId[32];
    char patientId[24];
    char outerDeviceId[32];
    char effectiveFrom[12];
    char timezone[32];
    StoredCarePlanSchedule schedules[CARE_PLAN_MAX_SCHEDULES];
};

struct StoredCarePlanV2 {
    uint32_t magic;
    uint16_t formatVersion;
    uint32_t version;
    uint8_t scheduleCount;
    uint16_t buzzerDurationMinutes;
    uint16_t repeatIntervalMinutes;
    bool manualStopAllowed;
    int32_t takenDateKeys[CARE_PLAN_MAX_SCHEDULES];
    char carePlanId[32];
    char patientId[24];
    char outerDeviceId[32];
    char effectiveFrom[12];
    char timezone[32];
    StoredCarePlanSchedule schedules[CARE_PLAN_MAX_SCHEDULES];
};

struct StoredCarePlanV1 {
    uint32_t magic;
    uint16_t formatVersion;
    uint32_t version;
    uint8_t scheduleCount;
    uint16_t buzzerDurationMinutes;
    uint16_t repeatIntervalMinutes;
    bool manualStopAllowed;
    uint8_t takenScheduleIndex;
    int32_t takenDateKey;
    char carePlanId[32];
    char patientId[24];
    char outerDeviceId[32];
    char effectiveFrom[12];
    char timezone[32];
    StoredCarePlanSchedule schedules[CARE_PLAN_MAX_SCHEDULES];
};

StoredCarePlan activePlan = {};
bool hasActivePlan = false;
uint8_t selectedScheduleIndex = 0;
uint32_t viewRevision = 0;
uint32_t manualSelectionUntilMs = 0;
CarePlanScheduleStatus lastViewStatus = CARE_PLAN_STATUS_NONE;
uint8_t notifiedScheduleIndex = 0xFF;
int32_t notifiedDateKey = -1;
uint32_t lastNotificationMs = 0;
uint16_t notificationRepeatNumber = 0;
CarePlanTelemetryEvent pendingEvents[CARE_PLAN_EVENT_QUEUE_LENGTH] = {};
uint8_t pendingEventHead = 0;
uint8_t pendingEventCount = 0;
portMUX_TYPE carePlanMux = portMUX_INITIALIZER_UNLOCKED;

template <size_t N>
void copyText(char (&destination)[N], const char* source) {
    destination[0] = '\0';
    if (source != nullptr) {
        strncpy(destination, source, N - 1);
        destination[N - 1] = '\0';
    }
}

bool validTimeText(const char* value) {
    if (value == nullptr || strlen(value) != 5 || value[2] != ':') {
        return false;
    }
    if (!isdigit((unsigned char)value[0]) ||
        !isdigit((unsigned char)value[1]) ||
        !isdigit((unsigned char)value[3]) ||
        !isdigit((unsigned char)value[4])) {
        return false;
    }
    int hour = (value[0] - '0') * 10 + (value[1] - '0');
    int minute = (value[3] - '0') * 10 + (value[4] - '0');
    return hour >= 0 && hour <= 23 && minute >= 0 && minute <= 59;
}

int minuteOfDay(const char* value) {
    if (!validTimeText(value)) {
        return -1;
    }
    return ((value[0] - '0') * 10 + (value[1] - '0')) * 60 +
           ((value[3] - '0') * 10 + (value[4] - '0'));
}

int32_t timezoneOffsetSeconds(const char* timezone) {
    if (timezone != nullptr && strcmp(timezone, "Asia/Colombo") == 0) {
        return 5 * 60 * 60 + 30 * 60;
    }
    return 0;
}

bool localPlanTime(const StoredCarePlan& plan, tm& localTime, int32_t& dateKey) {
    time_t now = time(nullptr);
    if (now < 1700000000) {
        return false;
    }
    now += timezoneOffsetSeconds(plan.timezone);
    gmtime_r(&now, &localTime);
    dateKey = (localTime.tm_year + 1900) * 1000 + localTime.tm_yday;
    return true;
}

bool planIsEffective(const StoredCarePlan& plan, const tm& localTime) {
    if (strlen(plan.effectiveFrom) != 10) {
        return true;
    }
    char today[12];
    strftime(today, sizeof(today), "%Y-%m-%d", &localTime);
    return strcmp(today, plan.effectiveFrom) >= 0;
}

bool minuteInWindow(int nowMinute, int startMinute, int endMinute) {
    if (startMinute <= endMinute) {
        return nowMinute >= startMinute && nowMinute <= endMinute;
    }
    return nowMinute >= startMinute || nowMinute <= endMinute;
}

bool targetReachedInWindow(int nowMinute,
                           int startMinute,
                           int targetMinute,
                           int endMinute) {
    if (!minuteInWindow(nowMinute, startMinute, endMinute)) {
        return false;
    }
    if (startMinute <= endMinute) {
        return nowMinute >= targetMinute;
    }
    if (targetMinute >= startMinute) {
        return nowMinute >= targetMinute || nowMinute <= endMinute;
    }
    return nowMinute >= targetMinute && nowMinute <= endMinute;
}

int minutesUntil(int nowMinute, int targetMinute) {
    int delta = targetMinute - nowMinute;
    return delta < 0 ? delta + 1440 : delta;
}

bool savePlan(const StoredCarePlan& plan) {
    Preferences preferences;
    if (!preferences.begin(CARE_PLAN_NVS_NAMESPACE, false)) {
        return false;
    }
    size_t written = preferences.putBytes(CARE_PLAN_NVS_KEY, &plan, sizeof(plan));
    preferences.end();
    return written == sizeof(plan);
}

void clearTakenDates(int32_t (&dates)[CARE_PLAN_MAX_SCHEDULES]) {
    for (uint8_t index = 0; index < CARE_PLAN_MAX_SCHEDULES; ++index) {
        dates[index] = -1;
    }
}

bool loadPlan(StoredCarePlan& plan) {
    Preferences preferences;
    if (!preferences.begin(CARE_PLAN_NVS_NAMESPACE, true)) {
        return false;
    }
    size_t storedLength = preferences.getBytesLength(CARE_PLAN_NVS_KEY);
    bool loaded = false;
    if (storedLength == sizeof(plan)) {
        size_t read = preferences.getBytes(CARE_PLAN_NVS_KEY, &plan, sizeof(plan));
        loaded = read == sizeof(plan) &&
                 plan.magic == CARE_PLAN_STORAGE_MAGIC &&
                 plan.formatVersion == CARE_PLAN_STORAGE_VERSION &&
                 plan.scheduleCount <= CARE_PLAN_MAX_SCHEDULES;
    } else if (storedLength == sizeof(StoredCarePlanV2)) {
        StoredCarePlanV2 legacy = {};
        size_t read = preferences.getBytes(
            CARE_PLAN_NVS_KEY, &legacy, sizeof(legacy));
        if (read == sizeof(legacy) &&
            legacy.magic == CARE_PLAN_STORAGE_MAGIC &&
            legacy.formatVersion == 2 &&
            legacy.scheduleCount <= CARE_PLAN_MAX_SCHEDULES) {
            plan = {};
            plan.magic = legacy.magic;
            plan.formatVersion = CARE_PLAN_STORAGE_VERSION;
            plan.version = legacy.version;
            plan.scheduleCount = legacy.scheduleCount;
            plan.buzzerDurationMinutes = legacy.buzzerDurationMinutes;
            plan.repeatIntervalMinutes = legacy.repeatIntervalMinutes;
            plan.manualStopAllowed = legacy.manualStopAllowed;
            memcpy(plan.takenDateKeys,
                   legacy.takenDateKeys,
                   sizeof(plan.takenDateKeys));
            clearTakenDates(plan.silencedDateKeys);
            clearTakenDates(plan.missedDateKeys);
            memcpy(plan.carePlanId, legacy.carePlanId, sizeof(plan.carePlanId));
            memcpy(plan.patientId, legacy.patientId, sizeof(plan.patientId));
            memcpy(plan.outerDeviceId, legacy.outerDeviceId, sizeof(plan.outerDeviceId));
            memcpy(plan.effectiveFrom, legacy.effectiveFrom, sizeof(plan.effectiveFrom));
            memcpy(plan.timezone, legacy.timezone, sizeof(plan.timezone));
            memcpy(plan.schedules, legacy.schedules, sizeof(plan.schedules));
            loaded = true;
        }
    } else if (storedLength == sizeof(StoredCarePlanV1)) {
        StoredCarePlanV1 legacy = {};
        size_t read = preferences.getBytes(
            CARE_PLAN_NVS_KEY, &legacy, sizeof(legacy));
        if (read == sizeof(legacy) &&
            legacy.magic == CARE_PLAN_STORAGE_MAGIC &&
            legacy.formatVersion == 1 &&
            legacy.scheduleCount <= CARE_PLAN_MAX_SCHEDULES) {
            plan = {};
            plan.magic = legacy.magic;
            plan.formatVersion = CARE_PLAN_STORAGE_VERSION;
            plan.version = legacy.version;
            plan.scheduleCount = legacy.scheduleCount;
            plan.buzzerDurationMinutes = legacy.buzzerDurationMinutes;
            plan.repeatIntervalMinutes = legacy.repeatIntervalMinutes;
            plan.manualStopAllowed = legacy.manualStopAllowed;
            clearTakenDates(plan.takenDateKeys);
            clearTakenDates(plan.silencedDateKeys);
            clearTakenDates(plan.missedDateKeys);
            if (legacy.takenScheduleIndex < legacy.scheduleCount) {
                plan.takenDateKeys[legacy.takenScheduleIndex] = legacy.takenDateKey;
            }
            memcpy(plan.carePlanId, legacy.carePlanId, sizeof(plan.carePlanId));
            memcpy(plan.patientId, legacy.patientId, sizeof(plan.patientId));
            memcpy(plan.outerDeviceId, legacy.outerDeviceId, sizeof(plan.outerDeviceId));
            memcpy(plan.effectiveFrom, legacy.effectiveFrom, sizeof(plan.effectiveFrom));
            memcpy(plan.timezone, legacy.timezone, sizeof(plan.timezone));
            memcpy(plan.schedules, legacy.schedules, sizeof(plan.schedules));
            loaded = true;
        }
    }
    preferences.end();
    return loaded;
}

bool isLeapYear(int year) {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
}

int32_t previousDateKey(const tm& localTime) {
    int year = localTime.tm_year + 1900;
    if (localTime.tm_yday > 0) {
        return year * 1000 + localTime.tm_yday - 1;
    }
    int previousYear = year - 1;
    return previousYear * 1000 + (isLeapYear(previousYear) ? 365 : 364);
}

int32_t dateKeyFromIso(const char* value) {
    if (value == nullptr || strlen(value) != 10 ||
        value[4] != '-' || value[7] != '-') {
        return -1;
    }
    int year = 0;
    int month = 0;
    int day = 0;
    if (sscanf(value, "%4d-%2d-%2d", &year, &month, &day) != 3 ||
        month < 1 || month > 12 || day < 1 || day > 31) {
        return -1;
    }

    static const uint16_t daysBeforeMonth[12] = {
        0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334
    };
    int dayOfYear = daysBeforeMonth[month - 1] + day - 1;
    if (month > 2 && isLeapYear(year)) {
        dayOfYear++;
    }
    return year * 1000 + dayOfYear;
}

int32_t scheduleDateKey(const StoredCarePlanSchedule& schedule,
                        int nowMinute,
                        int32_t currentDateKey,
                        const tm& localTime) {
    int start = minuteOfDay(schedule.windowStart);
    int end = minuteOfDay(schedule.windowEnd);
    if (start > end && nowMinute <= end) {
        return previousDateKey(localTime);
    }
    return currentDateKey;
}

bool missedOccurrenceDateKey(const StoredCarePlanSchedule& schedule,
                             int nowMinute,
                             int32_t currentDateKey,
                             const tm& localTime,
                             int32_t& occurrenceDateKey) {
    int start = minuteOfDay(schedule.windowStart);
    int end = minuteOfDay(schedule.windowEnd);
    if (start < 0 || end < 0) {
        return false;
    }
    if (start <= end) {
        if (nowMinute <= end) {
            return false;
        }
        occurrenceDateKey = currentDateKey;
        return true;
    }
    if (nowMinute > end && nowMinute < start) {
        occurrenceDateKey = previousDateKey(localTime);
        return true;
    }
    return false;
}

void currentTimestamp(char* output, size_t outputLength) {
    time_t now = time(nullptr);
    tm utcTime = {};
    if (now >= 1700000000 && gmtime_r(&now, &utcTime) != nullptr) {
        strftime(output, outputLength, "%Y-%m-%dT%H:%M:%SZ", &utcTime);
        return;
    }
    strncpy(output, "1970-01-01T00:00:00Z", outputLength - 1);
    output[outputLength - 1] = '\0';
}

bool queueCarePlanTelemetry(const StoredCarePlan& plan,
                            uint8_t scheduleIndex,
                            const char* eventType,
                            uint16_t repeatNumber) {
    if (scheduleIndex >= plan.scheduleCount) {
        return false;
    }

    CarePlanTelemetryEvent event = {};
    event.carePlanVersion = plan.version;
    event.repeatNumber = repeatNumber;
    snprintf(event.eventId,
             sizeof(event.eventId),
             "REM-%lu-%08lX",
             (unsigned long)time(nullptr),
             (unsigned long)esp_random());
    copyText(event.eventType, eventType);
    copyText(event.scheduleId, plan.schedules[scheduleIndex].scheduleId);
    copyText(event.windowStart, plan.schedules[scheduleIndex].windowStart);
    copyText(event.targetTime, plan.schedules[scheduleIndex].targetTime);
    copyText(event.windowEnd, plan.schedules[scheduleIndex].windowEnd);
    currentTimestamp(event.timestamp, sizeof(event.timestamp));

    bool queued = false;
    portENTER_CRITICAL(&carePlanMux);
    if (pendingEventCount < CARE_PLAN_EVENT_QUEUE_LENGTH) {
        uint8_t tail =
            (pendingEventHead + pendingEventCount) % CARE_PLAN_EVENT_QUEUE_LENGTH;
        pendingEvents[tail] = event;
        pendingEventCount++;
        queued = true;
    }
    portEXIT_CRITICAL(&carePlanMux);

    if (!queued) {
        Serial.printf("[CarePlan] Event queue full; dropped %s\n", eventType);
    }
    return queued;
}

uint8_t bestScheduleIndex(const StoredCarePlan& plan,
                          int nowMinute,
                          bool requireDue) {
    uint8_t bestIndex = 0;
    int bestDelta = 1441;

    for (uint8_t index = 0; index < plan.scheduleCount; ++index) {
        int start = minuteOfDay(plan.schedules[index].windowStart);
        int target = minuteOfDay(plan.schedules[index].targetTime);
        int end = minuteOfDay(plan.schedules[index].windowEnd);
        bool due = start >= 0 && target >= 0 && end >= 0 &&
                   targetReachedInWindow(nowMinute, start, target, end);

        if (requireDue && due) {
            return index;
        }
        if (!requireDue && target >= 0) {
            int delta = minutesUntil(nowMinute, target);
            if (delta < bestDelta) {
                bestDelta = delta;
                bestIndex = index;
            }
        }
    }
    return bestIndex;
}

CarePlanScheduleStatus scheduleStatus(const StoredCarePlan& plan,
                                      uint8_t index,
                                      int nowMinute,
                                      int32_t dateKey,
                                      const tm& localTime) {
    if (index >= plan.scheduleCount) {
        return CARE_PLAN_STATUS_NONE;
    }
    int32_t occurrenceDateKey = scheduleDateKey(
        plan.schedules[index], nowMinute, dateKey, localTime);
    if (plan.takenDateKeys[index] == occurrenceDateKey) {
        return CARE_PLAN_STATUS_TAKEN;
    }

    const StoredCarePlanSchedule& schedule = plan.schedules[index];
    int start = minuteOfDay(schedule.windowStart);
    int target = minuteOfDay(schedule.targetTime);
    int end = minuteOfDay(schedule.windowEnd);
    if (start >= 0 && target >= 0 && end >= 0 &&
        targetReachedInWindow(nowMinute, start, target, end)) {
        return CARE_PLAN_STATUS_DUE;
    }
    int32_t missedDateKey = -1;
    if (missedOccurrenceDateKey(
            schedule, nowMinute, dateKey, localTime, missedDateKey) &&
        plan.missedDateKeys[index] == missedDateKey) {
        return CARE_PLAN_STATUS_MISSED;
    }
    return CARE_PLAN_STATUS_UPCOMING;
}

CarePlanView makeView(const StoredCarePlan& plan,
                      bool available,
                      uint8_t index,
                      CarePlanScheduleStatus status,
                      uint32_t revision,
                      bool reminderSilenced) {
    CarePlanView view = {};
    view.available = available;
    view.revision = revision;
    view.status = available ? status : CARE_PLAN_STATUS_NONE;
    if (!available) {
        return view;
    }

    view.version = plan.version;
    view.scheduleCount = plan.scheduleCount;
    view.selectedScheduleIndex = index;
    view.buzzerDurationMinutes = plan.buzzerDurationMinutes;
    view.repeatIntervalMinutes = plan.repeatIntervalMinutes;
    view.manualStopAllowed = plan.manualStopAllowed;
    view.reminderSilenced = reminderSilenced;
    copyText(view.carePlanId, plan.carePlanId);
    copyText(view.effectiveFrom, plan.effectiveFrom);
    copyText(view.timezone, plan.timezone);

    if (index < plan.scheduleCount) {
        const StoredCarePlanSchedule& schedule = plan.schedules[index];
        view.doseUnits = schedule.doseUnits;
        copyText(view.scheduleId, schedule.scheduleId);
        copyText(view.period, schedule.period);
        copyText(view.insulinType, schedule.insulinType);
        copyText(view.windowStart, schedule.windowStart);
        copyText(view.targetTime, schedule.targetTime);
        copyText(view.windowEnd, schedule.windowEnd);
    }
    return view;
}

void updateSelectionAndRevision(uint8_t index, CarePlanScheduleStatus status) {
    portENTER_CRITICAL(&carePlanMux);
    bool changed = selectedScheduleIndex != index || lastViewStatus != status;
    selectedScheduleIndex = index;
    lastViewStatus = status;
    if (changed) {
        viewRevision++;
    }
    portEXIT_CRITICAL(&carePlanMux);
}

CarePlanApplyResult rejectedResult(const char* message) {
    CarePlanApplyResult result = {};
    result.accepted = false;
    copyText(result.message, message);
    return result;
}
}

bool setupCarePlanService() {
    StoredCarePlan stored = {};
    if (!loadPlan(stored)) {
        Serial.println("[CarePlan] No valid stored Care Plan");
        return false;
    }

    portENTER_CRITICAL(&carePlanMux);
    activePlan = stored;
    hasActivePlan = true;
    selectedScheduleIndex = 0;
    lastViewStatus = CARE_PLAN_STATUS_UPCOMING;
    viewRevision++;
    portEXIT_CRITICAL(&carePlanMux);

    carePlanFocusCurrentSchedule();
    Serial.printf("[CarePlan] Restored %s v%lu with %u schedules\n",
                  stored.carePlanId,
                  (unsigned long)stored.version,
                  stored.scheduleCount);
    return true;
}

CarePlanApplyResult applyCarePlanPayload(const uint8_t* payload, size_t length) {
    CarePlanApplyResult result = {};
    if (payload == nullptr || length == 0 || length >= MQTT_BUFFER_BYTES) {
        return rejectedResult("Care Plan payload is empty or too large");
    }

    JsonDocument document;
    DeserializationError error = deserializeJson(document, payload, length);
    if (error) {
        return rejectedResult("Care Plan JSON could not be parsed");
    }

    const char* carePlanId = document["carePlanId"] | "";
    const char* outerDeviceId = document["outerDeviceId"] | "";
    const char* patientId = document["patientId"] | "";
    const char* timezone = document["timezone"] | "UTC";
    const char* effectiveFrom = document["effectiveFrom"] | "";
    uint32_t version = document["version"] | 0;

    copyText(result.carePlanId, carePlanId);
    result.version = version;

    if (carePlanId[0] == '\0' || version == 0) {
        copyText(result.message, "Missing Care Plan ID or version");
        return result;
    }
    if (strcmp(outerDeviceId, DEVICE_UID_OUTER) != 0) {
        copyText(result.message, "Care Plan is for another Outer Unit");
        return result;
    }

    JsonArrayConst schedules = document["schedules"].as<JsonArrayConst>();
    if (schedules.isNull() || schedules.size() > CARE_PLAN_MAX_SCHEDULES) {
        copyText(result.message, "Invalid or unsupported schedule count");
        return result;
    }

    StoredCarePlan next = {};
    next.magic = CARE_PLAN_STORAGE_MAGIC;
    next.formatVersion = CARE_PLAN_STORAGE_VERSION;
    next.version = version;
    next.scheduleCount = (uint8_t)schedules.size();
    clearTakenDates(next.takenDateKeys);
    clearTakenDates(next.silencedDateKeys);
    clearTakenDates(next.missedDateKeys);
    copyText(next.carePlanId, carePlanId);
    copyText(next.patientId, patientId);
    copyText(next.outerDeviceId, outerDeviceId);
    copyText(next.effectiveFrom, effectiveFrom);
    copyText(next.timezone, timezone);

    JsonObjectConst reminders = document["reminderSettings"].as<JsonObjectConst>();
    next.buzzerDurationMinutes = reminders["buzzerDurationMinutes"] | 3;
    next.repeatIntervalMinutes = reminders["repeatIntervalMinutes"] | 15;
    next.manualStopAllowed = reminders["manualStopAllowed"] | true;

    uint8_t index = 0;
    for (JsonObjectConst schedule : schedules) {
        StoredCarePlanSchedule& destination = next.schedules[index++];
        const char* scheduleId = schedule["scheduleId"] | "";
        const char* period = schedule["period"] | "";
        const char* insulinType = schedule["insulinType"] | "";
        const char* windowStart = schedule["windowStart"] | "";
        const char* targetTime = schedule["targetTime"] | "";
        const char* windowEnd = schedule["windowEnd"] | "";
        float doseUnits = schedule["doseUnits"] | 0.0f;

        if (scheduleId[0] == '\0' ||
            insulinType[0] == '\0' ||
            doseUnits <= 0.0f ||
            !validTimeText(windowStart) ||
            !validTimeText(targetTime) ||
            !validTimeText(windowEnd)) {
            copyText(result.message, "A Care Plan schedule is incomplete");
            return result;
        }

        destination.doseUnits = doseUnits;
        copyText(destination.scheduleId, scheduleId);
        copyText(destination.period, period);
        copyText(destination.insulinType, insulinType);
        copyText(destination.windowStart, windowStart);
        copyText(destination.targetTime, targetTime);
        copyText(destination.windowEnd, windowEnd);
    }

    StoredCarePlan current = {};
    bool currentAvailable;
    portENTER_CRITICAL(&carePlanMux);
    current = activePlan;
    currentAvailable = hasActivePlan;
    portEXIT_CRITICAL(&carePlanMux);
    uint32_t currentVersion = currentAvailable ? current.version : 0;
    if (currentVersion > version) {
        copyText(result.message, "Care Plan version is older than stored version");
        return result;
    }
    if (currentAvailable) {
        for (uint8_t nextIndex = 0; nextIndex < next.scheduleCount; ++nextIndex) {
            for (uint8_t currentIndex = 0;
                 currentIndex < current.scheduleCount;
                 ++currentIndex) {
                if (strcmp(next.schedules[nextIndex].scheduleId,
                           current.schedules[currentIndex].scheduleId) == 0) {
                    next.takenDateKeys[nextIndex] =
                        current.takenDateKeys[currentIndex];
                    next.silencedDateKeys[nextIndex] =
                        current.silencedDateKeys[currentIndex];
                    next.missedDateKeys[nextIndex] =
                        current.missedDateKeys[currentIndex];
                    break;
                }
            }
        }
    }

    if (!savePlan(next)) {
        copyText(result.message, "Care Plan could not be stored");
        return result;
    }

    portENTER_CRITICAL(&carePlanMux);
    activePlan = next;
    hasActivePlan = true;
    selectedScheduleIndex = 0;
    lastViewStatus = CARE_PLAN_STATUS_UPCOMING;
    viewRevision++;
    portEXIT_CRITICAL(&carePlanMux);

    carePlanFocusCurrentSchedule();
    result.accepted = true;
    copyText(result.message, "Care Plan stored and displayed");
    Serial.printf("[CarePlan] Applied %s v%lu with %u schedules\n",
                  next.carePlanId,
                  (unsigned long)next.version,
                  next.scheduleCount);
    return result;
}

CarePlanView getCarePlanViewSnapshot() {
    StoredCarePlan plan = {};
    bool available;
    uint8_t index;
    uint32_t revision;

    portENTER_CRITICAL(&carePlanMux);
    plan = activePlan;
    available = hasActivePlan;
    index = selectedScheduleIndex;
    revision = viewRevision;
    portEXIT_CRITICAL(&carePlanMux);

    tm localTime = {};
    int32_t dateKey = -1;
    CarePlanScheduleStatus status = CARE_PLAN_STATUS_UPCOMING;
    if (!available) {
        status = CARE_PLAN_STATUS_NONE;
    } else if (localPlanTime(plan, localTime, dateKey)) {
        status = scheduleStatus(
            plan, index, localTime.tm_hour * 60 + localTime.tm_min, dateKey, localTime);
    }
    bool reminderSilenced = false;
    if (available && index < plan.scheduleCount && dateKey >= 0) {
        int nowMinute = localTime.tm_hour * 60 + localTime.tm_min;
        int32_t occurrenceDateKey = scheduleDateKey(
            plan.schedules[index], nowMinute, dateKey, localTime);
        reminderSilenced =
            plan.silencedDateKeys[index] == occurrenceDateKey;
    }
    return makeView(
        plan, available, index, status, revision, reminderSilenced);
}

void carePlanTick() {
    StoredCarePlan plan = {};
    bool available;
    portENTER_CRITICAL(&carePlanMux);
    plan = activePlan;
    available = hasActivePlan;
    portEXIT_CRITICAL(&carePlanMux);
    if (!available || plan.scheduleCount == 0) {
        return;
    }

    tm localTime = {};
    int32_t dateKey = -1;
    if (!localPlanTime(plan, localTime, dateKey)) {
        return;
    }
    if (!planIsEffective(plan, localTime)) {
        return;
    }
    int nowMinute = localTime.tm_hour * 60 + localTime.tm_min;

    bool missedStateChanged = false;
    int32_t effectiveDateKey = dateKeyFromIso(plan.effectiveFrom);
    for (uint8_t index = 0; index < plan.scheduleCount; ++index) {
        int32_t occurrenceDateKey = -1;
        if (!missedOccurrenceDateKey(
                plan.schedules[index],
                nowMinute,
                dateKey,
                localTime,
                occurrenceDateKey) ||
            (effectiveDateKey >= 0 && occurrenceDateKey < effectiveDateKey) ||
            plan.takenDateKeys[index] == occurrenceDateKey ||
            plan.missedDateKeys[index] == occurrenceDateKey) {
            continue;
        }
        if (queueCarePlanTelemetry(plan, index, "DOSE_MISSED", 0)) {
            plan.missedDateKeys[index] = occurrenceDateKey;
            missedStateChanged = true;
            Serial.printf("[CarePlan] Dose missed: %s\n",
                          plan.schedules[index].scheduleId);
        }
    }

    if (missedStateChanged) {
        StoredCarePlan persisted = {};
        bool shouldSave = false;
        portENTER_CRITICAL(&carePlanMux);
        if (hasActivePlan &&
            activePlan.version == plan.version &&
            strcmp(activePlan.carePlanId, plan.carePlanId) == 0) {
            memcpy(activePlan.missedDateKeys,
                   plan.missedDateKeys,
                   sizeof(activePlan.missedDateKeys));
            persisted = activePlan;
            viewRevision++;
            shouldSave = true;
        }
        portEXIT_CRITICAL(&carePlanMux);
        if (shouldSave && !savePlan(persisted)) {
            Serial.println("[CarePlan] Warning: missed state was not persisted");
        }
    }

    uint8_t dueIndex = bestScheduleIndex(plan, nowMinute, true);
    CarePlanScheduleStatus dueStatus =
        scheduleStatus(plan, dueIndex, nowMinute, dateKey, localTime);
    bool hasDueSchedule = dueStatus == CARE_PLAN_STATUS_DUE;

    uint8_t nextIndex;
    portENTER_CRITICAL(&carePlanMux);
    nextIndex = selectedScheduleIndex;
    bool manualSelectionActive = (int32_t)(manualSelectionUntilMs - millis()) > 0;
    portEXIT_CRITICAL(&carePlanMux);

    if (hasDueSchedule) {
        nextIndex = dueIndex;
    } else if (!manualSelectionActive) {
        nextIndex = bestScheduleIndex(plan, nowMinute, false);
    }

    CarePlanScheduleStatus status =
        scheduleStatus(plan, nextIndex, nowMinute, dateKey, localTime);
    updateSelectionAndRevision(nextIndex, status);

    uint32_t repeatMs = (uint32_t)plan.repeatIntervalMinutes * 60UL * 1000UL;
    if (repeatMs == 0) {
        repeatMs = 15UL * 60UL * 1000UL;
    }
    int32_t dueDateKey = scheduleDateKey(
        plan.schedules[dueIndex], nowMinute, dateKey, localTime);
    bool reminderSilenced =
        plan.silencedDateKeys[dueIndex] == dueDateKey;

    uint8_t previousNotifiedIndex;
    int32_t previousNotifiedDateKey;
    uint32_t previousNotificationMs;
    portENTER_CRITICAL(&carePlanMux);
    previousNotifiedIndex = notifiedScheduleIndex;
    previousNotifiedDateKey = notifiedDateKey;
    previousNotificationMs = lastNotificationMs;
    portEXIT_CRITICAL(&carePlanMux);

    bool firstNotification =
        previousNotifiedIndex != dueIndex ||
        previousNotifiedDateKey != dueDateKey;
    bool repeatNotification = !firstNotification &&
                              (millis() - previousNotificationMs) >= repeatMs;
    if (hasDueSchedule &&
        plan.takenDateKeys[dueIndex] != dueDateKey &&
        !reminderSilenced &&
        (firstNotification || repeatNotification)) {
        uint16_t repeatNumber = 0;
        portENTER_CRITICAL(&carePlanMux);
        if (firstNotification) {
            notificationRepeatNumber = 0;
        } else {
            notificationRepeatNumber++;
        }
        repeatNumber = notificationRepeatNumber;
        notifiedScheduleIndex = dueIndex;
        notifiedDateKey = dueDateKey;
        lastNotificationMs = millis();
        viewRevision++;
        portEXIT_CRITICAL(&carePlanMux);
        queueCarePlanTelemetry(
            plan,
            dueIndex,
            firstNotification ? "REMINDER_STARTED" : "REMINDER_REPEATED",
            repeatNumber);
        updateDisplayPage(DISPLAY_PAGE_PRESCRIPTION);
        Serial.printf("[CarePlan] Dose due: %s %.1fU at %s\n",
                      plan.schedules[dueIndex].insulinType,
                      plan.schedules[dueIndex].doseUnits,
                      plan.schedules[dueIndex].targetTime);
    }
}

void carePlanSelectPreviousSchedule() {
    portENTER_CRITICAL(&carePlanMux);
    if (hasActivePlan && activePlan.scheduleCount > 0) {
        selectedScheduleIndex = selectedScheduleIndex == 0
            ? activePlan.scheduleCount - 1
            : selectedScheduleIndex - 1;
        manualSelectionUntilMs = millis() + MANUAL_SELECTION_HOLD_MS;
        viewRevision++;
    }
    portEXIT_CRITICAL(&carePlanMux);
}

void carePlanSelectNextSchedule() {
    portENTER_CRITICAL(&carePlanMux);
    if (hasActivePlan && activePlan.scheduleCount > 0) {
        selectedScheduleIndex = (selectedScheduleIndex + 1) % activePlan.scheduleCount;
        manualSelectionUntilMs = millis() + MANUAL_SELECTION_HOLD_MS;
        viewRevision++;
    }
    portEXIT_CRITICAL(&carePlanMux);
}

void carePlanFocusCurrentSchedule() {
    StoredCarePlan plan = {};
    bool available;
    portENTER_CRITICAL(&carePlanMux);
    plan = activePlan;
    available = hasActivePlan;
    portEXIT_CRITICAL(&carePlanMux);
    if (!available || plan.scheduleCount == 0) {
        return;
    }

    tm localTime = {};
    int32_t dateKey = -1;
    uint8_t index = 0;
    CarePlanScheduleStatus status = CARE_PLAN_STATUS_UPCOMING;
    if (localPlanTime(plan, localTime, dateKey)) {
        int nowMinute = localTime.tm_hour * 60 + localTime.tm_min;
        uint8_t dueIndex = bestScheduleIndex(plan, nowMinute, true);
        CarePlanScheduleStatus dueStatus =
            scheduleStatus(plan, dueIndex, nowMinute, dateKey, localTime);
        index = dueStatus == CARE_PLAN_STATUS_DUE
            ? dueIndex
            : bestScheduleIndex(plan, nowMinute, false);
        status = scheduleStatus(plan, index, nowMinute, dateKey, localTime);
    }
    updateSelectionAndRevision(index, status);
}

void carePlanMarkDoseTaken(float doseUnits) {
    (void)doseUnits;
    StoredCarePlan plan = {};
    bool available;
    uint8_t index;
    portENTER_CRITICAL(&carePlanMux);
    plan = activePlan;
    available = hasActivePlan;
    index = selectedScheduleIndex;
    portEXIT_CRITICAL(&carePlanMux);
    if (!available || index >= plan.scheduleCount) {
        return;
    }

    tm localTime = {};
    int32_t dateKey = -1;
    if (!localPlanTime(plan, localTime, dateKey)) {
        return;
    }
    const StoredCarePlanSchedule& schedule = plan.schedules[index];
    int start = minuteOfDay(schedule.windowStart);
    int end = minuteOfDay(schedule.windowEnd);
    int nowMinute = localTime.tm_hour * 60 + localTime.tm_min;
    if (start < 0 || end < 0 || !minuteInWindow(nowMinute, start, end)) {
        Serial.println("[CarePlan] Confirmed dose is outside the selected schedule window");
        return;
    }

    StoredCarePlan persisted = {};
    int32_t occurrenceDateKey =
        scheduleDateKey(schedule, nowMinute, dateKey, localTime);
    bool possibleDoubleDose =
        plan.takenDateKeys[index] == occurrenceDateKey;
    portENTER_CRITICAL(&carePlanMux);
    activePlan.takenDateKeys[index] = occurrenceDateKey;
    persisted = activePlan;
    lastViewStatus = CARE_PLAN_STATUS_TAKEN;
    viewRevision++;
    portEXIT_CRITICAL(&carePlanMux);
    if (!savePlan(persisted)) {
        Serial.println("[CarePlan] Warning: taken state was not persisted");
    }
    if (possibleDoubleDose) {
        queueCarePlanTelemetry(
            plan, index, "POSSIBLE_DOUBLE_DOSE", 0);
        Serial.printf("[CarePlan] Possible double dose: %s\n",
                      schedule.scheduleId);
    }
}

bool carePlanStopReminder() {
    StoredCarePlan plan = {};
    bool available;
    uint8_t index;
    portENTER_CRITICAL(&carePlanMux);
    plan = activePlan;
    available = hasActivePlan;
    index = selectedScheduleIndex;
    portEXIT_CRITICAL(&carePlanMux);
    if (!available ||
        !plan.manualStopAllowed ||
        index >= plan.scheduleCount) {
        return false;
    }

    tm localTime = {};
    int32_t dateKey = -1;
    if (!localPlanTime(plan, localTime, dateKey)) {
        return false;
    }
    int nowMinute = localTime.tm_hour * 60 + localTime.tm_min;
    if (scheduleStatus(plan, index, nowMinute, dateKey, localTime) !=
        CARE_PLAN_STATUS_DUE) {
        return false;
    }

    int32_t occurrenceDateKey = scheduleDateKey(
        plan.schedules[index], nowMinute, dateKey, localTime);
    if (plan.silencedDateKeys[index] == occurrenceDateKey) {
        return false;
    }

    StoredCarePlan persisted = {};
    bool updated = false;
    portENTER_CRITICAL(&carePlanMux);
    if (hasActivePlan &&
        activePlan.version == plan.version &&
        strcmp(activePlan.carePlanId, plan.carePlanId) == 0) {
        activePlan.silencedDateKeys[index] = occurrenceDateKey;
        persisted = activePlan;
        viewRevision++;
        updated = true;
    }
    portEXIT_CRITICAL(&carePlanMux);
    if (!updated) {
        return false;
    }
    if (!savePlan(persisted)) {
        Serial.println("[CarePlan] Warning: reminder stop was not persisted");
    }
    queueCarePlanTelemetry(
        plan, index, "REMINDER_MANUALLY_STOPPED", 0);
    Serial.printf("[CarePlan] Reminder stopped: %s\n",
                  plan.schedules[index].scheduleId);
    return true;
}

bool takePendingCarePlanTelemetry(CarePlanTelemetryEvent& event) {
    bool available = false;
    portENTER_CRITICAL(&carePlanMux);
    if (pendingEventCount > 0) {
        event = pendingEvents[pendingEventHead];
        pendingEventHead =
            (pendingEventHead + 1) % CARE_PLAN_EVENT_QUEUE_LENGTH;
        pendingEventCount--;
        available = true;
    }
    portEXIT_CRITICAL(&carePlanMux);
    return available;
}
