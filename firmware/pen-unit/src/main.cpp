#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/queue.h>
#include "config/app_config.h"
#include "models/dose_event.h"
#include "services/storage_service.h"

#if CONFIG_FREERTOS_UNICORE
static constexpr BaseType_t DOSE_TASK_CORE = 0;
static constexpr BaseType_t BLE_TASK_CORE = 0;
#else
static constexpr BaseType_t DOSE_TASK_CORE = 1;
static constexpr BaseType_t BLE_TASK_CORE = 0;
#endif

// Forward declarations for tasks defined in tasks/
void doseDetectionTask(void* pvParams);
void bleTransferTask(void* pvParams);

// Shared queue: doseDetectionTask → bleTransferTask
QueueHandle_t doseEventQueue = nullptr;
PenDoseStorageService doseStorage;

void setup() {
    Serial.begin(SERIAL_BAUD);
    delay(200);
    Serial.println("=== Dia-Smart Pen Unit Starting ===");

    if (!doseStorage.begin()) {
        Serial.println("[FATAL] Failed to initialise dose storage - halting");
        while (true) { delay(1000); }
    }
    Serial.printf("[Main] Dose storage ready: %u pending / %u capacity\n",
                  doseStorage.countByStatus(DOSE_RECORD_PENDING),
                  doseStorage.capacity());

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
        DOSE_TASK_CORE
    );

    // BLE GATT server on Core 0 (Bluetooth stack lives on Core 0)
    xTaskCreatePinnedToCore(
        bleTransferTask,
        "BLETransfer",
        STACK_BLE_TRANSFER,
        nullptr,
        1,
        nullptr,
        BLE_TASK_CORE
    );

    Serial.println("[Main] FreeRTOS tasks started");
}

// FreeRTOS takes over — loop() intentionally idle.
void loop() {
    vTaskDelay(pdMS_TO_TICKS(10000));
}
