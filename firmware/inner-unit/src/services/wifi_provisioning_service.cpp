#include "wifi_provisioning_service.h"

#include <Preferences.h>
#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <freertos/task.h>

#include "config/app_config.h"
#include "../../../common/config/wifi_provisioning_security.h"
#include "../../../common/protocols/wifi_config_packets.h"
#include "../../../common/services/espnow_peer_store.h"
#include "../../../common/services/wifi_credential_manager.h"

namespace {
struct ReceivedWifiFrame {
    uint8_t senderMac[6];
    uint16_t length;
    uint8_t data[WIFI_CONFIG_MAX_PACKET_SIZE];
};

const uint8_t BROADCAST_MAC[6] = {
    0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF
};

QueueHandle_t receivedFrameQueue = nullptr;
TaskHandle_t provisioningTaskHandle = nullptr;
EspNowPeerStore peerStore("diasmart-peer");
WifiCredentialManager credentialManager(
    "diasmart-wifi",
    WIFI_SSID,
    WIFI_PASSWORD);
uint8_t pairedOuterMac[6] = {};
bool hasPairedOuter = false;
volatile bool switchInProgress = false;
uint32_t stagedNonce = 0;
uint32_t stagedCommandHash = 0;

bool sameMac(const uint8_t left[6], const uint8_t right[6]) {
    return memcmp(left, right, 6) == 0;
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

bool addBroadcastPeer() {
    return configurePeer(BROADCAST_MAC, false);
}

void onEspNowSent(
    const uint8_t* mac,
    esp_now_send_status_t status
) {
    (void)mac;
    if (status != ESP_NOW_SEND_SUCCESS) {
        Serial.println("[ESP-NOW] Send failed");
    }
}

void onEspNowReceived(
    const uint8_t* senderMac,
    const uint8_t* data,
    int length
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

bool initializeEspNow() {
    if (esp_now_init() != ESP_OK) {
        return false;
    }
    if (esp_now_set_pmk(DIASMART_ESPNOW_PMK) != ESP_OK) {
        esp_now_deinit();
        return false;
    }

    esp_now_register_send_cb(onEspNowSent);
    esp_now_register_recv_cb(onEspNowReceived);
    if (!addBroadcastPeer()) {
        esp_now_deinit();
        return false;
    }
    if (hasPairedOuter &&
        !configurePeer(pairedOuterMac, true)) {
        esp_now_deinit();
        return false;
    }
    return true;
}

bool reinitializeEspNow() {
    esp_now_deinit();
    return initializeEspNow();
}

void initializePairPacket(
    WifiConfigPairPacket& packet,
    WifiConfigPacketType type,
    uint32_t nonce
) {
    memset(&packet, 0, sizeof(packet));
    initializeWifiConfigHeader(
        packet.header,
        type,
        0,
        nonce,
        0,
        0,
        nullptr);
}

void sendStageAck(
    const WifiConfigPacketHeader& request,
    WifiConfigResultStatus status,
    WifiConfigReason reason
) {
    if (!hasPairedOuter) {
        return;
    }

    WifiConfigResultPacket response = {};
    response.payload.status = static_cast<uint8_t>(status);
    response.payload.reason = static_cast<uint16_t>(reason);
    initializeWifiConfigHeader(
        response.header,
        WifiConfigPacketType::WIFI_CONFIG_STAGE_ACK,
        sizeof(response.payload),
        request.transactionNonce,
        request.configurationVersion,
        request.commandHash,
        &response.payload);
    esp_now_send(
        pairedOuterMac,
        reinterpret_cast<const uint8_t*>(&response),
        sizeof(response));
}

void sendApplyResult(
    const WifiConfigPacketHeader& request,
    WifiConfigResultStatus status,
    WifiConfigReason reason,
    uint8_t channel,
    const IPAddress& ipAddress
) {
    if (!hasPairedOuter) {
        return;
    }

    WifiConfigResultPacket response = {};
    response.payload.status = static_cast<uint8_t>(status);
    response.payload.wifiChannel = channel;
    for (size_t i = 0; i < 4; ++i) {
        response.payload.ipAddress[i] = ipAddress[i];
    }
    response.payload.reason = static_cast<uint16_t>(reason);
    initializeWifiConfigHeader(
        response.header,
        WifiConfigPacketType::WIFI_CONFIG_RESULT,
        sizeof(response.payload),
        request.transactionNonce,
        request.configurationVersion,
        request.commandHash,
        &response.payload);
    for (uint8_t attempt = 0;
         attempt < WIFI_RESULT_SEND_ATTEMPTS;
         ++attempt) {
        esp_now_send(
            pairedOuterMac,
            reinterpret_cast<const uint8_t*>(&response),
            sizeof(response));
        if ((attempt + 1) < WIFI_RESULT_SEND_ATTEMPTS) {
            vTaskDelay(pdMS_TO_TICKS(WIFI_RESULT_RETRY_DELAY_MS));
        }
    }
}

bool connectAndLockChannel(
    const WifiConfiguration& configuration,
    uint32_t timeoutMs,
    uint8_t& channel,
    IPAddress& ipAddress
) {
    switchInProgress = true;
    esp_now_deinit();
    WiFi.setAutoReconnect(false);
    WiFi.disconnect(false);
    WiFi.mode(WIFI_STA);

    if (configuration.openNetwork != 0) {
        WiFi.begin(configuration.ssid);
    } else {
        WiFi.begin(configuration.ssid, configuration.password);
    }

    const uint32_t startMs = millis();
    while (WiFi.status() != WL_CONNECTED &&
           (millis() - startMs) < timeoutMs) {
        vTaskDelay(pdMS_TO_TICKS(250));
    }

    const bool connected = WiFi.status() == WL_CONNECTED;
    if (connected) {
        channel = WiFi.channel();
        ipAddress = WiFi.localIP();
    }
    WiFi.disconnect(false);
    if (connected) {
        esp_wifi_set_channel(channel, WIFI_SECOND_CHAN_NONE);
    } else {
        esp_wifi_set_channel(ESPNOW_CHANNEL, WIFI_SECOND_CHAN_NONE);
    }

    const bool espNowReady = initializeEspNow();
    switchInProgress = false;
    return connected && espNowReady;
}

bool restoreActiveWifiChannel(
    uint8_t& channel,
    IPAddress& ipAddress
) {
    WifiConfiguration configuration = {};
    WifiCredentialSource source = WifiCredentialSource::NONE;
    if (!credentialManager.loadActive(configuration, source)) {
        return false;
    }

    bool connected = connectAndLockChannel(
        configuration,
        WIFI_CONNECT_TIMEOUT_MS,
        channel,
        ipAddress);
    clearWifiConfiguration(configuration);

    if (!connected && source == WifiCredentialSource::NVS_CURRENT) {
        Serial.println(
            "[WiFiProvisioning] Saved recovery failed; trying development fallback");
        if (credentialManager.loadDevelopmentFallback(configuration)) {
            connected = connectAndLockChannel(
                configuration,
                WIFI_CONNECT_TIMEOUT_MS,
                channel,
                ipAddress);
        }
        clearWifiConfiguration(configuration);
    }
    return connected;
}

void handlePairRequest(const ReceivedWifiFrame& frame) {
    if (!validateWifiConfigPacket(
            frame.data,
            frame.length,
            WifiConfigPacketType::PAIR_REQUEST,
            sizeof(WifiConfigPairPacket))) {
        return;
    }
    if (hasPairedOuter &&
        !sameMac(frame.senderMac, pairedOuterMac)) {
        return;
    }

    const auto* request =
        reinterpret_cast<const WifiConfigPairPacket*>(frame.data);
    if (!configurePeer(frame.senderMac, false)) {
        return;
    }

    WifiConfigPairPacket response = {};
    initializePairPacket(
        response,
        WifiConfigPacketType::PAIR_ACK,
        request->header.transactionNonce);
    if (esp_now_send(
            frame.senderMac,
            reinterpret_cast<const uint8_t*>(&response),
            sizeof(response)) != ESP_OK) {
        return;
    }

    vTaskDelay(pdMS_TO_TICKS(200));
    if (!peerStore.save(frame.senderMac) ||
        !configurePeer(frame.senderMac, true)) {
        return;
    }
    memcpy(pairedOuterMac, frame.senderMac, 6);
    hasPairedOuter = true;
    Serial.println("[WiFiProvisioning] Paired Outer secured");
}

void handleStage(const ReceivedWifiFrame& frame) {
    if (!hasPairedOuter ||
        !sameMac(frame.senderMac, pairedOuterMac) ||
        !validateWifiConfigPacket(
            frame.data,
            frame.length,
            WifiConfigPacketType::WIFI_CONFIG_STAGE,
            sizeof(WifiConfigStagePacket))) {
        return;
    }

    const auto* request =
        reinterpret_cast<const WifiConfigStagePacket*>(frame.data);
    char commandId[WIFI_COMMAND_ID_MAX_LENGTH + 1] = {};
    snprintf(
        commandId,
        sizeof(commandId),
        "HASH-%08lX",
        static_cast<unsigned long>(request->header.commandHash));

    WifiConfiguration configuration = {};
    const WifiValidationResult validation =
        initializeWifiConfiguration(
            configuration,
            request->payload.ssid,
            request->payload.password,
            request->payload.openNetwork != 0,
            request->header.configurationVersion,
            commandId);
    if (validation != WifiValidationResult::VALID) {
        clearWifiConfiguration(configuration);
        sendStageAck(
            request->header,
            WifiConfigResultStatus::FAILED,
            WifiConfigReason::INVALID_PACKET);
        return;
    }

    if (!credentialManager.store().stagePending(configuration)) {
        clearWifiConfiguration(configuration);
        sendStageAck(
            request->header,
            WifiConfigResultStatus::FAILED,
            WifiConfigReason::STORAGE_FAILED);
        return;
    }
    clearWifiConfiguration(configuration);

    stagedNonce = request->header.transactionNonce;
    stagedCommandHash = request->header.commandHash;
    sendStageAck(
        request->header,
        WifiConfigResultStatus::STAGED,
        WifiConfigReason::NONE);
    Serial.printf(
        "[WiFiProvisioning] Pending configuration staged. version=%lu\n",
        static_cast<unsigned long>(
            request->header.configurationVersion));
}

void handleApply(const ReceivedWifiFrame& frame) {
    if (!hasPairedOuter ||
        !sameMac(frame.senderMac, pairedOuterMac) ||
        !validateWifiConfigPacket(
            frame.data,
            frame.length,
            WifiConfigPacketType::WIFI_CONFIG_APPLY,
            sizeof(WifiConfigControlPacket))) {
        return;
    }

    const auto* request =
        reinterpret_cast<const WifiConfigControlPacket*>(frame.data);
    WifiConfiguration pending = {};
    if (!credentialManager.store().loadPending(pending) ||
        request->header.transactionNonce != stagedNonce ||
        request->header.commandHash != stagedCommandHash ||
        request->header.configurationVersion !=
            pending.configurationVersion) {
        clearWifiConfiguration(pending);
        sendApplyResult(
            request->header,
            WifiConfigResultStatus::FAILED,
            WifiConfigReason::VERSION_MISMATCH,
            WiFi.channel(),
            IPAddress());
        return;
    }

    vTaskDelay(pdMS_TO_TICKS(request->payload.applyDelayMs));

    uint8_t newChannel = ESPNOW_CHANNEL;
    IPAddress newIp;
    const bool connected = connectAndLockChannel(
        pending,
        WIFI_CONNECT_TIMEOUT_MS,
        newChannel,
        newIp);
    clearWifiConfiguration(pending);

    if (connected && credentialManager.store().promotePending()) {
        sendApplyResult(
            request->header,
            WifiConfigResultStatus::CONNECTED,
            WifiConfigReason::NONE,
            newChannel,
            newIp);
        Serial.printf(
            "[WiFiProvisioning] Applied configuration. version=%lu channel=%u\n",
            static_cast<unsigned long>(
                request->header.configurationVersion),
            newChannel);
        return;
    }

    credentialManager.store().clearPending();
    uint8_t recoveryChannel = ESPNOW_CHANNEL;
    IPAddress recoveryIp;
    restoreActiveWifiChannel(recoveryChannel, recoveryIp);
    sendApplyResult(
        request->header,
        WifiConfigResultStatus::FAILED,
        WifiConfigReason::CONNECTION_FAILED,
        recoveryChannel,
        IPAddress());
    Serial.println("[WiFiProvisioning] Apply failed; previous Wi-Fi restored");
}

void provisioningTask(void* parameter) {
    (void)parameter;
    ReceivedWifiFrame frame = {};
    for (;;) {
        if (xQueueReceive(
                receivedFrameQueue,
                &frame,
                portMAX_DELAY) != pdTRUE) {
            continue;
        }

        const auto* header =
            reinterpret_cast<const WifiConfigPacketHeader*>(frame.data);
        switch (static_cast<WifiConfigPacketType>(header->packetType)) {
            case WifiConfigPacketType::PAIR_REQUEST:
                handlePairRequest(frame);
                break;
            case WifiConfigPacketType::WIFI_CONFIG_STAGE:
                handleStage(frame);
                break;
            case WifiConfigPacketType::WIFI_CONFIG_APPLY:
                handleApply(frame);
                break;
            default:
                break;
        }
        memset(&frame, 0, sizeof(frame));
    }
}
}

bool setupInnerWifiProvisioningService() {
    if (receivedFrameQueue != nullptr) {
        return true;
    }

    receivedFrameQueue = xQueueCreate(
        WIFI_CONFIG_FRAME_QUEUE_LENGTH,
        sizeof(ReceivedWifiFrame));
    if (receivedFrameQueue == nullptr) {
        return false;
    }

    hasPairedOuter = peerStore.load(pairedOuterMac);
    if (!initializeEspNow()) {
        vQueueDelete(receivedFrameQueue);
        receivedFrameQueue = nullptr;
        return false;
    }

    if (xTaskCreatePinnedToCore(
            provisioningTask,
            "WiFiProvision",
            WIFI_PROVISIONING_TASK_STACK,
            nullptr,
            2,
            &provisioningTaskHandle,
            0) != pdPASS) {
        esp_now_deinit();
        vQueueDelete(receivedFrameQueue);
        receivedFrameQueue = nullptr;
        return false;
    }

    Serial.printf(
        "[ESP-NOW] Ready on channel %d; secure peer=%s\n",
        static_cast<int>(WiFi.channel()),
        hasPairedOuter ? "restored" : "awaiting pairing");
    return true;
}

bool sendInnerSensorPacket(const uint8_t* data, size_t length) {
    if (switchInProgress || data == nullptr || length == 0) {
        return false;
    }
    return esp_now_send(BROADCAST_MAC, data, length) == ESP_OK;
}

bool isInnerWifiSwitchInProgress() {
    return switchInProgress;
}
