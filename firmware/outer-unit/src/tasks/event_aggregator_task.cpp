#include <Arduino.h>
#include <WiFi.h>
#include <math.h>
#include <string.h>
#include <time.h>
#include <esp_system.h>
#include "models/telemetry_event.h"
#include "include/system_queues.h"
#include "config/app_config.h"
#include "managers/display_state_manager.h"
#include "services/care_plan_service.h"

// Shared variable written by bleManagerTask, read here for battery section.
// Declared extern in ble_manager.cpp.
extern volatile int g_lastBleRssi;
extern volatile uint32_t g_espNowRxTotal;
extern volatile uint32_t g_espNowRxQueued;
extern volatile uint32_t g_espNowRxLenDrop;
extern volatile uint32_t g_espNowRxMagicDrop;
extern volatile uint32_t g_espNowRxQueueDrop;
extern volatile uint32_t g_espNowRxDuplicateDrop;

// ---- ISO-8601 timestamp helper ------------------------------------------- //
static void getTimestamp(char* buf, size_t len) {
    struct tm ti;
    if (getLocalTime(&ti)) {
        strftime(buf, len, "%Y-%m-%dT%H:%M:%SZ", &ti);
    } else {
        // NTP not available — use millis as a fallback marker
        snprintf(buf, len, "1970-01-01T%08luZ", millis() / 1000);
    }
}

static void generateEventId(char* buf, size_t len) {
    static char lastMinuteKey[13] = "";
    static uint16_t minuteCounter = 0;
    static uint16_t fallbackCounter = 0;
    static uint32_t bootNonce = esp_random();

    struct tm ti;
    if (getLocalTime(&ti)) {
        char minuteKey[13];
        strftime(minuteKey, sizeof(minuteKey), "%Y%m%d%H%M", &ti);

        if (strncmp(lastMinuteKey, minuteKey, sizeof(lastMinuteKey)) != 0) {
            strncpy(lastMinuteKey, minuteKey, sizeof(lastMinuteKey));
            lastMinuteKey[sizeof(lastMinuteKey) - 1] = '\0';
            minuteCounter = 0;
        }

        if (minuteCounter < 9999) {
            minuteCounter++;
        }

        snprintf(buf, len, "%s%04u", minuteKey, minuteCounter);
        return;
    }

    fallbackCounter++;
    snprintf(buf,
             len,
             "BOOT%08lX%08lu%04u",
             (unsigned long)bootNonce,
             (unsigned long)(millis() / 1000),
             fallbackCounter);
}

static bool validDelta(float current, float previous, float threshold) {
    if (isnan(current) || isnan(previous)) {
        return isnan(current) != isnan(previous);
    }
    return fabsf(current - previous) >= threshold;
}

static bool innerPacketShouldPublish(const InnerPacket& current,
                                     const InnerPacket& previous,
                                     bool hasPrevious) {
    if (!hasPrevious) return true;
    if (current.doorOpen != previous.doorOpen) return true;
    if (validDelta(current.temperatureC, previous.temperatureC, INNER_TEMP_EVENT_DELTA_C)) return true;
    if (fabsf(current.weightG - previous.weightG) >= INNER_WEIGHT_EVENT_DELTA_G) return true;
    if (fabsf(current.estimatedPercent - previous.estimatedPercent) >= INNER_INVENTORY_EVENT_DELTA_PERCENT) return true;
    if (current.batteryPercent <= INNER_BATTERY_LOW_PERCENT &&
        previous.batteryPercent > INNER_BATTERY_LOW_PERCENT) {
        return true;
    }
    return false;
}

struct PendingDoseConfirmation {
    bool active;
    bool editing;
    DoseReading original;
    int roundedUnits;
    char editBuffer[DOSE_EDIT_MAX_DIGITS + 1];
    uint8_t editLen;
    uint32_t startedAtMs;
};

static int roundedDoseUnits(float doseUnits) {
    int units = (int)lroundf(doseUnits);
    if (units < 1) units = 1;
    if (units > DOSE_CONFIRM_MAX_UNITS) units = DOSE_CONFIRM_MAX_UNITS;
    return units;
}

