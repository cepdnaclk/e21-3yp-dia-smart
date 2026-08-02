#pragma once

// =============================================================================
// Inner Unit - App Configuration
// Keep hardware pins, sensor calibration values, and event-trigger thresholds
// here. Change this file when wiring, calibration, or trigger sensitivity changes.
// =============================================================================

// ---- Serial ----------------------------------------------------------------
#define SERIAL_BAUD              115200

// ---- WiFi (needed only to lock ESP-NOW channel; not used for data) ----------
#define WIFI_SSID                "ananthu73"
#define WIFI_PASSWORD            "123123123@@"
// How long to wait for WiFi before falling back to manual channel set (ms).
#define WIFI_CONNECT_TIMEOUT_MS  10000

// ---- ESP-NOW ---------------------------------------------------------------
// Must match outer unit's WiFi channel.
#define ESPNOW_CHANNEL           1
#define WIFI_CONFIG_FRAME_QUEUE_LENGTH 4
#define WIFI_PROVISIONING_TASK_STACK   8192

// ---- DS18B20 temperature sensor (OneWire) ----------------------------------
#define TEMP_SENSOR_PIN          21
// DS18B20 returns exactly 85.0 on parasite-power wiring error; treat as NAN.
#define DS18B20_ERROR_TEMP       85.0f
// Acceptable fridge temperature range in Celsius. Outside this range should be
// treated as an alert by the event-trigger logic.
#define TEMP_MIN_C               2.0f
#define TEMP_MAX_C               8.0f

// ---- Reed switch (door open/closed) ----------------------------------------
// INPUT_PULLUP: HIGH = door OPEN (magnet away), LOW = CLOSED (magnet shorts pin).
#define DOOR_SENSOR_PIN          4

// ---- Battery monitor (ADC1 through 100k/100k divider) ----------------------
// Wiring: Battery+ -> 100k -> GPIO34 ADC node -> 100k -> GND, Battery- -> GND.
// Use ADC1, not ADC2, because WiFi/ESP-NOW can block ADC2 reads on ESP32.
#define BATTERY_ADC_PIN          34
#define BATTERY_ADC_SAMPLES      16
#define BATTERY_DIVIDER_TOP_OHMS 100000.0f
#define BATTERY_DIVIDER_BOTTOM_OHMS 100000.0f
// First-pass estimate for the current 3.7V test battery/source.
// If using a fully charged Li-ion cell later, this may need to return to 4200.
#define BATTERY_EMPTY_MV         3300
#define BATTERY_FULL_MV          3700

// ---- HX711 load cell (insulin bottle weight) -------------------------------
#define HX711_DOUT_PIN           5
#define HX711_CLK_PIN            18
// Calibration factor from HX711 calibration. Tune until scale.get_units()
// reports the known calibration weight in grams.
#define LOAD_CELL_CALIBRATION    -1589.0f
// Number of readings to average per sample. Increase to reduce noise; this also
// makes each sample slower.
#define HX711_AVERAGES           20
// Two full cartridges at ~9.2g each. This drives inventory %.
#define FULL_BOTTLE_WEIGHT_G     18.4f
#define EMPTY_WEIGHT_DEADBAND_G  2.0f

// ---- Sensor sampling / event trigger tuning --------------------------------
// Raw sensors are sampled every SAMPLE_INTERVAL_MS. Event-triggered sending
// should compare the new sample with the last sent sample and send only when a
// threshold below is crossed, plus a periodic heartbeat.
#define SAMPLE_INTERVAL_MS       3000
// Door is cheap to read; poll it quickly so open/close sends over ESP-NOW fast.
#define DOOR_POLL_INTERVAL_MS    50
// Send when temperature changes by at least this many Celsius.
#define TEMP_EVENT_DELTA_C       0.5f
// Send when weight changes by at least this many grams.
#define WEIGHT_EVENT_DELTA_G     2.0f
// Send when calculated inventory percent changes by at least this amount.
#define INVENTORY_EVENT_DELTA_PERCENT 2.0f
// Send when door state changes and remains stable for this debounce duration.
#define DOOR_EVENT_DEBOUNCE_MS   250
// Send even when nothing changed, so outer knows the inner unit is alive.
#define INNER_HEARTBEAT_MS       60000
// Broadcast ESP-NOW has no end-to-end ACK from the outer application. Send a
// short burst so packets survive BLE/WiFi coexistence gaps on the outer unit.
#define ESPNOW_SAMPLE_BURST_COUNT 3
#define ESPNOW_DOOR_BURST_COUNT   6
#define ESPNOW_BURST_GAP_MS       40

// ---- Device identity -------------------------------------------------------
#define DEVICE_UID_INNER         "DS-INNER-0001"
