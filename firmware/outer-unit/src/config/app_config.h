#ifndef APP_CONFIG_H
#define APP_CONFIG_H

// ---- Wi-Fi ---------------------------------------------------------------- //
#define WIFI_SSID                    "SLT-4G-74699C"
#define WIFI_PASSWORD                "Arnikan1811"

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
#define AWS_IOT_PUBLISH_TOPIC        "diasmart/patient/1/telemetry"

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

// How often the outer unit disconnects from pen and syncs the glucometer
#define GLUCOMETER_SYNC_INTERVAL_MS  30000

// ---- Storage / Inventory thresholds -------------------------------------- //
#define TEMP_MIN_C                   2.0f
#define TEMP_MAX_C                   8.0f
#define FULL_BOTTLE_WEIGHT_G         300.0f

// ---- FreeRTOS Queue lengths ---------------------------------------------- //
#define QUEUE_TELEMETRY_LEN          10
#define QUEUE_INNER_PACKET_LEN       5
#define QUEUE_GLUCOSE_LEN            5
#define QUEUE_DOSE_LEN               10

// ---- FreeRTOS Stack sizes (bytes) ---------------------------------------- //
#define STACK_EVENT_AGG              8192
#define STACK_MQTT_PUBLISH           8192
#define STACK_BLE_MANAGER            16384   // BLE client stack is large

#endif