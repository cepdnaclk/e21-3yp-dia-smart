#ifndef APP_CONFIG_H
#define APP_CONFIG_H

// --- Wi-Fi Configuration ---
// Replace these with your actual home/lab Wi-Fi credentials
#define WIFI_SSID "SLT-4G-74699C"
#define WIFI_PASSWORD "Arnikan1811"

// --- Device Configuration ---
#define DEVICE_UID "DS-OUTER-0001"
#define PATIENT_ID "P001"
#define FIRMWARE_VERSION "v1.0.0"

// --- AWS IoT Configuration ---
// Get this from AWS IoT Console -> Settings -> "Device data endpoint"
#define AWS_IOT_ENDPOINT "a36biie00zvpfg-ats.iot.eu-north-1.amazonaws.com"
#define AWS_IOT_PORT 8883
#define AWS_IOT_PUBLISH_TOPIC "diasmart/patient/P001/telemetry"

#endif