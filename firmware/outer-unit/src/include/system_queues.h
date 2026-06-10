#pragma once

#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <math.h>
#include "../../../common/protocols/espnow_packets.h"
#include "../../../common/protocols/ble_packets.h"
#include "../models/keypad_event.h"

// ---- Queue handles (defined in main.cpp) ---------------------------------- //
extern QueueHandle_t telemetryQueue;     // TelemetryEvent  -> mqtt publish
extern QueueHandle_t innerPacketQueue;   // InnerPacket     -> event aggregator
extern QueueHandle_t glucoseQueue;       // GlucoseReading  -> event aggregator
extern QueueHandle_t doseQueue;          // DoseReading     -> event aggregator
extern QueueHandle_t keypadQueue;        // KeypadEvent     -> event aggregator