static bool sameDoseReading(const DoseReading& left, const DoseReading& right) {
    if (left.hasPenTakenEpoch && right.hasPenTakenEpoch) {
        return left.penRecordSlot == right.penRecordSlot &&
               left.penTakenEpochSec == right.penTakenEpochSec;
    }

    return fabsf(left.doseUnits - right.doseUnits) < 0.05f &&
           strncmp(left.injectedAt, right.injectedAt, sizeof(left.injectedAt)) == 0;
}

static uint8_t dosePromptRemainingSec(const PendingDoseConfirmation& pending, uint32_t nowMs) {
    uint32_t elapsedMs = nowMs - pending.startedAtMs;
    if (elapsedMs >= DOSE_CONFIRM_TIMEOUT_MS) {
        return 0;
    }
    return (uint8_t)((DOSE_CONFIRM_TIMEOUT_MS - elapsedMs + 999) / 1000);
}

static void refreshDosePrompt(const PendingDoseConfirmation& pending, uint32_t nowMs) {
    updateDisplayDosePrompt(pending.active,
                            pending.editing,
                            pending.original.doseUnits,
                            pending.roundedUnits,
                            roundedDoseUnits(pending.original.doseUnits),
                            dosePromptRemainingSec(pending, nowMs),
                            pending.editBuffer);
}

static void clearDosePrompt() {
    updateDisplayDosePrompt(false, false, 0.0f, 0, 0, 0, "");
}

static void startDosePrompt(PendingDoseConfirmation& pending, const DoseReading& doseReading) {
    carePlanFocusCurrentSchedule();
    pending = {};
    pending.active = true;
    pending.editing = false;
    pending.original = doseReading;
    pending.roundedUnits = roundedDoseUnits(doseReading.doseUnits);
    pending.startedAtMs = millis();
    refreshDosePrompt(pending, pending.startedAtMs);
    Serial.printf("[EventAgg] Dose pending confirmation: raw=%.1f rounded=%d timeout=%lus\n",
                  doseReading.doseUnits,
                  pending.roundedUnits,
                  (unsigned long)(DOSE_CONFIRM_TIMEOUT_MS / 1000));
}

// -------------------------------------------------------------------------- //

