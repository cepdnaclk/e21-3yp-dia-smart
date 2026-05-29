#pragma once

#include <stdint.h>

// =============================================================================
// InnerPacket — sent over ESP-NOW broadcast from inner unit to outer unit.
//
// CRITICAL: this struct must be byte-identical to the InnerPacket struct in
// outer-unit/src/include/system_queues.h. Both use #pragma pack(push,1) to
// prevent any compiler padding between fields.
// =============================================================================

#pragma pack(push, 1)
struct InnerPacket {
    // Magic number — outer unit uses this to reject garbage ESP-NOW frames.
    // Value: 0x494E4E52 = ASCII 'I','N','N','R'
    uint32_t magic;

    // Rolling sequence counter (wraps at 0xFFFFFFFF → 0).
    // Outer unit uses this to detect dropped packets.
    uint32_t seq;

    // Door status: 1 = OPEN, 0 = CLOSED
    // Derived from reed switch: digitalRead(DOOR_SENSOR_PIN) == HIGH
    uint8_t  doorOpen;

    // DS18B20 temperature reading in °C.
    // NAN if sensor returned DS18B20_ERROR_TEMP (85.0 = parasite power fault).
    float    temperatureC;

    // HX711 load cell reading in grams.
    // Holds last valid reading if scale.is_ready() == false (never sends -1).
    float    weightG;

    // Pre-calculated: (weightG / FULL_BOTTLE_WEIGHT_G) * 100, clamped 0–100.
    float    estimatedPercent;
};
#pragma pack(pop)

// Sentinel value — outer unit checks packet.magic == INNER_MAGIC before use
static const uint32_t INNER_MAGIC = 0x494E4E52U;
