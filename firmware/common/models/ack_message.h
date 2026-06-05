#pragma once

#include <stdint.h>
#include "../protocols/ack_protocol.h"

// Shared ACK message for future inter-device delivery confirmation.
// This is defined now so pen/inner/outer can later adopt one common message
// format without introducing per-unit variants.

#pragma pack(push, 1)
struct AckMessage {
    uint8_t  protocolVersion;   // ACK_PROTOCOL_VERSION
    uint8_t  messageType;       // AckMessageType
    uint8_t  sourcePeerType;    // AckPeerType
    uint8_t  targetPeerType;    // AckPeerType
    uint32_t bootSessionId;     // sender boot/session identifier
    uint32_t sourceSequence;    // sender-side event sequence number
    char     eventUid[40];      // stable event identifier
    uint8_t  resultCode;        // AckResultCode
    uint8_t  reserved;          // reserved for alignment / future flags
    uint16_t retryAfterMs;      // optional backoff hint for ACK_RESULT_RETRY
};
#pragma pack(pop)
