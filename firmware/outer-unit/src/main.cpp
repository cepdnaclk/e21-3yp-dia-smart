#include <Arduino.h>
#include <WiFi.h>
#include <esp_now.h>
#include <esp_wifi.h>
#include <time.h>
#include "config/app_config.h"
#include "models/telemetry_event.h"
#include "include/system_queues.h"
#include "managers/wifi_manager.h"
#include "services/care_plan_service.h"

// ---- Queue definitions (extern declared in system_queues.h) -------------- //
QueueHandle_t telemetryQueue;
QueueHandle_t innerPacketQueue;
QueueHandle_t glucoseQueue;
QueueHandle_t doseQueue;
QueueHandle_t keypadQueue;

volatile uint32_t g_espNowRxTotal = 0;
volatile uint32_t g_espNowRxQueued = 0;
volatile uint32_t g_espNowRxLenDrop = 0;
volatile uint32_t g_espNowRxMagicDrop = 0;
volatile uint32_t g_espNowRxQueueDrop = 0;
volatile uint32_t g_espNowRxDuplicateDrop = 0;

static uint32_t lastQueuedInnerSeq = 0;
static bool hasLastQueuedInnerSeq = false;

// ---- Task forward declarations -------------------------------------------- //
void eventAggregatorTask(void* parameter);
void mqttPublishTask(void* parameter);
void bleManagerTask(void* parameter);
void displayUiTask(void* parameter);
void keypadTask(void* parameter);

// ---- ESP-NOW receive callback --------------------------------------------- //
// Runs in WiFi/ESP-NOW task context (NOT in our FreeRTOS task).
// Must be fast — just copy and enqueue, no Serial.print inside.
static void onEspNowRecv(const uint8_t* mac, const uint8_t* data, int len) {
    (void)mac;
    g_espNowRxTotal++;
    if (len != sizeof(InnerPacket)) {
        g_espNowRxLenDrop++;
        return;
    }

    const InnerPacket* pkt = reinterpret_cast<const InnerPacket*>(data);
    if (pkt->magic != INNER_MAGIC) {
        g_espNowRxMagicDrop++;
        return;
    }
    if (hasLastQueuedInnerSeq && pkt->seq == lastQueuedInnerSeq) {
        g_espNowRxDuplicateDrop++;
        return;
    }
    if (innerPacketQueue == nullptr) {
        g_espNowRxQueueDrop++;
        return;
    }

    if (xQueueSend(innerPacketQueue, pkt, 0) == pdTRUE) {
        lastQueuedInnerSeq = pkt->seq;
        hasLastQueuedInnerSeq = true;
        g_espNowRxQueued++;
    } else {
        g_espNowRxQueueDrop++;
    }
}

// ---- ESP-NOW init --------------------------------------------------------- //
static void initEspNow() {
    if (esp_now_init() != ESP_OK) {
        Serial.println("[ESP-NOW] Init failed — halting");
        while (true) { vTaskDelay(pdMS_TO_TICKS(1000)); }
    }
    // Register recv callback — no need to add a peer for receive-only
    esp_now_register_recv_cb(onEspNowRecv);
    Serial.println("[ESP-NOW] Initialised, listening for InnerPackets");
}

// ---- NTP sync ------------------------------------------------------------- //
static void syncNtp() {
    configTime(0, 0, "pool.ntp.org", "time.nist.gov");
    Serial.print("[NTP] Waiting for time sync");
    struct tm ti;
    uint32_t start = millis();
    while (!getLocalTime(&ti) && (millis() - start) < 10000) {
        delay(500);
        Serial.print(".");
    }
    if (getLocalTime(&ti)) {
        char buf[32];
        strftime(buf, sizeof(buf), "%Y-%m-%dT%H:%M:%SZ", &ti);
        Serial.printf("\n[NTP] Time: %s\n", buf);
    } else {
        Serial.println("\n[NTP] Sync failed — timestamps will be epoch-based");
    }
}

// -------------------------------------------------------------------------- //

void setup() {
    Serial.begin(SERIAL_BAUD);
    delay(200);
    Serial.println("=== Dia-Smart Outer Unit Starting ===");

    // WiFi — must connect before ESP-NOW (channel must be locked to AP channel)
    setupWiFi();

    // NTP time sync (non-fatal — timestamps degrade gracefully)
    syncNtp();

    // Restore the last acknowledged prescription before UI tasks start.
    setupCarePlanService();

    // Create all queues before ESP-NOW registration so receive callback can enqueue.
    telemetryQueue   = xQueueCreate(QUEUE_TELEMETRY_LEN,    sizeof(TelemetryEvent));
    innerPacketQueue = xQueueCreate(QUEUE_INNER_PACKET_LEN, sizeof(InnerPacket));
    glucoseQueue     = xQueueCreate(QUEUE_GLUCOSE_LEN,      sizeof(GlucoseReading));
    doseQueue        = xQueueCreate(QUEUE_DOSE_LEN,         sizeof(DoseReading));
    keypadQueue      = xQueueCreate(QUEUE_KEYPAD_LEN,       sizeof(KeypadEvent));

    if (!telemetryQueue || !innerPacketQueue || !glucoseQueue || !doseQueue || !keypadQueue) {
        Serial.println("[Main] Queue creation failed — halting");
        while (true) { vTaskDelay(pdMS_TO_TICKS(1000)); }
    }

    Serial.printf("[Main] InnerPacket size=%u bytes\n", (unsigned)sizeof(InnerPacket));

    // ESP-NOW — init after WiFi so channel is already set by the AP association
    initEspNow();

    // eventAggregatorTask — reads sensor + dose + glucose queues → builds TelemetryEvent
    xTaskCreatePinnedToCore(
        eventAggregatorTask, "EventAgg",
        STACK_EVENT_AGG, nullptr, 2, nullptr, 1);

    // mqttPublishTask — drains telemetryQueue → serialises → publishes to AWS
    xTaskCreatePinnedToCore(
        mqttPublishTask, "MQTTPub",
        STACK_MQTT_PUBLISH, nullptr, 1, nullptr, 0);

    // bleManagerTask — BLE central: pen GATT notify + glucometer RACP
    xTaskCreatePinnedToCore(
        bleManagerTask, "BLEMgr",
        STACK_BLE_MANAGER, nullptr, 1, nullptr, 0);

    // displayUiTask - TFT dashboard. It reads the latest telemetry snapshot only.
    xTaskCreatePinnedToCore(
        displayUiTask, "DisplayUI",
        STACK_DISPLAY_UI, nullptr, 1, nullptr, 1);

    // keypadTask - scans 4x4 keypad and sends key events to eventAggregatorTask.
    xTaskCreatePinnedToCore(
        keypadTask, "Keypad",
        STACK_KEYPAD, nullptr, 1, nullptr, 1);

    Serial.println("[Main] All tasks started");
}

// FreeRTOS takes over — loop() is intentionally idle.
void loop() {
    vTaskDelay(portMAX_DELAY);
}
