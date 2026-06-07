#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include <stdio.h>
#include <string.h>
#include <string>
#include "../config/app_config.h"
#include "../models/dose_event.h"
#include "../models/persistent_dose_record.h"
#include "../services/storage_service.h"

extern QueueHandle_t doseEventQueue;
extern PenDoseStorageService doseStorage;
extern bool doseStorageReady;

// ---- BLE connection state (set from server callbacks) -------------------- //
static volatile bool deviceConnected = false;
static volatile bool timeSyncReady = false;
static volatile bool connectionTimeSyncReady = false;
static uint32_t timeSyncEpochSec = 0;
static uint32_t timeSyncMillis = 0;
static uint32_t lastTimeSyncWaitLogMs = 0;

static BLEServer* pServer = nullptr;
static BLECharacteristic* pDoseCharistic = nullptr;

static bool parseTimeSyncPayload(const char* payload, uint32_t* epochSec) {
    if (payload == nullptr || epochSec == nullptr || strncmp(payload, "t,", 2) != 0) {
        return false;
    }

    unsigned long parsedEpoch = 0;
    if (sscanf(payload, "t,%lu", &parsedEpoch) != 1 || parsedEpoch < 1700000000UL) {
        return false;
    }

    *epochSec = (uint32_t)parsedEpoch;
    return true;
}

static uint32_t calculateTakenEpochSec(uint32_t doseTakenMillis) {
    int32_t deltaMs = (int32_t)(doseTakenMillis - timeSyncMillis);
    int64_t takenEpoch = (int64_t)timeSyncEpochSec + (deltaMs / 1000);
    if (takenEpoch < 0) {
        return 0;
    }
    return (uint32_t)takenEpoch;
}

static void drainDoseQueueSignals() {
    DoseEvent ignored;
    while (xQueueReceive(doseEventQueue, &ignored, 0) == pdTRUE) {
        Serial.println("[BLE] Dose queue signal received; stored record will be sent");
    }
}

static bool notifyStoredDose(uint8_t index, const PersistentDoseRecord& record) {
    uint32_t takenEpochSec = calculateTakenEpochSec(record.timing.sourceTimestampMs);
    int doseTenths = (int)(record.doseUnits * 10.0f + 0.5f);
    char payload[28];
    snprintf(payload, sizeof(payload), "d,%u,%d,%lu",
             index,
             doseTenths,
             (unsigned long)takenEpochSec);

    pDoseCharistic->setValue(payload);
    pDoseCharistic->notify();

    Serial.printf("[BLE] Notified stored dose slot %u: \"%s\" (%.1f units, epoch %lu, conf %.0f%%)\n",
                  index,
                  payload,
                  record.doseUnits,
                  (unsigned long)takenEpochSec,
                  record.confidencePercent);

    if (!doseStorage.updateStatus(index, DOSE_RECORD_SENT)) {
        Serial.printf("[BLE] WARNING: failed to mark slot %u as sent\n", index);
        return false;
    }

    vTaskDelay(pdMS_TO_TICKS(BLE_NOTIFY_INTERVAL_MS));
    return true;
}

static void notifyPendingStoredDoses() {
    if (!doseStorageReady) {
        return;
    }

    if (!timeSyncReady || !connectionTimeSyncReady) {
        uint32_t nowMs = millis();
        if ((nowMs - lastTimeSyncWaitLogMs) >= 2000UL) {
            Serial.println("[BLE] Waiting for outer time sync before sending stored doses");
            lastTimeSyncWaitLogMs = nowMs;
        }
        return;
    }

    for (uint8_t i = 0; i < doseStorage.capacity(); ++i) {
        if (!deviceConnected) {
            return;
        }

        PersistentDoseRecord record = {};
        if (doseStorage.read(i, &record) && record.status == DOSE_RECORD_PENDING) {
            notifyStoredDose(i, record);
        }
    }
}

// ---- BLE Server callbacks ------------------------------------------------ //

class DoseCharacteristicCallbacks : public BLECharacteristicCallbacks {
    void onWrite(BLECharacteristic* characteristic) override {
        std::string value = characteristic->getValue();
        if (value.empty()) {
            return;
        }

        char payload[32] = {};
        size_t copyLen = (value.length() < sizeof(payload) - 1) ? value.length() : sizeof(payload) - 1;
        memcpy(payload, value.data(), copyLen);

        uint32_t epochSec = 0;
        if (!parseTimeSyncPayload(payload, &epochSec)) {
            Serial.printf("[BLE] Ignored write payload: \"%s\"\n", payload);
            return;
        }

        timeSyncEpochSec = epochSec;
        timeSyncMillis = millis();
        timeSyncReady = true;
        connectionTimeSyncReady = true;
        Serial.printf("[BLE] Time sync received: epoch=%lu at penMillis=%lu\n",
                      (unsigned long)timeSyncEpochSec,
                      (unsigned long)timeSyncMillis);
    }
};

class DoseServerCallbacks : public BLEServerCallbacks {
    void onConnect(BLEServer* pSrv) override {
        (void)pSrv;
        deviceConnected = true;
        connectionTimeSyncReady = false;
        Serial.println("[BLE] Client connected");
    }

    void onDisconnect(BLEServer* pSrv) override {
        deviceConnected = false;
        Serial.println("[BLE] Client disconnected");
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
        BLECharacteristic::PROPERTY_NOTIFY |
        BLECharacteristic::PROPERTY_WRITE
    );

    pDoseCharistic->setCallbacks(new DoseCharacteristicCallbacks());
    pDoseCharistic->addDescriptor(new BLE2902());

    pService->start();

    BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
    pAdvertising->addServiceUUID(BLE_SERVICE_UUID);
    pAdvertising->setScanResponse(true);
    pAdvertising->setMinPreferred(0x06);
    BLEDevice::startAdvertising();

    Serial.printf("[BLE] Advertising as \"%s\"\n", BLE_DEVICE_NAME);
}

// ---- Task ---------------------------------------------------------------- //

void bleTransferTask(void* pvParams) {
    (void)pvParams;
    setupBLE();

    for (;;) {
        if (deviceConnected) {
            drainDoseQueueSignals();
            notifyPendingStoredDoses();
        }

        vTaskDelay(pdMS_TO_TICKS(50));
    }
}
