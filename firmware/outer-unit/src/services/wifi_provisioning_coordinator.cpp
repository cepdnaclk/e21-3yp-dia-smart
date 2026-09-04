#include "wifi_provisioning_coordinator.h"

#include <Preferences.h>
#include <WiFi.h>
#include <esp_now.h>
#include <esp_system.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>

#include "config/app_config.h"
#include "managers/wifi_manager.h"
#include "services/wifi_command_service.h"
#include "../../../common/config/wifi_provisioning_security.h"
#include "../../../common/protocols/wifi_config_packets.h"
#include "../../../common/services/espnow_peer_store.h"

namespace {
struct ReceivedWifiFrame {
    uint8_t senderMac[6];
    uint16_t length;
    uint8_t data[WIFI_CONFIG_MAX_PACKET_SIZE];
};

QueueHandle_t receivedFrameQueue = nullptr;
QueueHandle_t innerMacQueue = nullptr;
TaskHandle_t coordinatorTaskHandle = nullptr;
EspNowPeerStore peerStore("diasmart-peer");
uint8_t pairedInnerMac[6] = {};
bool hasPairedInner = false;
volatile uint32_t nextProvisioningAttemptMs = 0;
WifiProvisioningRuntimeStatus runtimeStatus = {
    WifiProvisioningRuntimeState::IDLE,
    "PENDING",
    "PENDING",
    "IDLE",
    0
};
portMUX_TYPE statusMux = portMUX_INITIALIZER_UNLOCKED;

bool sameMac(const uint8_t left[6], const uint8_t right[6]) {
    return memcmp(left, right, 6) == 0;
}

void setRuntimeStatus(
    WifiProvisioningRuntimeState state,
    const char* outerStatus,
    const char* innerStatus,
    const char* message,
    uint32_t configurationVersion
) {
    portENTER_CRITICAL(&statusMux);
    runtimeStatus.state = state;
    strlcpy(
        runtimeStatus.outerStatus,
        outerStatus,
        sizeof(runtimeStatus.outerStatus));
    strlcpy(
        runtimeStatus.innerStatus,
        innerStatus,
        sizeof(runtimeStatus.innerStatus));
    strlcpy(
        runtimeStatus.message,
        message,
        sizeof(runtimeStatus.message));
    runtimeStatus.configurationVersion = configurationVersion;
    portEXIT_CRITICAL(&statusMux);
}

bool configurePeer(const uint8_t peerMac[6], bool encrypted) {
    if (esp_now_is_peer_exist(peerMac)) {
        esp_now_del_peer(peerMac);
    }

    esp_now_peer_info_t peer = {};
    memcpy(peer.peer_addr, peerMac, 6);
    peer.channel = 0;
    peer.encrypt = encrypted;
    if (encrypted) {
        memcpy(peer.lmk, DIASMART_ESPNOW_LMK, ESP_NOW_KEY_LEN);
    }
    return esp_now_add_peer(&peer) == ESP_OK;
}

bool waitForPacket(
    WifiConfigPacketType packetType,
    uint32_t nonce,
    uint32_t configurationVersion,
    ReceivedWifiFrame& matchingFrame,
    uint32_t timeoutMs
) {
    const uint32_t startMs = millis();
    ReceivedWifiFrame frame = {};
    while ((millis() - startMs) < timeoutMs) {
        const uint32_t remaining =
            timeoutMs - (millis() - startMs);
        if (xQueueReceive(
                receivedFrameQueue,
                &frame,
                pdMS_TO_TICKS(remaining)) != pdTRUE) {
            return false;
        }

        const auto* header =
            reinterpret_cast<const WifiConfigPacketHeader*>(frame.data);
        if (sameMac(frame.senderMac, pairedInnerMac) &&
            header->packetType == static_cast<uint8_t>(packetType) &&
            header->transactionNonce == nonce &&
            (configurationVersion == 0 ||
             header->configurationVersion == configurationVersion)) {
            matchingFrame = frame;
            return true;
        }
        memset(&frame, 0, sizeof(frame));
    }
    return false;
}

bool pairInner(const uint8_t innerMac[6]) {
    if (!configurePeer(innerMac, false)) {
        return false;
    }

    const uint32_t nonce = esp_random();
    WifiConfigPairPacket request = {};
    initializeWifiConfigHeader(
        request.header,
        WifiConfigPacketType::PAIR_REQUEST,
        0,
        nonce,
        0,
        0,
        nullptr);
    if (esp_now_send(
            innerMac,
            reinterpret_cast<const uint8_t*>(&request),
            sizeof(request)) != ESP_OK) {
        return false;
    }

    memcpy(pairedInnerMac, innerMac, 6);
    ReceivedWifiFrame response = {};
    if (!waitForPacket(
            WifiConfigPacketType::PAIR_ACK,
            nonce,
            0,
            response,
            WIFI_PAIRING_TIMEOUT_MS) ||
        !validateWifiConfigPacket(
            response.data,
            response.length,
            WifiConfigPacketType::PAIR_ACK,
            sizeof(WifiConfigPairPacket))) {
        memset(pairedInnerMac, 0, sizeof(pairedInnerMac));
        return false;
    }

    if (!peerStore.save(innerMac) ||
        !configurePeer(innerMac, true)) {
        memset(pairedInnerMac, 0, sizeof(pairedInnerMac));
        return false;
    }

    hasPairedInner = true;
    vTaskDelay(pdMS_TO_TICKS(250));
    Serial.println("[WiFiProvisioning] Paired Inner secured");
    return true;
}

bool stageOnInner(
    const WifiConfiguration& pending,
    uint32_t nonce
) {
    WifiConfigStagePacket packet = {};
    strlcpy(packet.payload.ssid, pending.ssid, sizeof(packet.payload.ssid));
    strlcpy(
        packet.payload.password,
        pending.password,
        sizeof(packet.payload.password));
    packet.payload.openNetwork = pending.openNetwork;
    initializeWifiConfigHeader(
        packet.header,
        WifiConfigPacketType::WIFI_CONFIG_STAGE,
        sizeof(packet.payload),
        nonce,
        pending.configurationVersion,
        wifiConfigCommandHash(pending.commandId),
        &packet.payload);

    bool accepted = false;
    for (uint8_t attempt = 0;
         attempt < WIFI_CONFIG_SEND_ATTEMPTS && !accepted;
         ++attempt) {
        if (esp_now_send(
                pairedInnerMac,
                reinterpret_cast<const uint8_t*>(&packet),
                sizeof(packet)) != ESP_OK) {
            vTaskDelay(pdMS_TO_TICKS(250));
            continue;
        }

        ReceivedWifiFrame response = {};
        if (waitForPacket(
                WifiConfigPacketType::WIFI_CONFIG_STAGE_ACK,
                nonce,
                pending.configurationVersion,
                response,
                WIFI_STAGE_ACK_TIMEOUT_MS) &&
            validateWifiConfigPacket(
                response.data,
                response.length,
                WifiConfigPacketType::WIFI_CONFIG_STAGE_ACK,
                sizeof(WifiConfigResultPacket))) {
            const auto* result =
                reinterpret_cast<const WifiConfigResultPacket*>(
                    response.data);
            accepted =
                result->payload.status ==
                static_cast<uint8_t>(WifiConfigResultStatus::STAGED);
        }
    }
    memset(&packet, 0, sizeof(packet));
    return accepted;
}

bool sendApply(
    const WifiConfiguration& pending,
    uint32_t nonce
) {
    WifiConfigControlPacket packet = {};
    packet.payload.applyDelayMs = WIFI_CONFIG_APPLY_DELAY_MS;
    initializeWifiConfigHeader(
        packet.header,
        WifiConfigPacketType::WIFI_CONFIG_APPLY,
        sizeof(packet.payload),
        nonce,
        pending.configurationVersion,
        wifiConfigCommandHash(pending.commandId),
        &packet.payload);
    const bool sent = esp_now_send(
        pairedInnerMac,
        reinterpret_cast<const uint8_t*>(&packet),
        sizeof(packet)) == ESP_OK;
    memset(&packet, 0, sizeof(packet));
    return sent;
}

void queueFailure(
    const WifiConfiguration& pending,
    const char* reason
) {
    setRuntimeStatus(
        WifiProvisioningRuntimeState::FAILED,
        "FAILED",
        "FAILED",
        reason,
        pending.configurationVersion);
    queueWifiCommandStatus(
        pending.commandId,
        pending.configurationVersion,
        "FAILED",
        reason);
}

void keepPendingForRetry(
    const WifiConfiguration& pending,
    const char* reason
) {
    setRuntimeStatus(
        WifiProvisioningRuntimeState::WAITING_FOR_INNER,
        isWiFiConnected() ? "CONNECTED" : "PENDING",
        "PENDING",
        reason,
        pending.configurationVersion);
    Serial.printf(
        "[WiFiProvisioning] %s; retrying Inner in %lu ms\n",
        reason,
        static_cast<unsigned long>(WIFI_PROVISION_RETRY_DELAY_MS));
}

void restoreCurrentWifi() {
    WifiConfiguration current = {};
    WifiCredentialSource source = WifiCredentialSource::NONE;
    if (loadActiveWifiConfiguration(current, source)) {
        connectUsingWifiConfiguration(
            current,
            WIFI_CONNECT_TIMEOUT_MS,
            true);
    }
    clearWifiConfiguration(current);
}

void applyPendingConfiguration() {
    WifiCredentialStore& store = wifiCredentialStore();
    WifiConfiguration pending = {};
    if (!store.loadPending(pending)) {
        return;
    }

    setRuntimeStatus(
        WifiProvisioningRuntimeState::WAITING_FOR_INNER,
        "PENDING",
        "PENDING",
        "WAITING_FOR_INNER",
        pending.configurationVersion);
    if (!hasPairedInner) {
        clearWifiConfiguration(pending);
        return;
    }

    WifiConfiguration current = {};
    if (!store.loadCurrent(current)) {
        WifiCredentialSource source = WifiCredentialSource::NONE;
        if (loadActiveWifiConfiguration(current, source)) {
            store.saveCurrent(current);
        }
    }
    clearWifiConfiguration(current);

    const uint32_t nonce = esp_random();
    setRuntimeStatus(
        WifiProvisioningRuntimeState::STAGING_INNER,
        "PENDING",
        "PENDING",
        "CONFIGURING_INNER",
        pending.configurationVersion);
    if (!stageOnInner(pending, nonce)) {
        keepPendingForRetry(pending, "WAITING_FOR_INNER");
        clearWifiConfiguration(pending);
        return;
    }

    if (!sendApply(pending, nonce)) {
        keepPendingForRetry(pending, "WAITING_FOR_INNER");
        clearWifiConfiguration(pending);
        return;
    }

    setRuntimeStatus(
        WifiProvisioningRuntimeState::APPLYING,
        "CONNECTING",
        "CONNECTING",
        "CONNECTING_WIFI",
        pending.configurationVersion);
    vTaskDelay(pdMS_TO_TICKS(WIFI_CONFIG_APPLY_DELAY_MS));

    const bool outerConnected = connectUsingWifiConfiguration(
        pending,
        WIFI_CONNECT_TIMEOUT_MS,
        false);
    if (!outerConnected) {
        restoreCurrentWifi();
        keepPendingForRetry(pending, "OUTER_WIFI_RETRY");
        clearWifiConfiguration(pending);
        return;
    }

    setRuntimeStatus(
        WifiProvisioningRuntimeState::OUTER_CONNECTED,
        "CONNECTED",
        "CONNECTING",
        "OUTER_CONNECTED",
        pending.configurationVersion);
    ReceivedWifiFrame innerResultFrame = {};
    uint8_t innerIp[4] = {};
    bool innerConnected = false;
    if (waitForPacket(
            WifiConfigPacketType::WIFI_CONFIG_RESULT,
            nonce,
            pending.configurationVersion,
            innerResultFrame,
            WIFI_INNER_RESULT_TIMEOUT_MS) &&
        validateWifiConfigPacket(
            innerResultFrame.data,
            innerResultFrame.length,
            WifiConfigPacketType::WIFI_CONFIG_RESULT,
            sizeof(WifiConfigResultPacket))) {
        const auto* result =
            reinterpret_cast<const WifiConfigResultPacket*>(
                innerResultFrame.data);
        memcpy(innerIp, result->payload.ipAddress, sizeof(innerIp));
        if (result->payload.status ==
            static_cast<uint8_t>(
                WifiConfigResultStatus::CONNECTED)) {
            innerConnected = true;
        }
    }

    if (!innerConnected) {
        queueInnerWifiConfigurationResult(
            pending.commandId,
            "PENDING",
            innerIp,
            "WAITING_FOR_INNER");
        restoreCurrentWifi();
        keepPendingForRetry(pending, "WAITING_FOR_INNER");
        clearWifiConfiguration(pending);
        return;
    }

    if (!store.promotePending()) {
        restoreCurrentWifi();
        queueFailure(pending, "CREDENTIAL_PROMOTION_FAILED");
        clearWifiConfiguration(pending);
        return;
    }

    queueWifiCommandStatus(
        pending.commandId,
        pending.configurationVersion,
        "APPLIED",
        "BOTH_UNITS_CONNECTED");
    queueInnerWifiConfigurationResult(
        pending.commandId,
        "CONNECTED",
        innerIp,
        "INNER_WIFI_CONNECTED");
    setRuntimeStatus(
        WifiProvisioningRuntimeState::SUCCESS,
        "CONNECTED",
        "CONNECTED",
        "BOTH_UNITS_CONNECTED",
        pending.configurationVersion);
    clearWifiConfiguration(pending);
}

void coordinatorTask(void* parameter) {
    (void)parameter;
    uint8_t candidateMac[6] = {};

    for (;;) {
        if (!hasPairedInner &&
            xQueueReceive(
                innerMacQueue,
                candidateMac,
                pdMS_TO_TICKS(250)) == pdTRUE) {
            pairInner(candidateMac);
            memset(candidateMac, 0, sizeof(candidateMac));
        }

        if (hasPairedInner &&
            wifiCredentialStore().transactionState() ==
                WifiTransactionState::STAGED &&
            (int32_t)(millis() - nextProvisioningAttemptMs) >= 0) {
            applyPendingConfiguration();
            nextProvisioningAttemptMs =
                millis() + WIFI_PROVISION_RETRY_DELAY_MS;
        }

        vTaskDelay(pdMS_TO_TICKS(250));
    }
}
}

