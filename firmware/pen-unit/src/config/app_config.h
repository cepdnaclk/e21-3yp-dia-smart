#pragma once

// =============================================================================
// Pen Unit — App Configuration
// ALL hardware-specific values live here. Change only this file when hardware
// changes (different pin wiring, re-calibration, new BLE name, etc.)
// =============================================================================

// ---- Serial ----
#define SERIAL_BAUD              115200

// ---- BLE ----
#define BLE_DEVICE_NAME          "Dose_ESP32_C3"
#define BLE_SERVICE_UUID         "12345678-1234-1234-1234-1234567890ab"
#define BLE_CHARACTERISTIC_UUID  "abcd1234-5678-1234-5678-abcdef123456"

// ---- I2C (AS5600 magnetic encoder) ----
#define I2C_SDA_PIN              6
#define I2C_SCL_PIN              7

// ---- Button (dose confirm) ----
// INPUT_PULLUP: LOW = pressed, HIGH = released
#define BUTTON_PIN               3
#define BUTTON_DEBOUNCE_MS       50

// ---- Dose detection ----
// Degrees of rotation per one insulin unit on the pen dial
#define DEGREES_PER_UNIT         15.0f
// Minimum valid dose to accept (avoids accidental micro-rotations)
#define DOSE_MIN_UNITS           0.5f
// Maximum valid dose (safety cap)
#define DOSE_MAX_UNITS           100.0f
// Confidence threshold below which dose is discarded (0–100 %)
#define DOSE_CONFIDENCE_THRESHOLD 60.0f

// ---- Internal queue depth ----
#define DOSE_QUEUE_LENGTH        10

// ---- Task stack sizes (bytes) ----
#define STACK_DOSE_DETECT        4096
#define STACK_BLE_TRANSFER       8192

// ---- BLE notify pacing ----
#define BLE_NOTIFY_INTERVAL_MS   100

// Advertising interval units are 0.625 ms.
#define BLE_ADV_FAST_MIN_INTERVAL 0x0030
#define BLE_ADV_FAST_MAX_INTERVAL 0x0060
#define BLE_ADV_SLOW_MIN_INTERVAL 0x0640
#define BLE_ADV_SLOW_MAX_INTERVAL 0x0C80
#define BLE_ADV_STATE_CHECK_MS    1000
