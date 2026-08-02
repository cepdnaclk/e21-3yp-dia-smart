#pragma once

#include <stddef.h>
#include <stdint.h>
#include <string.h>

constexpr size_t WIFI_SSID_MAX_LENGTH = 32;
constexpr size_t WIFI_PASSWORD_MAX_LENGTH = 63;
constexpr size_t WIFI_PASSWORD_MIN_LENGTH = 8;
constexpr size_t WIFI_COMMAND_ID_MAX_LENGTH = 39;

enum class WifiValidationResult : uint8_t {
    VALID = 0,
    MISSING_SSID,
    SSID_TOO_LONG,
    PASSWORD_TOO_SHORT,
    PASSWORD_TOO_LONG,
    OPEN_NETWORK_HAS_PASSWORD,
    COMMAND_ID_TOO_LONG,
    CHECKSUM_MISMATCH
};

enum class WifiCredentialSource : uint8_t {
    NONE = 0,
    NVS_CURRENT,
    DEVELOPMENT_FALLBACK
};

enum class WifiTransactionState : uint8_t {
    IDLE = 0,
    STAGED,
    APPLYING,
    APPLIED,
    ROLLING_BACK,
    ROLLED_BACK
};

#pragma pack(push, 1)
struct WifiConfiguration {
    char ssid[WIFI_SSID_MAX_LENGTH + 1];
    char password[WIFI_PASSWORD_MAX_LENGTH + 1];
    char commandId[WIFI_COMMAND_ID_MAX_LENGTH + 1];
    uint32_t configurationVersion;
    uint32_t checksum;
    uint8_t openNetwork;
    uint8_t valid;
};
#pragma pack(pop)

inline void clearWifiConfiguration(WifiConfiguration& configuration) {
    volatile uint8_t* bytes =
        reinterpret_cast<volatile uint8_t*>(&configuration);
    for (size_t i = 0; i < sizeof(WifiConfiguration); ++i) {
        bytes[i] = 0;
    }
}
inline uint32_t updateWifiChecksum(
    uint32_t checksum,
    const uint8_t* data,
    size_t length
) {
    constexpr uint32_t FNV_PRIME = 16777619U;
    for (size_t i = 0; i < length; ++i) {
        checksum ^= data[i];
        checksum *= FNV_PRIME;
    }
    return checksum;
}

inline uint32_t calculateWifiConfigurationChecksum(
    const WifiConfiguration& configuration
) {
    constexpr uint32_t FNV_OFFSET_BASIS = 2166136261U;
    uint32_t checksum = FNV_OFFSET_BASIS;

    checksum = updateWifiChecksum(
        checksum,
        reinterpret_cast<const uint8_t*>(configuration.ssid),
        strnlen(configuration.ssid, sizeof(configuration.ssid)));
    checksum = updateWifiChecksum(
        checksum,
        reinterpret_cast<const uint8_t*>(configuration.password),
        strnlen(configuration.password, sizeof(configuration.password)));
    checksum = updateWifiChecksum(
        checksum,
        reinterpret_cast<const uint8_t*>(configuration.commandId),
        strnlen(configuration.commandId, sizeof(configuration.commandId)));
    checksum = updateWifiChecksum(
        checksum,
        reinterpret_cast<const uint8_t*>(&configuration.configurationVersion),
        sizeof(configuration.configurationVersion));
    checksum = updateWifiChecksum(
        checksum,
        &configuration.openNetwork,
        sizeof(configuration.openNetwork));
    return checksum;
}

inline WifiValidationResult validateWifiConfigurationFields(
    const WifiConfiguration& configuration
) {
    const size_t ssidLength =
        strnlen(configuration.ssid, sizeof(configuration.ssid));
    if (ssidLength == 0) {
        return WifiValidationResult::MISSING_SSID;
    }
    if (ssidLength > WIFI_SSID_MAX_LENGTH) {
        return WifiValidationResult::SSID_TOO_LONG;
    }

    const size_t passwordLength =
        strnlen(configuration.password, sizeof(configuration.password));
    if (configuration.openNetwork != 0) {
        if (passwordLength != 0) {
            return WifiValidationResult::OPEN_NETWORK_HAS_PASSWORD;
        }
    } else {
        if (passwordLength < WIFI_PASSWORD_MIN_LENGTH) {
            return WifiValidationResult::PASSWORD_TOO_SHORT;
        }
        if (passwordLength > WIFI_PASSWORD_MAX_LENGTH) {
            return WifiValidationResult::PASSWORD_TOO_LONG;
        }
    }

    if (strnlen(configuration.commandId, sizeof(configuration.commandId)) >
        WIFI_COMMAND_ID_MAX_LENGTH) {
        return WifiValidationResult::COMMAND_ID_TOO_LONG;
    }

    return WifiValidationResult::VALID;
}

inline WifiValidationResult validateStoredWifiConfiguration(
    const WifiConfiguration& configuration
) {
    WifiValidationResult fields = validateWifiConfigurationFields(configuration);
    if (fields != WifiValidationResult::VALID) {
        return fields;
    }
    if (configuration.valid == 0 ||
        configuration.checksum !=
            calculateWifiConfigurationChecksum(configuration)) {
        return WifiValidationResult::CHECKSUM_MISMATCH;
    }
    return WifiValidationResult::VALID;
}

inline WifiValidationResult initializeWifiConfiguration(
    WifiConfiguration& configuration,
    const char* ssid,
    const char* password,
    bool openNetwork,
    uint32_t configurationVersion,
    const char* commandId
) {
    clearWifiConfiguration(configuration);

    if (ssid == nullptr || password == nullptr || commandId == nullptr) {
        return WifiValidationResult::MISSING_SSID;
    }

    const size_t ssidLength = strnlen(ssid, WIFI_SSID_MAX_LENGTH + 2);
    const size_t passwordLength =
        strnlen(password, WIFI_PASSWORD_MAX_LENGTH + 2);
    const size_t commandIdLength =
        strnlen(commandId, WIFI_COMMAND_ID_MAX_LENGTH + 2);

    if (ssidLength > WIFI_SSID_MAX_LENGTH) {
        return WifiValidationResult::SSID_TOO_LONG;
    }
    if (passwordLength > WIFI_PASSWORD_MAX_LENGTH) {
        return WifiValidationResult::PASSWORD_TOO_LONG;
    }
    if (commandIdLength > WIFI_COMMAND_ID_MAX_LENGTH) {
        return WifiValidationResult::COMMAND_ID_TOO_LONG;
    }

    memcpy(configuration.ssid, ssid, ssidLength);
    memcpy(configuration.password, password, passwordLength);
    memcpy(configuration.commandId, commandId, commandIdLength);
    configuration.configurationVersion = configurationVersion;
    configuration.openNetwork = openNetwork ? 1 : 0;

    WifiValidationResult result =
        validateWifiConfigurationFields(configuration);
    if (result != WifiValidationResult::VALID) {
        clearWifiConfiguration(configuration);
        return result;
    }

    configuration.valid = 1;
    configuration.checksum =
        calculateWifiConfigurationChecksum(configuration);
    return WifiValidationResult::VALID;
}
