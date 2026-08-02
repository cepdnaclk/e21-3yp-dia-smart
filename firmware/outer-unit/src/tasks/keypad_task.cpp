#include <Arduino.h>

#include "config/app_config.h"
#include "include/system_queues.h"

namespace {
const uint8_t ROW_PINS[4] = {
    KEYPAD_ROW1_PIN,
    KEYPAD_ROW2_PIN,
    KEYPAD_ROW3_PIN,
    KEYPAD_ROW4_PIN
};

const uint8_t COL_PINS[4] = {
    KEYPAD_COL1_PIN,
    KEYPAD_COL2_PIN,
    KEYPAD_COL3_PIN,
    KEYPAD_COL4_PIN
};

const char KEY_MAP[4][4] = {
    {'1', '2', '3', 'A'},
    {'4', '5', '6', 'B'},
    {'7', '8', '9', 'C'},
    {'*', '0', '#', 'D'}
};

char scanKeypadOnce() {
    for (uint8_t row = 0; row < 4; ++row) {
        digitalWrite(ROW_PINS[row], LOW);
        delayMicroseconds(50);

        for (uint8_t col = 0; col < 4; ++col) {
            if (digitalRead(COL_PINS[col]) == LOW) {
                digitalWrite(ROW_PINS[row], HIGH);
                return KEY_MAP[row][col];
            }
        }

        digitalWrite(ROW_PINS[row], HIGH);
    }
    return '\0';
}
}

void keypadTask(void* parameter) {
    (void)parameter;

    for (uint8_t row = 0; row < 4; ++row) {
        pinMode(ROW_PINS[row], OUTPUT);
        digitalWrite(ROW_PINS[row], HIGH);
    }
    for (uint8_t col = 0; col < 4; ++col) {
        pinMode(COL_PINS[col], INPUT_PULLUP);
    }

    char lastStableKey = '\0';
    char candidateKey = '\0';
    uint32_t candidateSinceMs = 0;
    bool emittedForCurrentPress = false;

    Serial.println("[Keypad] Task started");

    for (;;) {
        char key = scanKeypadOnce();
        uint32_t now = millis();

        if (key != candidateKey) {
            candidateKey = key;
            candidateSinceMs = now;
        }

        if (key == '\0') {
            lastStableKey = '\0';
            emittedForCurrentPress = false;
        } else if (!emittedForCurrentPress &&
                   key != lastStableKey &&
                   (now - candidateSinceMs) >= KEYPAD_DEBOUNCE_MS) {
            lastStableKey = key;
            emittedForCurrentPress = true;

            KeypadEvent event = {};
            event.key = key;
            event.timestampMs = now;

            if (keypadQueue != nullptr &&
                xQueueSend(keypadQueue, &event, 0) == pdTRUE) {
                Serial.printf("[Keypad] Key pressed: %c\n", key);
            } else {
                Serial.println("[Keypad] keypadQueue full - key dropped");
            }
        }

        vTaskDelay(pdMS_TO_TICKS(KEYPAD_SCAN_INTERVAL_MS));
    }
}
