#pragma once

#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <math.h>

// ---- InnerPacket ---------------------------------------------------------- //
// ESP-NOW payload from the inner unit. This struct MUST be byte-for-byte
// identical to the version in firmware/inner-unit/src/models/inner_event.h.
// Any difference will cause silently corrupted data on receive.

#pragma pack(push, 1)
struct InnerPacket {
    uint32_t magic;            // 0x494E4E52 ('I','N','N','R') — validation marker
    uint32_t seq;              // rolling sequence counter
    uint8_t  doorOpen;         // 1 = door OPEN, 0 = door CLOSED
    float    temperatureC;     // DS18B20 reading (NAN if sensor error)
    float    weightG;          // last valid HX711 reading in grams
    float    estimatedPercent; // (weightG / FULL_BOTTLE_WEIGHT_G) * 100, clamped 0–100
};
#pragma pack(pop)

static const uint32_t INNER_MAGIC = 0x494E4E52U;

// ---- GlucoseReading ------------------------------------------------------- //
// One glucose measurement received from the BLE glucometer (0x2A18 notify).

struct GlucoseReading {
    int      valueMgDl;        // glucose concentration in mg/dL
    int      sequenceNumber;   // from glucometer's RACP sequence field
    uint32_t timestampMs;      // millis() at reception
};

// ---- DoseReading ---------------------------------------------------------- //
// One dose event received from the pen unit over BLE notify.

struct DoseReading {
    float    doseUnits;        // insulin units injected (from AS5600 angle)
    float    angleDegrees;     // raw angle measurement
    char     injectedAt[32];   // ISO-8601 timestamp string
    uint32_t timestampMs;      // millis() at reception
};

// ---- Queue handles (defined in main.cpp) ---------------------------------- //
extern QueueHandle_t telemetryQueue;     // TelemetryEvent  — aggregator → mqtt publish
extern QueueHandle_t innerPacketQueue;   // InnerPacket     — ESP-NOW ISR → aggregator
extern QueueHandle_t glucoseQueue;       // GlucoseReading  — BLE manager → aggregator
extern QueueHandle_t doseQueue;          // DoseReading     — BLE manager → aggregator
