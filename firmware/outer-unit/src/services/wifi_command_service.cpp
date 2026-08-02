#include "wifi_command_service.h"

#include <ArduinoJson.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <time.h>

#include "config/app_config.h"
#include "managers/wifi_manager.h"
#include "services/mqtt_service.h"

namespace {
struct WifiCommandMessage {
    uint16_t length;
    char payload[WIFI_COMMAND_MAX_BYTES + 1];
};

enum class WifiStatusEventType : uint8_t {
    COMMAND_ACK = 1,
    INNER_RESULT = 2
};

struct WifiStatusEvent {
    WifiStatusEventType eventType;
    char commandId[WIFI_COMMAND_ID_MAX_LENGTH + 1];
    char status[16];
    char message[64];
    uint32_t configurationVersion;
    uint8_t ipAddress[4];
};

QueueHandle_t wifiCommandQueue = nullptr;
QueueHandle_t wifiStatusQueue = nullptr;

void currentTimestamp(char* output, size_t outputLength) {
    struct tm timeInfo;
    if (getLocalTime(&timeInfo)) {
        strftime(output, outputLength, "%Y-%m-%dT%H:%M:%SZ", &timeInfo);
    } else {
        strncpy(
            output,
            "1970-01-01T00:00:00Z",
            outputLength - 1);
        output[outputLength - 1] = '\0';
    }
}

void publishWifiCommandAck(
    const char* commandId,
    const char* status,
    uint32_t configurationVersion,
    const char* message
) {
    if (commandId == nullptr || commandId[0] == '\0') {
        return;
    }

    JsonDocument document;
    document["commandId"] = commandId;
    document["commandType"] = "WIFI_CONFIGURATION";
    document["status"] = status;
    document["outerDeviceId"] = DEVICE_UID_OUTER;
    document["message"] = message;
    document["configurationVersion"] = configurationVersion;

    char timestamp[32];
    currentTimestamp(timestamp, sizeof(timestamp));
    document["timestamp"] = timestamp;

    String ackPayload;
    serializeJson(document, ackPayload);
    if (!publishMqttMessage(
            AWS_IOT_COMMAND_ACK_TOPIC,
            ackPayload,
            false)) {
        Serial.printf(
            "[WiFiCommand] ACK publish failed. status=%s\n",
            status);
    }
}

void publishInnerResult(const WifiStatusEvent& event) {
    JsonDocument document;
    char timestamp[32];
    currentTimestamp(timestamp, sizeof(timestamp));

    char eventId[48];
    snprintf(
        eventId,
        sizeof(eventId),
        "INNER-WIFI-%lu-%08lX",
        static_cast<unsigned long>(time(nullptr)),
        static_cast<unsigned long>(esp_random()));

    char ipAddress[16];
    snprintf(
        ipAddress,
        sizeof(ipAddress),
        "%u.%u.%u.%u",
        event.ipAddress[0],
        event.ipAddress[1],
        event.ipAddress[2],
        event.ipAddress[3]);

    document["eventId"] = eventId;
    document["commandId"] = event.commandId;
    document["eventType"] = "INNER_WIFI_CONFIGURATION_RESULT";
    document["outerDeviceId"] = DEVICE_UID_OUTER;
    document["innerDeviceId"] = DEVICE_UID_INNER;
    document["status"] = event.status;
    document["ipAddress"] = ipAddress;
    document["message"] = event.message;
    document["timestamp"] = timestamp;

    String payload;
    serializeJson(document, payload);
    if (!publishMqttMessage(
            AWS_IOT_DEVICE_TELEMETRY_TOPIC,
            payload,
            false)) {
        Serial.println("[WiFiCommand] Inner result publish failed");
    }
}

bool sameCommand(
    const WifiConfiguration& configuration,
    const char* commandId,
    uint32_t configurationVersion
) {
    return configuration.configurationVersion == configurationVersion &&
           strncmp(
               configuration.commandId,
               commandId,
               sizeof(configuration.commandId)) == 0;
}

bool sameCredentials(
    const WifiConfiguration& configuration,
    const char* ssid,
    const char* password
) {
    return configuration.openNetwork == 0 &&
           strcmp(configuration.ssid, ssid) == 0 &&
           strcmp(configuration.password, password) == 0;
}

void rejectCommand(
    const char* commandId,
    uint32_t configurationVersion,
    const char* reason
) {
    Serial.printf("[WiFiCommand] Rejected: %s\n", reason);
    publishWifiCommandAck(
        commandId,
        "REJECTED",
        configurationVersion,
        reason);
}

void handleWifiCommand(const WifiCommandMessage& message) {
    JsonDocument document;
    const DeserializationError error = deserializeJson(
        document,
        message.payload,
        message.length);
    if (error) {
        Serial.println("[WiFiCommand] Rejected malformed JSON");
        return;
    }

    const char* commandId = document["commandId"] | "";
    const char* commandType = document["commandType"] | "";
    const char* outerDeviceId = document["outerDeviceId"] | "";
    JsonVariantConst payload = document["payload"];
    const uint32_t configurationVersion =
        payload["configurationVersion"] | 0U;

    const size_t commandIdLength =
        strnlen(commandId, WIFI_COMMAND_ID_MAX_LENGTH + 2);
    if (commandIdLength == 0 ||
        commandIdLength > WIFI_COMMAND_ID_MAX_LENGTH) {
        Serial.println("[WiFiCommand] Rejected invalid command identity");
        return;
    }
    if (strcmp(commandType, "WIFI_CONFIGURATION") != 0) {
        rejectCommand(
            commandId,
            configurationVersion,
            "UNSUPPORTED_COMMAND_TYPE");
        return;
    }
    if (strcmp(outerDeviceId, DEVICE_UID_OUTER) != 0) {
        rejectCommand(
            commandId,
            configurationVersion,
            "OUTER_DEVICE_ID_MISMATCH");
        return;
    }

    publishWifiCommandAck(
        commandId,
        "RECEIVED",
        configurationVersion,
        "COMMAND_RECEIVED");

    if (payload.isNull() ||
        !payload["wifiSsid"].is<const char*>() ||
        !payload["wifiPassword"].is<const char*>() ||
        configurationVersion == 0) {
        rejectCommand(
            commandId,
            configurationVersion,
            "INVALID_CONFIGURATION_PAYLOAD");
        return;
    }

    const char* innerDeviceId = payload["innerDeviceId"] | "";
    if (innerDeviceId[0] != '\0' &&
        strcmp(innerDeviceId, DEVICE_UID_INNER) != 0) {
        rejectCommand(
            commandId,
            configurationVersion,
            "INNER_DEVICE_ID_MISMATCH");
        return;
    }

    const char* ssid = payload["wifiSsid"];
    const char* password = payload["wifiPassword"];
    WifiCredentialStore& store = wifiCredentialStore();
    WifiConfiguration current = {};
    if (store.loadCurrent(current)) {
        if (sameCommand(current, commandId, configurationVersion) ||
            (current.configurationVersion == configurationVersion &&
             sameCredentials(current, ssid, password))) {
            clearWifiConfiguration(current);
            publishWifiCommandAck(
                commandId,
                "APPLIED",
                configurationVersion,
                "CONFIGURATION_ALREADY_APPLIED");
            return;
        }
        if (configurationVersion <= current.configurationVersion) {
            clearWifiConfiguration(current);
            rejectCommand(
                commandId,
                configurationVersion,
                "STALE_CONFIGURATION_VERSION");
            return;
        }
    }
    clearWifiConfiguration(current);

    WifiConfiguration pending = {};
    if (store.loadPending(pending)) {
        if (sameCommand(pending, commandId, configurationVersion) ||
            (pending.configurationVersion == configurationVersion &&
             sameCredentials(pending, ssid, password))) {
            clearWifiConfiguration(pending);
            publishWifiCommandAck(
                commandId,
                "VALIDATED",
                configurationVersion,
                "CONFIGURATION_ALREADY_STAGED");
            return;
        }
        if (configurationVersion <= pending.configurationVersion) {
            clearWifiConfiguration(pending);
            rejectCommand(
                commandId,
                configurationVersion,
                "STALE_CONFIGURATION_VERSION");
            return;
        }
    }
    clearWifiConfiguration(pending);

    WifiConfiguration configuration = {};
    const WifiValidationResult validation =
        initializeWifiConfiguration(
            configuration,
            ssid,
            password,
            false,
            configurationVersion,
            commandId);
    if (validation != WifiValidationResult::VALID) {
        clearWifiConfiguration(configuration);
        rejectCommand(
            commandId,
            configurationVersion,
            "INVALID_WIFI_CREDENTIALS");
        return;
    }

    if (!store.stagePending(configuration)) {
        clearWifiConfiguration(configuration);
        rejectCommand(
            commandId,
            configurationVersion,
            "CREDENTIAL_STORAGE_FAILED");
        return;
    }
    clearWifiConfiguration(configuration);

    Serial.printf(
        "[WiFiCommand] Configuration staged. version=%lu\n",
        static_cast<unsigned long>(configurationVersion));
    publishWifiCommandAck(
        commandId,
        "VALIDATED",
        configurationVersion,
        "CONFIGURATION_STAGED");
}
}

