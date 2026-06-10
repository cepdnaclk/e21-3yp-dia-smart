#pragma once

#include <stdint.h>

// Shared ACK protocol definitions for inter-device event delivery.
// This file defines protocol constants only. Runtime wiring is intentionally
// deferred to later phases so current device behavior remains unchanged.

static const uint8_t ACK_PROTOCOL_VERSION = 1;

enum AckMessageType : uint8_t {
    ACK_MESSAGE_EVENT_RESULT = 1
};

enum AckPeerType : uint8_t {
    ACK_PEER_UNKNOWN = 0,
    ACK_PEER_INNER_UNIT = 1,
    ACK_PEER_OUTER_UNIT = 2,
    ACK_PEER_PEN_UNIT = 3,
    ACK_PEER_GLUCOMETER = 4
};

enum AckResultCode : uint8_t {
    ACK_RESULT_ACCEPTED = 1,
    ACK_RESULT_DUPLICATE = 2,
    ACK_RESULT_INVALID = 3,
    ACK_RESULT_RETRY = 4,
    ACK_RESULT_NOT_FOUND = 5
};

// Default values for future runtime wiring.
static const uint32_t ACK_RESPONSE_TIMEOUT_MS = 3000;
static const uint8_t ACK_RETRY_LIMIT = 3;
