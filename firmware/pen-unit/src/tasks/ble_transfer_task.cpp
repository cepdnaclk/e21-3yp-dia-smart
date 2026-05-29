#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include "../config/app_config.h"
#include "../models/dose_event.h"

extern QueueHandle_t doseEventQueue;

// ---- BLE connection state (set from server callbacks) -------------------- //
static volatile bool deviceConnected    = false;
static volatile bool prevConnected      = false;

static BLEServer*         pServer         = nullptr;
static BLECharacteristic* pDoseCharistic  = nullptr;

// ---- BLE Server callbacks ------------------------------------------------ //

class DoseServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pSrv) override {
        deviceConnected = true;
        Serial.println("[BLE] Client connected");
    }

    void onDisconnect(BLEServer* pSrv) override {
        deviceConnected = false;
        Serial.println("[BLE] Client disconnected");
        // Restart advertising so outer unit can reconnect
        pSrv->startAdvertising();
        Serial.println("[BLE] Advertising restarted");
    }
};

// ---- BLE setup ----------------------------------------------------------- //

static void setupBLE() {
    BLEDevice::init(BLE_DEVICE_NAME);

    pServer = BLEDevice::createServer();
    pServer->setCallbacks(new DoseServerCallbacks());

    BLEService* pService = pServer->createService(BLE_SERVICE_UUID);

    pDoseCharistic = pService->createCharacteristic(
        BLE_CHARACTERISTIC_UUID,
        BLECharacteristic::PROPERTY_READ |
        BLECharacteristic::PROPERTY_NOTIFY
    );

    // Add CCCD (Client Characteristic Configuration Descriptor) — required for
    // the central (outer unit) to enable notifications.
    pDoseCharistic->addDescriptor(new BLE2902());

    pService->start();

    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(BLE_SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);  // helps with iPhone connections
    BLEDevice::startAdvertising();

    Serial.printf("[BLE] Advertising as \"%s\"\n", BLE_DEVICE_NAME);
}

// ---- Task ---------------------------------------------------------------- //

void bleTransferTask(void* pvParams) {
    setupBLE();

    DoseEvent event;

    for (;;) {
        if (deviceConnected) {
            // Drain the dose queue and notify for every pending event
            while (xQueueReceive(doseEventQueue, &event, 0) == pdTRUE) {
                // Format: "dose,<units>" — e.g. "dose,6.0"
                char payload[32];
                snprintf(payload, sizeof(payload), "dose,%.1f", event.doseUnits);

                pDoseCharistic->setValue(payload);
                pDoseCharistic->notify();

                Serial.printf("[BLE] Notified: \"%s\"  (conf %.0f%%)\n",
                              payload, event.confidencePercent);

                vTaskDelay(pdMS_TO_TICKS(BLE_NOTIFY_INTERVAL_MS));
            }
        }

        // Short sleep when idle or not connected
        vTaskDelay(pdMS_TO_TICKS(50));
    }
}
