#pragma once

// =============================================================================
// Inner Unit — App Configuration
// ALL hardware-specific values live here. Change only this file when hardware
// changes (different pin wiring, re-calibration, new WiFi, etc.)
// =============================================================================

// ---- Serial ----------------------------------------------------------------
#define SERIAL_BAUD              115200

// ---- WiFi (needed only to lock ESP-NOW channel — not used for data) --------
#define WIFI_SSID                "your-ssid"
#define WIFI_PASSWORD            "your-password"
// How long to wait for WiFi before falling back to manual channel set (ms)
#define WIFI_CONNECT_TIMEOUT_MS  10000

// ---- ESP-NOW ---------------------------------------------------------------
// Must match outer unit's WiFi channel
#define ESPNOW_CHANNEL           1

// ---- DS18B20 Temperature sensor (OneWire) ----------------------------------
#define TEMP_SENSOR_PIN          21
// DS18B20 returns exactly 85.0 on parasite-power wiring error — treat as NAN
#define DS18B20_ERROR_TEMP       85.0f
// Acceptable temperature range for fridge (°C). Outside = alert.
#define TEMP_MIN_C               2.0f
#define TEMP_MAX_C               8.0f

// ---- Reed switch (door open/closed) ----------------------------------------
// INPUT_PULLUP: HIGH = door OPEN (magnet away), LOW = CLOSED (magnet shorts pin)
#define DOOR_SENSOR_PIN          4

// ---- HX711 load cell (insulin bottle weight) --------------------------------
#define HX711_DOUT_PIN           5
#define HX711_CLK_PIN            18
// Calibration factor — adjust after calibrating with known weight
#define LOAD_CELL_CALIBRATION    245.0f
// Number of readings to average per sample (reduces noise)
#define HX711_AVERAGES           3
// Reference weight of a full insulin bottle (grams) — for % calculation
#define FULL_BOTTLE_WEIGHT_G     300.0f

// ---- Sensor sampling interval ----------------------------------------------
#define SAMPLE_INTERVAL_MS       3000

// ---- Device identity -------------------------------------------------------
#define DEVICE_UID_INNER         "DS-INNER-0001"
