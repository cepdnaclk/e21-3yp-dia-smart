#ifndef APP_CONFIG_H
#define APP_CONFIG_H

// ---- Wi-Fi ---------------------------------------------------------------- //
#define WIFI_SSID                    "ananthu73"
#define WIFI_PASSWORD                "123123123@@"
#define WIFI_CONNECT_TIMEOUT_MS      10000

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
#define AWS_IOT_CARE_PLAN_TOPIC      "diasmart/devices/" DEVICE_UID_OUTER "/care-plan"
#define AWS_IOT_COMMAND_TOPIC        "diasmart/devices/" DEVICE_UID_OUTER "/commands"
#define AWS_IOT_COMMAND_ACK_TOPIC    "diasmart/devices/" DEVICE_UID_OUTER "/command-ack"
#define AWS_IOT_DEVICE_TELEMETRY_TOPIC "diasmart/devices/" DEVICE_UID_OUTER "/telemetry"
#define MQTT_BUFFER_BYTES            8192
#define WIFI_COMMAND_MAX_BYTES       1024
#define WIFI_COMMAND_QUEUE_LENGTH    3
#define WIFI_STATUS_QUEUE_LENGTH     6

// ---- Serial --------------------------------------------------------------- //
#define SERIAL_BAUD                  115200

// ---- ESP-NOW -------------------------------------------------------------- //
// Outer unit receives on same channel as its WiFi AP.
// Inner unit must lock to the same channel (ESPNOW_CHANNEL=1 on both sides).
#define ESPNOW_CHANNEL               1
#define WIFI_CONFIG_FRAME_QUEUE_LENGTH 4
#define WIFI_PROVISIONING_TASK_STACK   8192
#define WIFI_PAIRING_TIMEOUT_MS        3000
#define WIFI_STAGE_ACK_TIMEOUT_MS      3000
#define WIFI_INNER_RESULT_TIMEOUT_MS   15000
#define WIFI_CONFIG_SEND_ATTEMPTS      3
#define WIFI_PROVISION_RETRY_DELAY_MS  5000
#define LOCAL_PROVISION_MAX_BODY_BYTES 256
#define LOCAL_PROVISIONING_TASK_STACK  6144
#define LOCAL_PROVISION_SUCCESS_GRACE_MS 30000

// ---- Pen Unit BLE (central scans for this peripheral) -------------------- //
#define PEN_BLE_DEVICE_NAME          "Dose_ESP32_C3"
#define PEN_BLE_SERVICE_UUID         "12345678-1234-1234-1234-1234567890ab"
#define PEN_BLE_CHAR_UUID            "abcd1234-5678-1234-5678-abcdef123456"

// ---- Glucometer BLE (Glucose Profile — 0x1808) --------------------------- //
#define GLUCOMETER_BLE_PIN           836337
#define GLUCOMETER_SERVICE_UUID      ((uint16_t)0x1808)
#define GLUCOMETER_MEAS_UUID         ((uint16_t)0x2A18)   // NOTIFY
#define GLUCOMETER_RACP_UUID         ((uint16_t)0x2A52)   // INDICATE
// Timezone attached to the meter's user-facing date/time.
#define GLUCOMETER_UTC_OFFSET_MINUTES 330

// BLE scan/session scheduling. Keep pen checks short/frequent and glucometer
// checks longer/less frequent so one peripheral does not starve the other.
#define PEN_SCAN_WINDOW_SEC          2
#define PEN_SCAN_IDLE_DELAY_MS       3000
#define PEN_SESSION_HOLD_MS          2500
#define GLUCOMETER_SCAN_WINDOW_SEC   10
#define GLUCOMETER_INITIAL_SCAN_DELAY_MS 5000

// Use one RACP request per glucometer connection. After the request completes
// (or times out), disconnect and start a fresh session later. The Guide Me is
// more reliable when each stored-record fetch has a clean BLE session.
#define GLUCOMETER_SESSION_RETRY_DELAY_MS 5000
#define GLUCOMETER_RACP_TIMEOUT_MS        12000

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

// ---- Care Plan / prescription display ----------------------------------- //
// Reject oversized plans instead of silently hiding prescribed schedules.
#define CARE_PLAN_MAX_SCHEDULES      8

// ---- Offline telemetry queue -------------------------------------------- //
// Stores exact compact backend JSON payloads in LittleFS and retries later.
#define OFFLINE_JSON_QUEUE_MAX_RECORDS 50
#define OFFLINE_JSON_MAX_BYTES         2048
#define OFFLINE_QUEUE_RETRY_INTERVAL_MS 5000

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
