#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include "config/app_config.h"
#include "models/dose_event.h"
#include "services/storage_service.h"

// Forward declarations for tasks defined in tasks/
void doseDetectionTask(void* pvParams);
void bleTransferTask(void* pvParams);

// Shared queue: doseDetectionTask → bleTransferTask
QueueHandle_t doseEventQueue = nullptr;
PenDoseStorageService doseStorage;
bool doseStorageReady = false;

void setup() {
    Serial.begin(SERIAL_BAUD);
    delay(200);
    Serial.println("=== Dia-Smart Pen Unit Starting ===");

    doseStorageReady = doseStorage.begin();
    if (doseStorageReady) {
        Serial.printf("[Main] Dose storage ready: %u pending / %u capacity\n",
                      doseStorage.countByStatus(DOSE_RECORD_PENDING),
                      doseStorage.capacity());
    } else {
        Serial.println("[Main] ERROR: dose storage unavailable; confirmed doses will not be queued");
    }

    // Create shared queue before starting tasks
    doseEventQueue = xQueueCreate(DOSE_QUEUE_LENGTH, sizeof(DoseEvent));
    if (doseEventQueue == nullptr) {
        Serial.println("[FATAL] Failed to create doseEventQueue — halting");
        while (true) { delay(1000); }
    }

    // Dose detection on Core 1 (sensor-heavy, avoids BLE Core 0 contention)
    xTaskCreatePinnedToCore(
        doseDetectionTask,
        "DoseDetect",
        STACK_DOSE_DETECT,
        nullptr,
        2,          // higher priority so button presses are never missed
        nullptr,
        1           // Core 1
    );

    // BLE GATT server on Core 0 (Bluetooth stack lives on Core 0)
    xTaskCreatePinnedToCore(
        bleTransferTask,
        "BLETransfer",
        STACK_BLE_TRANSFER,
        nullptr,
        1,
        nullptr,
        0           // Core 0
    );

    Serial.println("[Main] FreeRTOS tasks started");
}

// FreeRTOS takes over — loop() intentionally idle.
void loop() {
    vTaskDelay(pdMS_TO_TICKS(10000));
}
