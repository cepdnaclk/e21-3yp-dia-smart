#pragma once

#include <stdint.h>

// Development-only defaults keep prototype kits buildable. Production builds
// must inject unique per-kit key bytes and define DIASMART_ESPNOW_KEYS_PROVISIONED.
#ifndef DIASMART_ESPNOW_PMK_BYTES
#define DIASMART_ESPNOW_PMK_BYTES \
    0x44, 0x69, 0x61, 0x53, 0x6D, 0x61, 0x72, 0x74, \
    0x50, 0x4D, 0x4B, 0x2D, 0x44, 0x45, 0x56, 0x31
#define DIASMART_USING_DEVELOPMENT_ESPNOW_KEYS 1
#endif

#ifndef DIASMART_ESPNOW_LMK_BYTES
#define DIASMART_ESPNOW_LMK_BYTES \
    0x44, 0x69, 0x61, 0x53, 0x6D, 0x61, 0x72, 0x74, \
    0x4C, 0x4D, 0x4B, 0x2D, 0x44, 0x45, 0x56, 0x31
#define DIASMART_USING_DEVELOPMENT_ESPNOW_KEYS 1
#endif

#ifndef DIASMART_SETUP_AP_PASSWORD
#define DIASMART_SETUP_AP_PASSWORD "DiaSmartSetup0001"
#define DIASMART_USING_DEVELOPMENT_SETUP_PASSWORD 1
#endif

#if defined(DIASMART_PRODUCTION_BUILD) && \
    !defined(DIASMART_ESPNOW_KEYS_PROVISIONED)
#error "Production firmware requires unique provisioned ESP-NOW PMK/LMK keys"
#endif

#if defined(DIASMART_PRODUCTION_BUILD) && \
    !defined(DIASMART_SETUP_PASSWORD_PROVISIONED)
#error "Production firmware requires a unique provisioned setup AP password"
#endif

static const uint8_t DIASMART_ESPNOW_PMK[16] = {
    DIASMART_ESPNOW_PMK_BYTES
};

static const uint8_t DIASMART_ESPNOW_LMK[16] = {
    DIASMART_ESPNOW_LMK_BYTES
};
