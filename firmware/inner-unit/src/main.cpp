#include <Arduino.h>
#include <freertos/FreeRTOS.h>
#include <freertos/task.h>

#include "config/app_config.h"
#include "services/wifi_provisioning_service.h"

// Forward declaration for task defined in tasks/.
void sensorSamplingTask(void* pvParams);

void setup() {
    Serial.begin(SERIAL_BAUD);
    delay(200);
    Serial.println("=== Dia-Smart Inner Unit Starting ===");

    // The provisioning service owns Wi-Fi credentials and radio channels.
    prepareInnerWifiChannel();

    if (!setupInnerWifiProvisioningService()) {
        Serial.println("[Main] Wi-Fi provisioning service failed - halting");
        while (true) {
            vTaskDelay(pdMS_TO_TICKS(1000));
        }
    }

    xTaskCreatePinnedToCore(
        sensorSamplingTask,
        "SensorSample",
        8192,
        nullptr,
        1,
        nullptr,
        1);

    Serial.println("[Main] Sensor task started");
}

void loop() {
    vTaskDelay(pdMS_TO_TICKS(10000));
}
