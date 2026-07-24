#include "care_plan_service.h"

#include <ArduinoJson.h>
#include <Preferences.h>
#include <time.h>

#include "config/app_config.h"
#include "managers/display_state_manager.h"

namespace {
constexpr uint32_t CARE_PLAN_STORAGE_MAGIC = 0x43504C4EU;
constexpr uint16_t CARE_PLAN_STORAGE_VERSION = 1;
constexpr const char* CARE_PLAN_NVS_NAMESPACE = "care_plan";
constexpr const char* CARE_PLAN_NVS_KEY = "active";
constexpr uint32_t MANUAL_SELECTION_HOLD_MS = 30000;

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
uint8_t takenScheduleIndex = 0xFF;
int32_t takenDateKey = -1;
uint8_t notifiedScheduleIndex = 0xFF;
int32_t notifiedDateKey = -1;
uint32_t lastNotificationMs = 0;
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

bool loadPlan(StoredCarePlan& plan) {
    Preferences preferences;
    if (!preferences.begin(CARE_PLAN_NVS_NAMESPACE, true)) {
        return false;
    }
    size_t storedLength = preferences.getBytesLength(CARE_PLAN_NVS_KEY);
    size_t read = storedLength == sizeof(plan)
        ? preferences.getBytes(CARE_PLAN_NVS_KEY, &plan, sizeof(plan))
        : 0;
    preferences.end();
    return read == sizeof(plan) &&
           plan.magic == CARE_PLAN_STORAGE_MAGIC &&
           plan.formatVersion == CARE_PLAN_STORAGE_VERSION &&
           plan.scheduleCount <= CARE_PLAN_MAX_SCHEDULES;
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
                                      int32_t dateKey) {
    if (index >= plan.scheduleCount) {
        return CARE_PLAN_STATUS_NONE;
    }
    if (takenScheduleIndex == index && takenDateKey == dateKey) {
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
    return CARE_PLAN_STATUS_UPCOMING;
}

CarePlanView makeView(const StoredCarePlan& plan,
                      bool available,
                      uint8_t index,
                      CarePlanScheduleStatus status,
                      uint32_t revision) {
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
    takenScheduleIndex = stored.takenScheduleIndex;
    takenDateKey = stored.takenDateKey;
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
    next.takenScheduleIndex = 0xFF;
    next.takenDateKey = -1;
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
    if (currentAvailable &&
        current.version == version &&
        strcmp(current.carePlanId, next.carePlanId) == 0) {
        next.takenScheduleIndex = current.takenScheduleIndex;
        next.takenDateKey = current.takenDateKey;
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
    takenScheduleIndex = next.takenScheduleIndex;
    takenDateKey = next.takenDateKey;
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
        status = scheduleStatus(plan, index, localTime.tm_hour * 60 + localTime.tm_min, dateKey);
    }
    return makeView(plan, available, index, status, revision);
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
    uint8_t dueIndex = bestScheduleIndex(plan, nowMinute, true);
    CarePlanScheduleStatus dueStatus = scheduleStatus(plan, dueIndex, nowMinute, dateKey);
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

    CarePlanScheduleStatus status = scheduleStatus(plan, nextIndex, nowMinute, dateKey);
    updateSelectionAndRevision(nextIndex, status);

    uint32_t repeatMs = (uint32_t)plan.repeatIntervalMinutes * 60UL * 1000UL;
    if (repeatMs == 0) {
        repeatMs = 15UL * 60UL * 1000UL;
    }
    bool firstNotification = notifiedScheduleIndex != dueIndex || notifiedDateKey != dateKey;
    bool repeatNotification = !firstNotification &&
                              (millis() - lastNotificationMs) >= repeatMs;
    if (hasDueSchedule &&
        !(takenScheduleIndex == dueIndex && takenDateKey == dateKey) &&
        (firstNotification || repeatNotification)) {
        portENTER_CRITICAL(&carePlanMux);
        notifiedScheduleIndex = dueIndex;
        notifiedDateKey = dateKey;
        lastNotificationMs = millis();
        viewRevision++;
        portEXIT_CRITICAL(&carePlanMux);
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
        CarePlanScheduleStatus dueStatus = scheduleStatus(plan, dueIndex, nowMinute, dateKey);
        index = dueStatus == CARE_PLAN_STATUS_DUE
            ? dueIndex
            : bestScheduleIndex(plan, nowMinute, false);
        status = scheduleStatus(plan, index, nowMinute, dateKey);
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
    portENTER_CRITICAL(&carePlanMux);
    takenScheduleIndex = index;
    takenDateKey = dateKey;
    activePlan.takenScheduleIndex = index;
    activePlan.takenDateKey = dateKey;
    persisted = activePlan;
    lastViewStatus = CARE_PLAN_STATUS_TAKEN;
    viewRevision++;
    portEXIT_CRITICAL(&carePlanMux);
    if (!savePlan(persisted)) {
        Serial.println("[CarePlan] Warning: taken state was not persisted");
    }
}