bool setupWifiProvisioningCoordinator() {
    if (receivedFrameQueue != nullptr) {
        return true;
    }

    receivedFrameQueue = xQueueCreate(
        WIFI_CONFIG_FRAME_QUEUE_LENGTH,
        sizeof(ReceivedWifiFrame));
    innerMacQueue = xQueueCreate(1, 6);
    if (receivedFrameQueue == nullptr || innerMacQueue == nullptr) {
        return false;
    }

    if (esp_now_set_pmk(DIASMART_ESPNOW_PMK) != ESP_OK) {
        return false;
    }

    hasPairedInner = peerStore.load(pairedInnerMac);
    if (hasPairedInner &&
        !configurePeer(pairedInnerMac, true)) {
        return false;
    }

    if (xTaskCreatePinnedToCore(
            coordinatorTask,
            "WiFiCoord",
            WIFI_PROVISIONING_TASK_STACK,
            nullptr,
            2,
            &coordinatorTaskHandle,
            1) != pdPASS) {
        return false;
    }

    Serial.printf(
        "[WiFiProvisioning] Coordinator ready; secure peer=%s\n",
        hasPairedInner ? "restored" : "awaiting Inner");
    return true;
}

void observeInnerSensorMac(const uint8_t senderMac[6]) {
    if (senderMac == nullptr ||
        hasPairedInner ||
        innerMacQueue == nullptr) {
        return;
    }
    xQueueOverwrite(innerMacQueue, senderMac);
}

void handleOuterWifiProvisioningPacket(
    const uint8_t senderMac[6],
    const uint8_t* data,
    size_t length
) {
    if (!isWifiConfigPacket(data, length) ||
        receivedFrameQueue == nullptr) {
        return;
    }

    ReceivedWifiFrame frame = {};
    memcpy(frame.senderMac, senderMac, 6);
    frame.length = static_cast<uint16_t>(length);
    memcpy(frame.data, data, length);
    xQueueSend(receivedFrameQueue, &frame, 0);
}

void getWifiProvisioningRuntimeStatus(
    WifiProvisioningRuntimeStatus& status
) {
    portENTER_CRITICAL(&statusMux);
    status = runtimeStatus;
    portEXIT_CRITICAL(&statusMux);
}

void notifyWifiProvisioningStaged(uint32_t configurationVersion) {
    nextProvisioningAttemptMs = 0;
    setRuntimeStatus(
        WifiProvisioningRuntimeState::WAITING_FOR_INNER,
        "PENDING",
        "PENDING",
        "WAITING_FOR_INNER",
        configurationVersion);
}