void eventAggregatorTask(void* parameter) {
    // ---- Last-known values (carry-forward if no fresh packet arrives) ---- //
    InnerPacket  lastInner   = {};
    lastInner.temperatureC   = NAN;
    lastInner.doorOpen       = 0;
    lastInner.weightG        = 0.0f;
    lastInner.estimatedPercent = 0.0f;
    lastInner.batteryVoltageV = 0.0f;
    lastInner.batteryPercent = 0;
    InnerPacket lastPublishedInner = lastInner;
    bool hasInnerSnapshot = false;
    bool hasPublishedInner = false;
    PendingDoseConfirmation pendingDose = {};
    DoseReading deferredDose = {};
    bool hasDeferredDose = false;
    DoseReading lastResolvedDose = {};
    bool hasLastResolvedDose = false;

    GlucoseReading lastGlucose = {};
    lastGlucose.valueMgDl      = 0;
    lastGlucose.sequenceNumber = 0;

    DoseReading lastDose = {};
    lastDose.doseUnits   = 0.0f;
    strncpy(lastDose.injectedAt, "1970-01-01T00:00:00Z", sizeof(lastDose.injectedAt));
    DoseReading doseToPublish = {};
    bool hasDoseToPublish = false;

    static uint32_t seq = 0;
    uint32_t lastPeriodicMs = 0;   // tracks 30s periodic publish
    uint32_t lastRxStatsLogMs = 0;

    Serial.println("[EventAgg] Task started");

    for (;;) {
        bool gotInner = false;
        uint32_t nowMs = millis();
        carePlanTick();
        if ((nowMs - lastRxStatsLogMs) >= 5000) {
            lastRxStatsLogMs = nowMs;
            Serial.printf("[ESP-NOW RX] total=%lu queued=%lu dupDrop=%lu lenDrop=%lu magicDrop=%lu queueDrop=%lu\n",
                          (unsigned long)g_espNowRxTotal,
                          (unsigned long)g_espNowRxQueued,
                          (unsigned long)g_espNowRxDuplicateDrop,
                          (unsigned long)g_espNowRxLenDrop,
                          (unsigned long)g_espNowRxMagicDrop,
                          (unsigned long)g_espNowRxQueueDrop);
        }
        // ---- Drain InnerPacket queue — keep only the newest packet ------- //
        {
            InnerPacket pkt;
            if (xQueueReceive(innerPacketQueue, &pkt, 0) == pdTRUE && pkt.magic == INNER_MAGIC) {
                lastInner = pkt;
                gotInner = true;
                hasInnerSnapshot = true;
            }
            if (gotInner) {
                Serial.printf("[EventAgg] InnerPacket seq=%lu  temp=%.2f°C  weight=%.1fg  pct=%.1f%%  door=%s\n",
                              (unsigned long)lastInner.seq,
                              lastInner.temperatureC,
                              lastInner.weightG,
                              lastInner.estimatedPercent,
                              lastInner.doorOpen ? "OPEN" : "CLOSED");
                Serial.printf("[EventAgg] Inner battery %.2fV (%u%%)\n",
                              lastInner.batteryVoltageV,
                              lastInner.batteryPercent);
            }
        }

        bool confirmedDose = false;

        // ---- Check for new dose events, suppressing duplicate pen records - //
        if (!pendingDose.active && hasDeferredDose) {
            startDosePrompt(pendingDose, deferredDose);
            hasDeferredDose = false;
        }

        DoseReading doseReading;
        while (xQueueReceive(doseQueue, &doseReading, 0) == pdTRUE) {
            if (pendingDose.active && sameDoseReading(doseReading, pendingDose.original)) {
                Serial.println("[EventAgg] Duplicate pending pen dose ignored");
                continue;
            }

            if (hasLastResolvedDose && sameDoseReading(doseReading, lastResolvedDose)) {
                Serial.println("[EventAgg] Duplicate resolved pen dose ignored");
                continue;
            }

            if (!pendingDose.active) {
                startDosePrompt(pendingDose, doseReading);
            } else if (!hasDeferredDose) {
                deferredDose = doseReading;
                hasDeferredDose = true;
                Serial.println("[EventAgg] Deferred new pen dose until current prompt resolves");
            } else {
                Serial.println("[EventAgg] Additional pen dose ignored while one dose is deferred");
            }
        }

        // ---- Keypad controls for pending dose ---------------------------- //
        KeypadEvent keyEvent;
        while (xQueueReceive(keypadQueue, &keyEvent, 0) == pdTRUE) {
            char key = keyEvent.key;
            if (!pendingDose.active) {
                DisplayState displayState = getDisplayStateSnapshot();
                if (displayState.activePage == DISPLAY_PAGE_PRESCRIPTION && key == '*') {
                    carePlanSelectPreviousSchedule();
                    Serial.println("[EventAgg] Prescription: previous schedule");
                } else if (displayState.activePage == DISPLAY_PAGE_PRESCRIPTION && key == '#') {
                    carePlanSelectNextSchedule();
                    Serial.println("[EventAgg] Prescription: next schedule");
                } else if (displayState.activePage == DISPLAY_PAGE_PRESCRIPTION &&
                           key == 'C' &&
                           carePlanStopReminder()) {
                    Serial.println("[EventAgg] Prescription: reminder stopped");
                } else if (key == '1' || key == 'B') {
                    carePlanFocusCurrentSchedule();
                    updateDisplayPage(DISPLAY_PAGE_PRESCRIPTION);
                    Serial.println("[EventAgg] Display page: prescription");
                } else if (key == 'A') {
                    updateDisplayPage(DISPLAY_PAGE_DASHBOARD);
                    Serial.println("[EventAgg] Display page: home");
                } else if (key == 'C') {
                    updateDisplayPage(DISPLAY_PAGE_ALERTS);
                    Serial.println("[EventAgg] Display page: alerts");
                } else if (key == 'D') {
                    updateDisplayPage(DISPLAY_PAGE_DEVICE_STATUS);
                    Serial.println("[EventAgg] Display page: system");
                } else if (key == '0') {
                    updateDisplayPage(DISPLAY_PAGE_QUEUE_STATUS);
                    Serial.println("[EventAgg] Display page: queue diagnostics");
                }
                continue;
            }

            if (!pendingDose.editing) {
                if (key == 'A') {
                    lastDose = pendingDose.original;
                    lastDose.doseUnits = (float)pendingDose.roundedUnits;
                    doseToPublish = lastDose;
                    hasDoseToPublish = true;
                    lastResolvedDose = pendingDose.original;
                    hasLastResolvedDose = true;
                    confirmedDose = true;
                    carePlanMarkDoseTaken(lastDose.doseUnits);
                    Serial.printf("[EventAgg] Dose confirmed by patient: %d units\n",
                                  pendingDose.roundedUnits);
                    pendingDose = {};
                    clearDosePrompt();
                } else if (key == 'B') {
                    pendingDose.editing = true;
                    pendingDose.editLen = 0;
                    pendingDose.editBuffer[0] = '\0';
                    refreshDosePrompt(pendingDose, millis());
                    Serial.println("[EventAgg] Dose edit mode started");
                } else if (key == 'C') {
                    lastResolvedDose = pendingDose.original;
                    hasLastResolvedDose = true;
                    Serial.printf(
                        "[EventAgg] Accidental pen dose cancelled: %.1f units\n",
                        pendingDose.original.doseUnits);
                    pendingDose = {};
                    clearDosePrompt();
                }
            } else {
                if (key >= '0' && key <= '9') {
                    if (pendingDose.editLen < DOSE_EDIT_MAX_DIGITS) {
                        pendingDose.editBuffer[pendingDose.editLen++] = key;
                        pendingDose.editBuffer[pendingDose.editLen] = '\0';
                        refreshDosePrompt(pendingDose, millis());
                    }
                } else if (key == '*' && pendingDose.editLen > 0) {
                    pendingDose.editLen--;
                    pendingDose.editBuffer[pendingDose.editLen] = '\0';
                    refreshDosePrompt(pendingDose, millis());
                } else if (key == '#') {
                    pendingDose.editLen = 0;
                    pendingDose.editBuffer[0] = '\0';
                    refreshDosePrompt(pendingDose, millis());
                } else if (key == 'C') {
                    pendingDose.editing = false;
                    pendingDose.editLen = 0;
                    pendingDose.editBuffer[0] = '\0';
                    refreshDosePrompt(pendingDose, millis());
                } else if (key == 'D' && pendingDose.editLen > 0) {
                    int editedUnits = atoi(pendingDose.editBuffer);
                    if (editedUnits < 1) editedUnits = 1;
                    if (editedUnits > DOSE_CONFIRM_MAX_UNITS) editedUnits = DOSE_CONFIRM_MAX_UNITS;
                    lastDose = pendingDose.original;
                    lastDose.doseUnits = (float)editedUnits;
                    doseToPublish = lastDose;
                    hasDoseToPublish = true;
                    lastResolvedDose = pendingDose.original;
                    hasLastResolvedDose = true;
                    confirmedDose = true;
                    carePlanMarkDoseTaken(lastDose.doseUnits);
                    Serial.printf("[EventAgg] Dose edited by patient: raw=%.1f sent=%d units\n",
                                  pendingDose.original.doseUnits,
                                  editedUnits);
                    pendingDose = {};
                    clearDosePrompt();
                }
            }
        }

        if (pendingDose.active) {
            uint32_t promptNowMs = millis();
            if ((promptNowMs - pendingDose.startedAtMs) >= DOSE_CONFIRM_TIMEOUT_MS) {
                lastDose = pendingDose.original;
                lastDose.doseUnits = (float)pendingDose.roundedUnits;
                doseToPublish = lastDose;
                hasDoseToPublish = true;
                lastResolvedDose = pendingDose.original;
                hasLastResolvedDose = true;
                confirmedDose = true;
                carePlanMarkDoseTaken(lastDose.doseUnits);
                Serial.printf("[EventAgg] Dose auto-confirmed after timeout: %d units\n",
                              pendingDose.roundedUnits);
                pendingDose = {};
                clearDosePrompt();
            } else {
                refreshDosePrompt(pendingDose, promptNowMs);
            }
        }

        // ---- Check for new glucose reading ------------------------------- //
        GlucoseReading glucReading;
        bool hasGlucose = (xQueueReceive(glucoseQueue, &glucReading, 0) == pdTRUE);
        if (hasGlucose) {
            lastGlucose = glucReading;
            Serial.printf("[EventAgg] New glucose: %d mg/dL\n", lastGlucose.valueMgDl);
        }

        if (gotInner || hasGlucose || confirmedDose) {
            updateDisplayActivity(gotInner, hasGlucose, confirmedDose);
        }

        // ---- Periodic publish every 30s (for monitoring/debug) ---------- //
        bool periodicTick = (millis() - lastPeriodicMs) >= 30000;
        if (periodicTick) lastPeriodicMs = millis();
        bool innerTriggered = gotInner;

        // ---- Only build and enqueue an event when something new arrived -- //
        bool dosePublishPending = confirmedDose || hasDoseToPublish;
        if (dosePublishPending || hasGlucose || innerTriggered || periodicTick) {
            TelemetryEvent event = {};

            // Root
            generateEventId(event.eventId, sizeof(event.eventId));
            event.sequenceNumber = seq++;
            event.trigger        = dosePublishPending ? DOSE_EVENT :
                                   (hasGlucose ? GLUCOSE_EVENT :
                                   (lastInner.batteryPercent <= INNER_BATTERY_LOW_PERCENT ? BATTERY_LOW :
                                   (lastInner.estimatedPercent < 20.0f ? INVENTORY_LOW :
                                   (lastInner.temperatureC < TEMP_MIN_C || lastInner.temperatureC > TEMP_MAX_C ? TEMPERATURE_ALERT : DEVICE_HEALTH))));
            event.replayedEvent  = false;
            getTimestamp(event.timestamp, sizeof(event.timestamp));

            // Storage (from last inner-unit packet)
            event.temperatureC    = lastInner.temperatureC;
            event.doorOpen        = (lastInner.doorOpen == 1);

            // Inventory
            event.inventoryWeightG = lastInner.weightG;
            event.estimatedPercent = lastInner.estimatedPercent;

            // Glucose
            event.hasGlucose             = hasGlucose && lastGlucose.valueMgDl > 0;
            event.glucoseMgDl             = lastGlucose.valueMgDl;
            event.glucometerSequenceNumber = lastGlucose.sequenceNumber;

            // Dose
            const DoseReading& eventDose = hasDoseToPublish ? doseToPublish : lastDose;
            event.hasDose = dosePublishPending && eventDose.doseUnits > 0.0f;
            event.doseUnits = eventDose.doseUnits;
            strncpy(event.injectedAt, eventDose.injectedAt, sizeof(event.injectedAt));

            // Battery / system — real WiFi RSSI, heap, BLE RSSI from shared var
            event.innerBatteryPercent = lastInner.batteryPercent;
            event.penBatteryPercent   = 76;   // TODO: read from pen unit BLE battery service
            event.outerBatteryPercent = 94;   // TODO: ADC voltage divider
            event.wifiRssiDbm         = WiFi.RSSI();
            event.bleRssiDbm          = g_lastBleRssi;
            event.freeHeapBytes       = esp_get_free_heap_size();

            updateDisplayStateFromTelemetry(event);

            if (hasInnerSnapshot) {
                lastPublishedInner = lastInner;
                hasPublishedInner = true;
            }

            Serial.printf("[EventAgg] Enqueue telemetry door=%s temp=%.2fC weight=%.1fg innerBat=%u%% trigger=%d dose=%.1f hasDose=%d\n",
                          event.doorOpen ? "OPEN" : "CLOSED",
                          event.temperatureC,
                          event.inventoryWeightG,
                          event.innerBatteryPercent,
                          (int)event.trigger,
                          event.doseUnits,
                          event.hasDose ? 1 : 0);

            BaseType_t telemetrySendResult = xQueueSend(telemetryQueue, &event, pdMS_TO_TICKS(500));
            if (telemetrySendResult != pdTRUE) {
                Serial.println("[EventAgg] telemetryQueue full — event dropped");
            } else if (hasDoseToPublish) {
                hasDoseToPublish = false;
            }
        }

        // Poll every 500ms — low enough latency, high enough to avoid busy loop
        vTaskDelay(pdMS_TO_TICKS(500));
    }
}
