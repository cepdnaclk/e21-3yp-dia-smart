#include "local_provisioning_service.h"

#include <ArduinoJson.h>
#include <WebServer.h>
#include <WiFi.h>
#include <esp_system.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

#include "config/app_config.h"
#include "managers/wifi_manager.h"
#include "services/wifi_provisioning_coordinator.h"
#include "../../../common/config/wifi_provisioning_security.h"

namespace {
WebServer server(80);
TaskHandle_t serverTaskHandle = nullptr;
bool routesConfigured = false;
volatile bool provisioningModeActive = false;
uint32_t successObservedAtMs = 0;

void addCorsHeaders() {
    server.sendHeader("Access-Control-Allow-Origin", "*");
    server.sendHeader(
        "Access-Control-Allow-Methods",
        "GET,POST,OPTIONS");
    server.sendHeader(
        "Access-Control-Allow-Headers",
        "Content-Type");
    server.sendHeader("Cache-Control", "no-store");
}

void sendJson(int statusCode, const String& body) {
    addCorsHeaders();
    server.send(statusCode, "application/json", body);
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

void handleProvisionRequest() {
    String body = server.arg("plain");
    if (body.length() == 0 ||
        body.length() > LOCAL_PROVISION_MAX_BODY_BYTES) {
        body = "";
        sendJson(
            400,
            "{\"status\":\"error\",\"message\":\"INVALID_REQUEST\"}");
        return;
    }

    JsonDocument document;
    if (deserializeJson(document, body) ||
        !document["ssid"].is<const char*>() ||
        !document["password"].is<const char*>()) {
        body = "";
        sendJson(
            400,
            "{\"status\":\"error\",\"message\":\"INVALID_REQUEST\"}");
        return;
    }
    body = "";

    const char* ssid = document["ssid"];
    const char* password = document["password"];
    WifiCredentialStore& store = wifiCredentialStore();

    WifiConfiguration pending = {};
    const bool hasPending = store.loadPending(pending);
    if (hasPending && sameCredentials(pending, ssid, password)) {
        const uint32_t version = pending.configurationVersion;
        clearWifiConfiguration(pending);
        notifyWifiProvisioningStaged(version);
        sendJson(202, "{\"status\":\"accepted\"}");
        return;
    }

    uint32_t nextVersion = 1;
    WifiConfiguration current = {};
    if (store.loadCurrent(current)) {
        nextVersion = current.configurationVersion + 1;
    }
    clearWifiConfiguration(current);
    if (hasPending &&
        pending.configurationVersion >= nextVersion) {
        nextVersion = pending.configurationVersion + 1;
    }
    clearWifiConfiguration(pending);

    char commandId[WIFI_COMMAND_ID_MAX_LENGTH + 1] = {};
    snprintf(
        commandId,
        sizeof(commandId),
        "LOCAL-%08lX",
        static_cast<unsigned long>(esp_random()));

    WifiConfiguration configuration = {};
    const WifiValidationResult validation =
        initializeWifiConfiguration(
            configuration,
            ssid,
            password,
            false,
            nextVersion,
            commandId);
    if (validation != WifiValidationResult::VALID) {
        clearWifiConfiguration(configuration);
        sendJson(
            400,
            "{\"status\":\"error\",\"message\":\"INVALID_WIFI_CREDENTIALS\"}");
        return;
    }

    if (!store.stagePending(configuration)) {
        clearWifiConfiguration(configuration);
        sendJson(
            500,
            "{\"status\":\"error\",\"message\":\"CREDENTIAL_STORAGE_FAILED\"}");
        return;
    }
    clearWifiConfiguration(configuration);
    notifyWifiProvisioningStaged(nextVersion);
    successObservedAtMs = 0;
    Serial.printf(
        "[LocalProvisioning] Configuration accepted. version=%lu\n",
        static_cast<unsigned long>(nextVersion));
    sendJson(202, "{\"status\":\"accepted\"}");
}

void handleProvisionStatus() {
    WifiProvisioningRuntimeStatus runtime = {};
    getWifiProvisioningRuntimeStatus(runtime);

    const char* status = "connecting";
    if (runtime.state == WifiProvisioningRuntimeState::IDLE) {
        status = "idle";
    } else if (
        runtime.state == WifiProvisioningRuntimeState::SUCCESS) {
        status = "success";
    } else if (
        runtime.state == WifiProvisioningRuntimeState::FAILED) {
        status = "error";
    }

    JsonDocument document;
    document["status"] = status;
    document["outerStatus"] = runtime.outerStatus;
    document["innerStatus"] = runtime.innerStatus;
    document["message"] = runtime.message;

    String response;
    serializeJson(document, response);
    sendJson(200, response);
}

void configureRoutes() {
    if (routesConfigured) {
        return;
    }

    server.on(
        "/api/provision",
        HTTP_OPTIONS,
        []() {
            addCorsHeaders();
            server.send(204);
        });
    server.on(
        "/api/provision/status",
        HTTP_OPTIONS,
        []() {
            addCorsHeaders();
            server.send(204);
        });
    server.on(
        "/api/provision",
        HTTP_POST,
        handleProvisionRequest);
    server.on(
        "/api/provision/status",
        HTTP_GET,
        handleProvisionStatus);
    server.onNotFound(
        []() {
            sendJson(
                404,
                "{\"status\":\"error\",\"message\":\"NOT_FOUND\"}");
        });
    routesConfigured = true;
}

void serverTask(void* parameter) {
    (void)parameter;
    for (;;) {
        if (provisioningModeActive) {
            server.handleClient();

            WifiProvisioningRuntimeStatus runtime = {};
            getWifiProvisioningRuntimeStatus(runtime);
            if (runtime.state ==
                WifiProvisioningRuntimeState::SUCCESS) {
                if (successObservedAtMs == 0) {
                    successObservedAtMs = millis();
                } else if (
                    (millis() - successObservedAtMs) >=
                    LOCAL_PROVISION_SUCCESS_GRACE_MS) {
                    server.stop();
                    WiFi.softAPdisconnect(false);
                    provisioningModeActive = false;
                    successObservedAtMs = 0;
                    Serial.println(
                        "[LocalProvisioning] Setup AP stopped");
                }
            }
        }
        vTaskDelay(pdMS_TO_TICKS(20));
    }
}
}

bool startLocalProvisioningMode() {
    if (provisioningModeActive) {
        return true;
    }

    configureRoutes();
    const wifi_mode_t currentMode = WiFi.getMode();
    if (currentMode == WIFI_STA) {
        WiFi.mode(WIFI_AP_STA);
    } else if (currentMode == WIFI_OFF) {
        WiFi.mode(WIFI_AP);
    }

    String setupSsid = "DiaSmart-";
    setupSsid += DEVICE_UID_OUTER;
    const uint8_t setupChannel =
        isWiFiConnected() ? WiFi.channel() : ESPNOW_CHANNEL;
    if (!WiFi.softAP(
            setupSsid.c_str(),
            DIASMART_SETUP_AP_PASSWORD,
            setupChannel,
            false,
            2)) {
        Serial.println("[LocalProvisioning] Setup AP start failed");
        return false;
    }

    server.begin();
    provisioningModeActive = true;
    successObservedAtMs = 0;
    Serial.printf(
        "[LocalProvisioning] Setup mode ready: %s at 192.168.4.1\n",
        setupSsid.c_str());
    return true;
}

bool setupLocalProvisioningService() {
    if (serverTaskHandle == nullptr &&
        xTaskCreatePinnedToCore(
            serverTask,
            "LocalProvision",
            LOCAL_PROVISIONING_TASK_STACK,
            nullptr,
            1,
            &serverTaskHandle,
            0) != pdPASS) {
        return false;
    }

    WifiConfiguration current = {};
    const bool hasSavedConfiguration =
        wifiCredentialStore().loadCurrent(current);
    clearWifiConfiguration(current);
    if (!hasSavedConfiguration ||
        !isWiFiConnected() ||
        isWifiUsingDevelopmentFallback()) {
        return startLocalProvisioningMode();
    }
    return true;
}

bool isLocalProvisioningModeActive() {
    return provisioningModeActive;
}
