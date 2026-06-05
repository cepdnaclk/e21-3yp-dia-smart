#pragma once

#include <stdint.h>

// Shared ESP-NOW packet from inner unit to outer unit.
// Both units must consume this exact packed layout.

#pragma pack(push, 1)
struct InnerPacket {
    uint32_t magic;            // 0x494E4E52 ('I','N','N','R')
    uint32_t seq;              // rolling sequence counter
    uint8_t  doorOpen;         // 1 = OPEN, 0 = CLOSED
    float    temperatureC;     // NAN if sensor read failed
    float    weightG;          // last valid HX711 reading in grams
    float    estimatedPercent; // clamped 0-100 remaining estimate
};
#pragma pack(pop)

static const uint32_t INNER_MAGIC = 0x494E4E52U;
