#include <Arduino.h>
#include <WiFi.h>
#include <math.h>
#include <time.h>
#include <esp_system.h>
#include "models/telemetry_event.h"
#include "include/system_queues.h"
#include "config/app_config.h"
#include "managers/display_state_manager.h"

// Shared variable written by bleManagerTask, read here for battery section.
// Declared extern in ble_manager.cpp.
extern volatile int g_lastBleRssi;

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

    GlucoseReading lastGlucose = {};
    lastGlucose.valueMgDl      = 0;
    lastGlucose.sequenceNumber = 0;

    DoseReading lastDose = {};
    lastDose.doseUnits   = 0.0f;
    strncpy(lastDose.injectedAt, "1970-01-01T00:00:00Z", sizeof(lastDose.injectedAt));

    static uint32_t seq = 0;
    uint32_t lastPeriodicMs = 0;   // tracks 30s periodic publish

    Serial.println("[EventAgg] Task started");

    for (;;) {
        // ---- Drain InnerPacket queue — keep only the newest packet ------- //
        {
            InnerPacket pkt;
            bool gotInner = false;
            while (xQueueReceive(innerPacketQueue, &pkt, 0) == pdTRUE) {
                if (pkt.magic == INNER_MAGIC) {
                    lastInner = pkt;
                    gotInner = true;
                }
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

        // ---- Check for new dose event (primary trigger) ------------------ //
        DoseReading doseReading;
        bool hasDose = (xQueueReceive(doseQueue, &doseReading, 0) == pdTRUE);
        if (hasDose) {
            lastDose = doseReading;
            Serial.printf("[EventAgg] New dose: %.1f units\n", lastDose.doseUnits);
        }

        // ---- Check for new glucose reading ------------------------------- //
        GlucoseReading glucReading;
        bool hasGlucose = (xQueueReceive(glucoseQueue, &glucReading, 0) == pdTRUE);
        if (hasGlucose) {
            lastGlucose = glucReading;
            Serial.printf("[EventAgg] New glucose: %d mg/dL\n", lastGlucose.valueMgDl);
        }

        // ---- Periodic publish every 30s (for monitoring/debug) ---------- //
        bool periodicTick = (millis() - lastPeriodicMs) >= 30000;
        if (periodicTick) lastPeriodicMs = millis();

        // ---- Only build and enqueue an event when something new arrived -- //
        if (hasDose || hasGlucose || periodicTick) {
            TelemetryEvent event = {};

            // Root
            snprintf(event.eventId, sizeof(event.eventId),
                     "EVT-%s-%lu", DEVICE_UID_OUTER, (unsigned long)seq);
            event.sequenceNumber = seq++;
            event.trigger        = hasDose ? DOSE_EVENT : (hasGlucose ? GLUCOSE_EVENT : DOSE_EVENT);
            event.replayedEvent  = false;
            getTimestamp(event.timestamp, sizeof(event.timestamp));

            // Storage (from last inner-unit packet)
            event.temperatureC    = lastInner.temperatureC;
            event.doorOpen        = (lastInner.doorOpen == 1);

            // Inventory
            event.inventoryWeightG = lastInner.weightG;
            event.estimatedPercent = lastInner.estimatedPercent;

            // Glucose
            event.glucoseMgDl             = lastGlucose.valueMgDl;
            event.glucometerSequenceNumber = lastGlucose.sequenceNumber;

            // Dose
            event.doseUnits = lastDose.doseUnits;
            strncpy(event.injectedAt, lastDose.injectedAt, sizeof(event.injectedAt));

            // Battery / system — real WiFi RSSI, heap, BLE RSSI from shared var
            event.innerBatteryPercent = lastInner.batteryPercent;
            event.penBatteryPercent   = 76;   // TODO: read from pen unit BLE battery service
            event.outerBatteryPercent = 94;   // TODO: ADC voltage divider
            event.wifiRssiDbm         = WiFi.RSSI();
            event.bleRssiDbm          = g_lastBleRssi;
            event.freeHeapBytes       = esp_get_free_heap_size();

            updateDisplayStateFromTelemetry(event);

            if (xQueueSend(telemetryQueue, &event, pdMS_TO_TICKS(500)) != pdTRUE) {
                Serial.println("[EventAgg] telemetryQueue full — event dropped");
            }
        }

        // Poll every 500ms — low enough latency, high enough to avoid busy loop
        vTaskDelay(pdMS_TO_TICKS(500));
    }
}
