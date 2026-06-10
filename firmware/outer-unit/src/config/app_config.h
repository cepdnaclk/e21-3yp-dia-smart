#ifndef APP_CONFIG_H
#define APP_CONFIG_H

// ---- Wi-Fi ---------------------------------------------------------------- //
#define WIFI_SSID                    "ananthu73"
#define WIFI_PASSWORD                "123123123@@"

// ---- Device UIDs (must match backend DB exactly) ------------------------- //
#define DEVICE_UID                   "DS-OUTER-0001"   // MQTT client ID (keep alias)
#define DEVICE_UID_OUTER             "DS-OUTER-0001"
#define DEVICE_UID_INNER             "DS-INNER-0001"
#define DEVICE_UID_PEN               "DS-CAP-0001"
#define DEVICE_UID_GLUCOMETER        "DS-GLU-0001"

// ---- Patient (Long — matches DB patients.patient_id, NOT a string) ------- //
#define PATIENT_ID                   1
#define FIRMWARE_VERSION             "v1.0.0"

// ---- AWS IoT Core --------------------------------------------------------- //
#define AWS_IOT_ENDPOINT             "a36biie00zvpfg-ats.iot.eu-north-1.amazonaws.com"
#define AWS_IOT_PORT                 8883
// Must match backend MQTT_TOPIC subscription.
#define AWS_IOT_PUBLISH_TOPIC        "diasmart/device/telemetry"

// ---- Serial --------------------------------------------------------------- //
#define SERIAL_BAUD                  115200

// ---- ESP-NOW -------------------------------------------------------------- //
// Outer unit receives on same channel as its WiFi AP.
// Inner unit must lock to the same channel (ESPNOW_CHANNEL=1 on both sides).
#define ESPNOW_CHANNEL               1

// ---- Pen Unit BLE (central scans for this peripheral) -------------------- //
#define PEN_BLE_DEVICE_NAME          "Dose_ESP32_C3"
#define PEN_BLE_SERVICE_UUID         "12345678-1234-1234-1234-1234567890ab"
#define PEN_BLE_CHAR_UUID            "abcd1234-5678-1234-5678-abcdef123456"

// ---- Glucometer BLE (Glucose Profile — 0x1808) --------------------------- //
#define GLUCOMETER_BLE_PIN           836337
#define GLUCOMETER_SERVICE_UUID      ((uint16_t)0x1808)
#define GLUCOMETER_MEAS_UUID         ((uint16_t)0x2A18)   // NOTIFY
#define GLUCOMETER_RACP_UUID         ((uint16_t)0x2A52)   // INDICATE

// BLE scan/session scheduling. Keep pen checks short/frequent and glucometer
// checks longer/less frequent so one peripheral does not starve the other.
#define PEN_SCAN_WINDOW_SEC          2
#define PEN_SCAN_IDLE_DELAY_MS       3000
#define PEN_SESSION_HOLD_MS          2500
#define GLUCOMETER_SCAN_WINDOW_SEC   10
#define GLUCOMETER_INITIAL_SCAN_DELAY_MS 5000

// How often the outer unit attempts to sync the glucometer
#define GLUCOMETER_SYNC_INTERVAL_MS  30000

// ---- Storage / Inventory thresholds -------------------------------------- //
#define TEMP_MIN_C                   2.0f
#define TEMP_MAX_C                   8.0f
#define FULL_BOTTLE_WEIGHT_G         300.0f
// Inner packet changes that should immediately produce a backend/display event.
#define INNER_TEMP_EVENT_DELTA_C     0.5f
#define INNER_WEIGHT_EVENT_DELTA_G   2.0f
#define INNER_INVENTORY_EVENT_DELTA_PERCENT 2.0f
#define INNER_BATTERY_LOW_PERCENT    20

// ---- TFT Display (8-bit parallel, PCB pin map) --------------------------- //
#define DISPLAY_ENABLED              1
#define DISPLAY_RAW_DIAGNOSTIC       0
#define DISPLAY_WIDTH                320
#define DISPLAY_HEIGHT               480
#define DISPLAY_REFRESH_MS           1000
#define DISPLAY_PIN_LCD_CS           9
#define DISPLAY_PIN_LCD_D0           12
#define DISPLAY_PIN_LCD_D1           13
#define DISPLAY_PIN_LCD_D2           14
#define DISPLAY_PIN_LCD_D3           15
#define DISPLAY_PIN_LCD_D4           16
#define DISPLAY_PIN_LCD_D5           17
#define DISPLAY_PIN_LCD_D6           18
#define DISPLAY_PIN_LCD_D7           21
#define DISPLAY_PIN_LCD_WR           7
#define DISPLAY_PIN_LCD_RS           8
#define DISPLAY_PIN_LCD_RST          6
// LCD_RD is pulled up to 3.3V through 10k on the PCB; firmware uses write-only mode.

// ---- 4x4 Keypad ---------------------------------------------------------- //
#define KEYPAD_ROW1_PIN              1
#define KEYPAD_ROW2_PIN              2
#define KEYPAD_ROW3_PIN              3
#define KEYPAD_ROW4_PIN              4
#define KEYPAD_COL1_PIN              35
#define KEYPAD_COL2_PIN              36
#define KEYPAD_COL3_PIN              37
#define KEYPAD_COL4_PIN              38
#define KEYPAD_SCAN_INTERVAL_MS      25
#define KEYPAD_DEBOUNCE_MS           80

// Dose confirmation: auto-send rounded pen dose if patient does not respond.
#define DOSE_CONFIRM_TIMEOUT_MS      40000
#define DOSE_EDIT_MAX_DIGITS         3
#define DOSE_CONFIRM_MAX_UNITS       100

// ---- FreeRTOS Queue lengths ---------------------------------------------- //
#define QUEUE_TELEMETRY_LEN          10
#define QUEUE_INNER_PACKET_LEN       5
#define QUEUE_GLUCOSE_LEN            5
#define QUEUE_DOSE_LEN               10
#define QUEUE_KEYPAD_LEN             8

// ---- FreeRTOS Stack sizes (bytes) ---------------------------------------- //
#define STACK_EVENT_AGG              8192
#define STACK_MQTT_PUBLISH           8192
#define STACK_BLE_MANAGER            16384   // BLE client stack is large
#define STACK_DISPLAY_UI             8192
#define STACK_KEYPAD                 3072

#endif
