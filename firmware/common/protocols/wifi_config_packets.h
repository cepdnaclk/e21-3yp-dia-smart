#pragma once

#include <stddef.h>
#include <stdint.h>
#include <string.h>

#include "../models/wifi_configuration.h"

constexpr uint32_t WIFI_CONFIG_PROTOCOL_MAGIC = 0x57434647U;
constexpr uint8_t WIFI_CONFIG_PROTOCOL_VERSION = 1;
constexpr uint16_t WIFI_CONFIG_APPLY_DELAY_MS = 1500;

enum class WifiConfigPacketType : uint8_t {
    PAIR_REQUEST = 1,
    PAIR_ACK = 2,
    WIFI_CONFIG_STAGE = 3,
    WIFI_CONFIG_STAGE_ACK = 4,
    WIFI_CONFIG_APPLY = 5,
    WIFI_CONFIG_RESULT = 6,
    WIFI_CONFIG_ROLLBACK = 7,
    WIFI_CONFIG_COMMIT = 8,
    WIFI_CONFIG_COMMIT_ACK = 9
};

enum class WifiConfigResultStatus : uint8_t {
    PENDING = 0,
    STAGED = 1,
    CONNECTED = 2,
    FAILED = 3,
    ROLLED_BACK = 4,
    COMMITTED = 5
};

enum class WifiConfigReason : uint16_t {
    NONE = 0,
    INVALID_PACKET = 1,
    WRONG_SENDER = 2,
    STORAGE_FAILED = 3,
    VERSION_MISMATCH = 4,
    CONNECTION_FAILED = 5,
    ROLLBACK_FAILED = 6
};

#pragma pack(push, 1)
struct WifiConfigPacketHeader {
    uint32_t magic;
    uint8_t protocolVersion;
    uint8_t packetType;
    uint16_t payloadLength;
    uint32_t transactionNonce;
    uint32_t configurationVersion;
    uint32_t commandHash;
    uint32_t payloadChecksum;
};

struct WifiConfigPairPacket {
    WifiConfigPacketHeader header;
};

struct WifiConfigStagePayload {
    char ssid[WIFI_SSID_MAX_LENGTH + 1];
    char password[WIFI_PASSWORD_MAX_LENGTH + 1];
    uint8_t openNetwork;
};

struct WifiConfigStagePacket {
    WifiConfigPacketHeader header;
    WifiConfigStagePayload payload;
};

struct WifiConfigControlPayload {
    uint16_t applyDelayMs;
    uint16_t reserved;
};

struct WifiConfigControlPacket {
    WifiConfigPacketHeader header;
    WifiConfigControlPayload payload;
};

struct WifiConfigResultPayload {
    uint8_t status;
    uint8_t wifiChannel;
    uint8_t ipAddress[4];
    uint16_t reason;
};

struct WifiConfigResultPacket {
    WifiConfigPacketHeader header;
    WifiConfigResultPayload payload;
};
#pragma pack(pop)

constexpr size_t WIFI_CONFIG_MAX_PACKET_SIZE =
    sizeof(WifiConfigStagePacket);
static_assert(
    WIFI_CONFIG_MAX_PACKET_SIZE <= 250,
    "Wi-Fi configuration packet exceeds ESP-NOW payload limit");

inline uint32_t wifiConfigHash(
    const uint8_t* data,
    size_t length
) {
    constexpr uint32_t FNV_OFFSET_BASIS = 2166136261U;
    constexpr uint32_t FNV_PRIME = 16777619U;
    uint32_t hash = FNV_OFFSET_BASIS;
    for (size_t i = 0; i < length; ++i) {
        hash ^= data[i];
        hash *= FNV_PRIME;
    }
    return hash;
}

inline uint32_t wifiConfigCommandHash(const char* commandId) {
    if (commandId == nullptr) {
        return 0;
    }
    return wifiConfigHash(
        reinterpret_cast<const uint8_t*>(commandId),
        strnlen(commandId, WIFI_COMMAND_ID_MAX_LENGTH + 1));
}

inline void initializeWifiConfigHeader(
    WifiConfigPacketHeader& header,
    WifiConfigPacketType packetType,
    uint16_t payloadLength,
    uint32_t transactionNonce,
    uint32_t configurationVersion,
    uint32_t commandHash,
    const void* payload
) {
    memset(&header, 0, sizeof(header));
    header.magic = WIFI_CONFIG_PROTOCOL_MAGIC;
    header.protocolVersion = WIFI_CONFIG_PROTOCOL_VERSION;
    header.packetType = static_cast<uint8_t>(packetType);
    header.payloadLength = payloadLength;
    header.transactionNonce = transactionNonce;
    header.configurationVersion = configurationVersion;
    header.commandHash = commandHash;
    header.payloadChecksum =
        payloadLength == 0 || payload == nullptr
            ? wifiConfigHash(nullptr, 0)
            : wifiConfigHash(
                  reinterpret_cast<const uint8_t*>(payload),
                  payloadLength);
}

inline bool validateWifiConfigPacket(
    const uint8_t* data,
    size_t length,
    WifiConfigPacketType expectedType,
    size_t expectedLength
) {
    if (data == nullptr ||
        length != expectedLength ||
        length < sizeof(WifiConfigPacketHeader)) {
        return false;
    }

    const auto* header =
        reinterpret_cast<const WifiConfigPacketHeader*>(data);
    if (header->magic != WIFI_CONFIG_PROTOCOL_MAGIC ||
        header->protocolVersion != WIFI_CONFIG_PROTOCOL_VERSION ||
        header->packetType != static_cast<uint8_t>(expectedType) ||
        sizeof(WifiConfigPacketHeader) + header->payloadLength != length) {
        return false;
    }

    const uint8_t* payload = data + sizeof(WifiConfigPacketHeader);
    return header->payloadChecksum ==
           wifiConfigHash(payload, header->payloadLength);
}

inline bool isWifiConfigPacket(
    const uint8_t* data,
    size_t length
) {
    if (data == nullptr || length < sizeof(WifiConfigPacketHeader)) {
        return false;
    }
    const auto* header =
        reinterpret_cast<const WifiConfigPacketHeader*>(data);
    return header->magic == WIFI_CONFIG_PROTOCOL_MAGIC &&
           header->protocolVersion == WIFI_CONFIG_PROTOCOL_VERSION &&
           length <= WIFI_CONFIG_MAX_PACKET_SIZE;
}
