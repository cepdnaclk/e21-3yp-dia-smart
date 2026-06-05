#include <Arduino.h>
#include <Wire.h>
#include <esp_system.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <stdio.h>
#include "../config/app_config.h"
#include "../models/dose_event.h"
#include "../models/persistent_dose_record.h"
#include "../services/storage_service.h"

// Declared in main.cpp, shared with bleTransferTask
extern QueueHandle_t doseEventQueue;
extern PenDoseStorageService doseStorage;
extern bool doseStorageReady;

// ---- AS5600 I2C helpers -------------------------------------------------- //

// AS5600 I2C address (fixed by hardware — cannot be changed)
static constexpr uint8_t AS5600_ADDR      = 0x36;
// Register containing the 12-bit raw angle (high byte at 0x0C, low byte 0x0D)
static constexpr uint8_t AS5600_RAW_ANGLE = 0x0C;

// Read current raw angle from AS5600 (0–4095 = 0°–360°)
// Returns -1 on I2C error.
static int16_t readRawAngle() {
    Wire.beginTransmission(AS5600_ADDR);
    Wire.write(AS5600_RAW_ANGLE);
    if (Wire.endTransmission(false) != 0) {
        return -1;
    }
    if (Wire.requestFrom((uint8_t)AS5600_ADDR, (uint8_t)2) != 2) {
        return -1;
    }
    uint16_t high = Wire.read();
    uint16_t low  = Wire.read();
    return (int16_t)((high << 8) | low) & 0x0FFF;
}

// Convert a 12-bit raw AS5600 value to degrees (0.0–359.9°)
static float rawToDegrees(int16_t raw) {
    return (raw / 4096.0f) * 360.0f;
}

// Compute shortest signed angular delta between two degree values.
// Result is in the range (-180, +180].
static float angleDelta(float from, float to) {
    float delta = to - from;
    while (delta >  180.0f) delta -= 360.0f;
    while (delta <= -180.0f) delta += 360.0f;
    return delta;
}

// ---- Task ----------------------------------------------------------------- //

static PersistentDoseRecord buildPersistentRecord(const DoseEvent& event,
                                                  uint32_t sequence,
                                                  uint32_t bootNonce) {
    PersistentDoseRecord record = {};
    record.identity.sourceSequence = sequence;
    snprintf(record.identity.eventUid,
             sizeof(record.identity.eventUid),
             "PEN-%08lX-%lu-%lu",
             (unsigned long)bootNonce,
             (unsigned long)sequence,
             (unsigned long)event.timestampMs);
    record.timing.sourceTimestampMs = event.timestampMs;
    record.timing.receivedAtMs = 0;
    record.doseUnits = event.doseUnits;
    record.angleDegrees = event.angleDegrees;
    record.confidencePercent = event.confidencePercent;
    record.status = DOSE_RECORD_PENDING;
    record.retryCount = 0;
    record.reserved = 0;
    return record;
}

void doseDetectionTask(void* pvParams) {
    // Initialise I2C bus
    Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);

    // Initialise button pin
    pinMode(BUTTON_PIN, INPUT_PULLUP);

    // Wait briefly for AS5600 to power up
    vTaskDelay(pdMS_TO_TICKS(200));

    // Read initial reference angle
    int16_t rawRef = readRawAngle();
    float   refDeg = (rawRef >= 0) ? rawToDegrees(rawRef) : 0.0f;

    // Button state tracking for debounce
    bool    lastButtonState = HIGH;
    uint32_t buttonPressedAt = 0;
    bool    pressDebounced = false;
    uint32_t lastLiveLogAtMs = 0;
    uint32_t doseSequence = 0;
    const uint32_t bootNonce = esp_random();

    Serial.println("[DoseDetect] Task started");

    for (;;) {
        // ---- Read current angle ------------------------------------------ //
        int16_t rawNow = readRawAngle();
        float   curDeg = (rawNow >= 0) ? rawToDegrees(rawNow) : refDeg;
        float   delta = angleDelta(refDeg, curDeg);
        float   absDelta = fabsf(delta);
        float   doseUnits = absDelta / DEGREES_PER_UNIT;

        // ---- Button debounce --------------------------------------------- //
        bool currentButton = (bool)digitalRead(BUTTON_PIN);

        if (lastButtonState == HIGH && currentButton == LOW) {
            // Falling edge — button just pressed
            buttonPressedAt = millis();
            pressDebounced = false;
        }

        // While held LOW, mark as a valid debounced press once stable.
        if (currentButton == LOW
            && !pressDebounced
            && ((millis() - buttonPressedAt) >= BUTTON_DEBOUNCE_MS)) {
            pressDebounced = true;
        }

        if (lastButtonState == LOW && currentButton == HIGH && pressDebounced) {
            // Rising edge after debounce — button released: process dose

            // Validate dose is within sane range
            if (doseUnits >= DOSE_MIN_UNITS && doseUnits <= DOSE_MAX_UNITS) {
                // Confidence: 100% if button pressed and angle moved cleanly.
                // Reduce to 70% if only button without meaningful rotation
                // (user may have accidentally pressed without turning).
                float confidence = (absDelta >= (DEGREES_PER_UNIT * DOSE_MIN_UNITS))
                                   ? 100.0f
                                   : 70.0f;

                if (confidence >= DOSE_CONFIDENCE_THRESHOLD) {
                    DoseEvent event;
                    event.doseUnits         = doseUnits;
                    event.angleDegrees      = absDelta;
                    event.confidencePercent = confidence;
                    event.timestampMs       = millis();

                    PersistentDoseRecord record = buildPersistentRecord(event, ++doseSequence, bootNonce);

                    if (!doseStorageReady) {
                        Serial.println("[DoseDetect] ERROR: dose storage unavailable, dose not queued");
                    } else if (doseStorage.appendPending(record)) {
                        if (xQueueSend(doseEventQueue, &event, 0) == pdTRUE) {
                            Serial.printf("[DoseDetect] Dose saved and queued: %.1f units (%.1f deg, conf %.0f%%)\n",
                                          doseUnits, absDelta, confidence);
                        } else {
                            Serial.println("[DoseDetect] WARNING: doseEventQueue full, saved dose will remain pending");
                        }
                    } else {
                        Serial.println("[DoseDetect] ERROR: dose storage full, dose not queued");
                    }

                    // Reset reference angle after a confirmed dose
                    refDeg = curDeg;
                    Serial.println("[DoseDetect] Reference reset after confirmed dose");
                } else {
                    Serial.printf("[DoseDetect] Low confidence (%.0f%%), dose ignored\n", confidence);
                }
            } else if (doseUnits > 0.0f) {
                Serial.printf("[DoseDetect] Dose out of range (%.1f units), ignored\n", doseUnits);
            }

            // Release handled; wait for next press cycle.
            pressDebounced = false;
        }

        // Legacy-style live logging so serial monitor always shows activity.
        if ((millis() - lastLiveLogAtMs) >= 200) {
            Serial.printf("[DoseDetect] Live Dial: %.2f u | Button: %d | Raw: %d\n",
                          doseUnits, currentButton ? 1 : 0, rawNow);
            lastLiveLogAtMs = millis();
        }

        lastButtonState = currentButton;

        // Poll at 50 Hz — fast enough to catch button presses reliably
        vTaskDelay(pdMS_TO_TICKS(20));
    }
}