bool setupWifiCommandService() {
    if (wifiCommandQueue != nullptr) {
        return true;
    }

    wifiCommandQueue = xQueueCreate(
        WIFI_COMMAND_QUEUE_LENGTH,
        sizeof(WifiCommandMessage));
    wifiStatusQueue = xQueueCreate(
        WIFI_STATUS_QUEUE_LENGTH,
        sizeof(WifiStatusEvent));
    if (wifiCommandQueue == nullptr || wifiStatusQueue == nullptr) {
        if (wifiCommandQueue != nullptr) {
            vQueueDelete(wifiCommandQueue);
            wifiCommandQueue = nullptr;
        }
        if (wifiStatusQueue != nullptr) {
            vQueueDelete(wifiStatusQueue);
            wifiStatusQueue = nullptr;
        }
        Serial.println("[WiFiCommand] Queue creation failed");
        return false;
    }
    return true;
}

bool enqueueWifiCommandPayload(
    const uint8_t* payload,
    size_t payloadLength
) {
    if (wifiCommandQueue == nullptr ||
        payload == nullptr ||
        payloadLength == 0 ||
        payloadLength > WIFI_COMMAND_MAX_BYTES) {
        return false;
    }

    WifiCommandMessage message = {};
    message.length = static_cast<uint16_t>(payloadLength);
    memcpy(message.payload, payload, payloadLength);
    message.payload[payloadLength] = '\0';
    return xQueueSend(wifiCommandQueue, &message, 0) == pdTRUE;
}

bool queueWifiCommandStatus(
    const char* commandId,
    uint32_t configurationVersion,
    const char* status,
    const char* message
) {
    if (wifiStatusQueue == nullptr ||
        commandId == nullptr ||
        status == nullptr ||
        message == nullptr) {
        return false;
    }

    WifiStatusEvent event = {};
    event.eventType = WifiStatusEventType::COMMAND_ACK;
    strlcpy(event.commandId, commandId, sizeof(event.commandId));
    strlcpy(event.status, status, sizeof(event.status));
    strlcpy(event.message, message, sizeof(event.message));
    event.configurationVersion = configurationVersion;
    return xQueueSend(wifiStatusQueue, &event, 0) == pdTRUE;
}

bool queueInnerWifiConfigurationResult(
    const char* commandId,
    const char* status,
    const uint8_t ipAddress[4],
    const char* message
) {
    if (wifiStatusQueue == nullptr ||
        commandId == nullptr ||
        status == nullptr ||
        ipAddress == nullptr ||
        message == nullptr) {
        return false;
    }

    WifiStatusEvent event = {};
    event.eventType = WifiStatusEventType::INNER_RESULT;
    strlcpy(event.commandId, commandId, sizeof(event.commandId));
    strlcpy(event.status, status, sizeof(event.status));
    strlcpy(event.message, message, sizeof(event.message));
    memcpy(event.ipAddress, ipAddress, sizeof(event.ipAddress));
    return xQueueSend(wifiStatusQueue, &event, 0) == pdTRUE;
}

void processPendingWifiCommand() {
    if (wifiCommandQueue == nullptr) {
        return;
    }

    WifiCommandMessage message = {};
    if (xQueueReceive(wifiCommandQueue, &message, 0) == pdTRUE) {
        handleWifiCommand(message);
        memset(&message, 0, sizeof(message));
    }

    WifiStatusEvent event = {};
    if (wifiStatusQueue != nullptr &&
        xQueueReceive(wifiStatusQueue, &event, 0) == pdTRUE) {
        if (event.eventType == WifiStatusEventType::COMMAND_ACK) {
            publishWifiCommandAck(
                event.commandId,
                event.status,
                event.configurationVersion,
                event.message);
        } else if (
            event.eventType == WifiStatusEventType::INNER_RESULT) {
            publishInnerResult(event);
        }
        memset(&event, 0, sizeof(event));
    }
}
