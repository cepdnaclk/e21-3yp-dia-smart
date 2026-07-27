#include <Arduino.h>
#include <BLEAdvertisedDevice.h>
#include <BLEClient.h>
#include <BLEDevice.h>
#include <BLERemoteCharacteristic.h>
#include <BLERemoteService.h>
#include <BLEScan.h>
#include <BLESecurity.h>
#include <esp_gap_ble_api.h>
#include <time.h>

#include "config/app_config.h"
#include "include/system_queues.h"
#include "../../../common/utils/glucose_time_utils.h"

// Shared with event_aggregator_task. This remains the pen RSSI because the
// existing display uses it as the pen-ready signal.
volatile int g_lastBleRssi = 0;

namespace {
BLEAdvertisedDevice* penDevice = nullptr;
BLEAdvertisedDevice* glucometerDevice = nullptr;
BLEClient* penClient = nullptr;
BLEClient* glucometerClient = nullptr;
BLERemoteCharacteristic* penDoseChar = nullptr;
BLERemoteCharacteristic* glucometerMeasureChar = nullptr;
BLERemoteCharacteristic* glucometerRacpChar = nullptr;

volatile bool penFound = false;
volatile bool glucometerFound = false;
bool penConnected = false;
bool glucometerConnected = false;
volatile bool racpDone = false;
volatile bool racpRequestInFlight = false;

uint32_t lastScanMs = 0;
uint32_t nextPenConnectMs = 0;
uint32_t nextGlucometerConnectMs = 0;
uint32_t lastPenRssiMs = 0;
uint32_t lastRacpRequestMs = 0;

volatile uint16_t pendingPenAckMask = 0;
bool hasLastGlucoseSeq = false;
uint16_t lastAcceptedGlucoseSeq = 0;

void getTimestamp(char* output, size_t outputLength) {
    tm timeInfo = {};
    if (getLocalTime(&timeInfo)) {
        strftime(output, outputLength, "%Y-%m-%dT%H:%M:%SZ", &timeInfo);
    } else {
        snprintf(output,
                 outputLength,
                 "1970-01-01T%08luZ",
                 (unsigned long)(millis() / 1000));
    }
}

bool getCurrentEpochSec(uint32_t* epochSec) {
    if (epochSec == nullptr) {
        return false;
    }
    time_t now = time(nullptr);
    if (now <= 1700000000) {
        return false;
    }
    *epochSec = (uint32_t)now;
    return true;
}

void getTimestampFromEpoch(char* output,
                           size_t outputLength,
                           uint32_t epochSec) {
    time_t timestamp = (time_t)epochSec;
    tm timeInfo = {};
    gmtime_r(&timestamp, &timeInfo);
    strftime(output, outputLength, "%Y-%m-%dT%H:%M:%SZ", &timeInfo);
}

void writePenTimeSync(BLERemoteCharacteristic* characteristic) {
    if (characteristic == nullptr || !characteristic->canWrite()) {
        Serial.println("[BLE] Pen characteristic cannot accept time sync");
        return;
    }

    uint32_t epochSec = 0;
    if (!getCurrentEpochSec(&epochSec)) {
        Serial.println("[BLE] Pen time sync skipped; NTP is not ready");
        return;
    }

    char payload[24];
    snprintf(payload, sizeof(payload), "t,%lu", (unsigned long)epochSec);
    characteristic->writeValue(
        (uint8_t*)payload, strlen(payload), true);
    Serial.printf("[BLE] Pen time sync sent: %s\n", payload);
}

void queuePenAck(uint8_t slot) {
    if (slot < 16) {
        pendingPenAckMask |= (uint16_t)(1U << slot);
    }
}

void sendPendingPenAcks() {
    if (!penConnected ||
        penDoseChar == nullptr ||
        !penDoseChar->canWrite()) {
        return;
    }

    uint16_t mask = pendingPenAckMask;
    for (uint8_t slot = 0; slot < 16; ++slot) {
        uint16_t bit = (uint16_t)(1U << slot);
        if ((mask & bit) == 0) {
            continue;
        }

        char payload[8];
        snprintf(payload, sizeof(payload), "a,%u", slot);
        penDoseChar->writeValue(
            (uint8_t*)payload, strlen(payload), true);
        pendingPenAckMask &= (uint16_t)~bit;
        Serial.printf("[BLE] Pen record %u acknowledged\n", slot);
    }
}

bool parseCompactDosePayload(const char* payload, DoseReading* dose) {
    if (payload == nullptr ||
        dose == nullptr ||
        strncmp(payload, "d,", 2) != 0) {
        return false;
    }

    int slot = -1;
    int doseTenths = 0;
    unsigned long takenEpochSec = 0;
    if (sscanf(payload,
               "d,%d,%d,%lu",
               &slot,
               &doseTenths,
               &takenEpochSec) != 3 ||
        slot < 0 ||
        doseTenths <= 0 ||
        takenEpochSec < 1700000000UL) {
        return false;
    }

    dose->doseUnits = doseTenths / 10.0f;
    dose->angleDegrees = 0.0f;
    dose->timestampMs = millis();
    dose->penRecordSlot = (uint8_t)slot;
    dose->penTakenEpochSec = (uint32_t)takenEpochSec;
    dose->hasPenTakenEpoch = true;
    getTimestampFromEpoch(
        dose->injectedAt, sizeof(dose->injectedAt), dose->penTakenEpochSec);
    return true;
}

bool parseLegacyDosePayload(const char* payload, DoseReading* dose) {
    if (payload == nullptr ||
        dose == nullptr ||
        strncmp(payload, "dose,", 5) != 0) {
        return false;
    }

    float units = atof(payload + 5);
    if (units <= 0.0f) {
        return false;
    }

    dose->doseUnits = units;
    dose->angleDegrees = 0.0f;
    dose->timestampMs = millis();
    dose->penRecordSlot = 0xFF;
    dose->penTakenEpochSec = 0;
    dose->hasPenTakenEpoch = false;
    getTimestamp(dose->injectedAt, sizeof(dose->injectedAt));
    return true;
}

void onPenDoseNotify(BLERemoteCharacteristic* characteristic,
                     uint8_t* data,
                     size_t length,
                     bool isNotify) {
    (void)characteristic;
    (void)isNotify;
    if (length == 0) {
        return;
    }

    char payload[64] = {};
    size_t copyLength =
        length < sizeof(payload) - 1 ? length : sizeof(payload) - 1;
    memcpy(payload, data, copyLength);

    DoseReading dose = {};
    if (!parseCompactDosePayload(payload, &dose) &&
        !parseLegacyDosePayload(payload, &dose)) {
        return;
    }

    if (xQueueSend(doseQueue, &dose, 0) != pdTRUE) {
        Serial.println("[BLE] doseQueue full; dose dropped");
        return;
    }

    Serial.printf("[BLE] Pen dose received: %.1fU at %s\n",
                  dose.doseUnits,
                  dose.injectedAt);
    if (dose.hasPenTakenEpoch) {
        queuePenAck(dose.penRecordSlot);
    }
}

void onGlucoseMeasurementNotify(BLERemoteCharacteristic* characteristic,
                                uint8_t* data,
                                size_t length,
                                bool isNotify) {
    (void)characteristic;
    (void)isNotify;
    if (length < 10) {
        Serial.printf("[BLE] Short glucose measurement ignored: %u bytes\n",
                      (unsigned)length);
        return;
    }

    const uint8_t flags = data[0];
    const size_t glucoseOffset =
        glucose_time::concentrationOffset(flags);
    if ((flags & 0x02U) == 0 || length < glucoseOffset + 2) {
        Serial.println("[BLE] Glucose concentration missing");
        return;
    }

    uint16_t sequenceNumber =
        (uint16_t)(data[1] | ((uint16_t)data[2] << 8));
    uint16_t rawGlucose =
        (uint16_t)(data[glucoseOffset] |
                   ((uint16_t)data[glucoseOffset + 1] << 8));
    int glucoseMgDl = (int)(rawGlucose & 0x0FFF);

    if (hasLastGlucoseSeq &&
        sequenceNumber == lastAcceptedGlucoseSeq) {
        Serial.printf("[BLE] Duplicate glucose seq=%u ignored\n",
                      sequenceNumber);
        return;
    }

    GlucoseReading reading = {};
    reading.sequenceNumber = sequenceNumber;
    reading.valueMgDl = glucoseMgDl;
    reading.timestampMs = millis();
    reading.hasMeasuredAt = glucose_time::formatMeasuredAt(
        data,
        length,
        GLUCOMETER_UTC_OFFSET_MINUTES,
        reading.measuredAt,
        sizeof(reading.measuredAt));
    if (xQueueSend(glucoseQueue, &reading, 0) != pdTRUE) {
        Serial.println("[BLE] glucoseQueue full; reading dropped");
        return;
    }

    lastAcceptedGlucoseSeq = sequenceNumber;
    hasLastGlucoseSeq = true;
    Serial.printf("[BLE] Glucose queued immediately: %d mg/dL seq=%u measuredAt=%s\n",
                  glucoseMgDl,
                  sequenceNumber,
                  reading.hasMeasuredAt ? reading.measuredAt : "event-time-fallback");
}

void onRacpIndicate(BLERemoteCharacteristic* characteristic,
                    uint8_t* data,
                    size_t length,
                    bool isNotify) {
    (void)characteristic;
    (void)isNotify;
    if (length < 4 || data[0] != 0x06) {
        return;
    }

    racpDone = true;
    racpRequestInFlight = false;
    if (data[3] == 0x01) {
        Serial.println("[BLE] RACP latest-record request completed");
    } else {
        Serial.printf("[BLE] RACP completed with response=0x%02X\n",
                      data[3]);
    }
}

class GlucometerSecurityCallbacks : public BLESecurityCallbacks {
public:
    bool onConfirmPIN(uint32_t pin) override {
        (void)pin;
        return true;
    }

    uint32_t onPassKeyRequest() override {
        return GLUCOMETER_BLE_PIN;
    }

    void onPassKeyNotify(uint32_t pin) override {
        (void)pin;
    }

    bool onSecurityRequest() override {
        return true;
    }

    void onAuthenticationComplete(esp_ble_auth_cmpl_t result) override {
        Serial.println(result.success
            ? "[BLE] Glucometer authentication succeeded"
            : "[BLE] Glucometer authentication failed");
    }
};

class DiscoveryCallbacks : public BLEAdvertisedDeviceCallbacks {
public:
    void onResult(BLEAdvertisedDevice advertisedDevice) override {
        bool foundTarget = false;

        if (!penConnected &&
            !penFound &&
            advertisedDevice.getName() == PEN_BLE_DEVICE_NAME) {
            penDevice = new BLEAdvertisedDevice(advertisedDevice);
            penFound = true;
            foundTarget = true;
            Serial.printf("[BLE] Pen discovered: %s\n",
                          advertisedDevice.getAddress().toString().c_str());
        }

        if (!glucometerConnected &&
            !glucometerFound &&
            advertisedDevice.haveServiceUUID() &&
            advertisedDevice.isAdvertisingService(
                BLEUUID((uint16_t)GLUCOMETER_SERVICE_UUID))) {
            glucometerDevice =
                new BLEAdvertisedDevice(advertisedDevice);
            glucometerFound = true;
            foundTarget = true;
            Serial.printf("[BLE] Glucometer discovered: %s\n",
                          advertisedDevice.getAddress().toString().c_str());
        }

        if (foundTarget) {
            BLEDevice::getScan()->stop();
        }
    }
};

BLEScan* setupScan(BLEAdvertisedDeviceCallbacks* callbacks) {
    BLEScan* scan = BLEDevice::getScan();
    scan->setAdvertisedDeviceCallbacks(callbacks, false);
    scan->setActiveScan(true);
    scan->setInterval(100);
    scan->setWindow(99);
    return scan;
}

void clearPenClient() {
    penConnected = false;
    penDoseChar = nullptr;
    g_lastBleRssi = 0;
    if (penClient == nullptr) {
        return;
    }
    if (penClient->isConnected()) {
        penClient->disconnect();
        vTaskDelay(pdMS_TO_TICKS(200));
    }
    delete penClient;
    penClient = nullptr;
}

void clearGlucometerClient() {
    glucometerConnected = false;
    glucometerMeasureChar = nullptr;
    glucometerRacpChar = nullptr;
    racpDone = false;
    racpRequestInFlight = false;
    if (glucometerClient == nullptr) {
        return;
    }
    if (glucometerClient->isConnected()) {
        glucometerClient->disconnect();
        vTaskDelay(pdMS_TO_TICKS(200));
    }
    delete glucometerClient;
    glucometerClient = nullptr;
}

bool requestLatestGlucometerRecord() {
    if (!glucometerConnected ||
        glucometerClient == nullptr ||
        !glucometerClient->isConnected() ||
        glucometerRacpChar == nullptr ||
        !glucometerRacpChar->canWrite()) {
        return false;
    }

    uint8_t command[2] = {0x01, 0x06};
    racpDone = false;
    racpRequestInFlight = true;
    lastRacpRequestMs = millis();
    glucometerRacpChar->writeValue(command, sizeof(command), true);
    Serial.println("[BLE] RACP latest-record request sent");
    return true;
}

bool connectPen() {
    if (penDevice == nullptr) {
        return false;
    }

    clearPenClient();
    penClient = BLEDevice::createClient();
    Serial.println("[BLE] Connecting pen without dropping glucometer...");
    if (!penClient->connect(penDevice)) {
        Serial.println("[BLE] Pen connect failed");
        clearPenClient();
        return false;
    }

    BLERemoteService* service =
        penClient->getService(BLEUUID(PEN_BLE_SERVICE_UUID));
    if (service == nullptr) {
        Serial.println("[BLE] Pen service not found");
        clearPenClient();
        return false;
    }

    penDoseChar =
        service->getCharacteristic(BLEUUID(PEN_BLE_CHAR_UUID));
    if (penDoseChar == nullptr || !penDoseChar->canNotify()) {
        Serial.println("[BLE] Pen notification characteristic unavailable");
        clearPenClient();
        return false;
    }

    penDoseChar->registerForNotify(onPenDoseNotify);
    writePenTimeSync(penDoseChar);
    penConnected = true;
    g_lastBleRssi = penClient->getRssi();
    lastPenRssiMs = millis();
    sendPendingPenAcks();
    Serial.printf("[BLE] Pen connected in parallel, RSSI=%d dBm\n",
                  g_lastBleRssi);
    return true;
}

bool connectGlucometer() {
    if (glucometerDevice == nullptr) {
        return false;
    }

    clearGlucometerClient();
    glucometerClient = BLEDevice::createClient();
    Serial.println("[BLE] Connecting glucometer without dropping pen...");
    if (!glucometerClient->connect(glucometerDevice)) {
        Serial.println("[BLE] Glucometer connect failed");
        clearGlucometerClient();
        return false;
    }

    esp_ble_set_encryption(*glucometerDevice->getAddress().getNative(),
                           ESP_BLE_SEC_ENCRYPT_MITM);
    vTaskDelay(pdMS_TO_TICKS(4000));

    esp_ble_gattc_cache_refresh(
        *glucometerDevice->getAddress().getNative());
    vTaskDelay(pdMS_TO_TICKS(500));
    glucometerClient->getServices();
    vTaskDelay(pdMS_TO_TICKS(2000));

    BLERemoteService* service = glucometerClient->getService(
        BLEUUID((uint16_t)GLUCOMETER_SERVICE_UUID));
    if (service == nullptr) {
        Serial.println("[BLE] Glucose service not found");
        clearGlucometerClient();
        return false;
    }

    glucometerMeasureChar = service->getCharacteristic(
        BLEUUID((uint16_t)GLUCOMETER_MEAS_UUID));
    glucometerRacpChar = service->getCharacteristic(
        BLEUUID((uint16_t)GLUCOMETER_RACP_UUID));
    if (glucometerMeasureChar == nullptr ||
        glucometerRacpChar == nullptr ||
        !glucometerMeasureChar->canNotify()) {
        Serial.println("[BLE] Glucometer characteristics unavailable");
        clearGlucometerClient();
        return false;
    }

    glucometerMeasureChar->registerForNotify(
        onGlucoseMeasurementNotify, true);
    glucometerRacpChar->registerForNotify(onRacpIndicate, false);
    glucometerConnected = true;
    vTaskDelay(pdMS_TO_TICKS(1000));
    requestLatestGlucometerRecord();
    Serial.println("[BLE] Glucometer connected in parallel");
    return true;
}

void scanForMissingDevices(BLEScan* scan) {
    if ((penConnected || penFound) &&
        (glucometerConnected || glucometerFound)) {
        return;
    }

    Serial.printf("[BLE] Scanning for%s%s\n",
                  (!penConnected && !penFound) ? " pen" : "",
                  (!glucometerConnected && !glucometerFound)
                      ? " glucometer"
                      : "");
    scan->clearResults();
    scan->start(PEN_SCAN_WINDOW_SEC, false);
    scan->clearResults();
    lastScanMs = millis();
}
}  // namespace

void bleManagerTask(void* parameter) {
    (void)parameter;
    BLEDevice::init("DiaSmart-Outer");

    BLEDevice::setSecurityCallbacks(
        new GlucometerSecurityCallbacks());
    BLESecurity* security = new BLESecurity();
    security->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);
    security->setCapability(ESP_IO_CAP_IN);
    security->setInitEncryptionKey(
        ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
    security->setRespEncryptionKey(
        ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);

    DiscoveryCallbacks* discoveryCallbacks = new DiscoveryCallbacks();
    BLEScan* scan = setupScan(discoveryCallbacks);
    lastScanMs = millis() - PEN_SCAN_IDLE_DELAY_MS;

    Serial.println("[BLE] Parallel client manager started");

    for (;;) {
        uint32_t now = millis();

        if (penConnected &&
            (penClient == nullptr || !penClient->isConnected())) {
            Serial.println("[BLE] Pen disconnected; reconnect scheduled");
            clearPenClient();
            penFound = false;
            nextPenConnectMs = now + PEN_SCAN_IDLE_DELAY_MS;
        }

        if (glucometerConnected &&
            (glucometerClient == nullptr ||
             !glucometerClient->isConnected())) {
            Serial.println(
                "[BLE] Glucometer disconnected; reconnect scheduled");
            clearGlucometerClient();
            glucometerFound = false;
            nextGlucometerConnectMs = now + PEN_SCAN_IDLE_DELAY_MS;
        }

        if (!penConnected &&
            penFound &&
            (int32_t)(now - nextPenConnectMs) >= 0) {
            if (!connectPen()) {
                nextPenConnectMs =
                    millis() + PEN_SCAN_IDLE_DELAY_MS;
            }
            penFound = false;
            delete penDevice;
            penDevice = nullptr;
        }

        if (!glucometerConnected &&
            glucometerFound &&
            (int32_t)(now - nextGlucometerConnectMs) >= 0) {
            if (!connectGlucometer()) {
                nextGlucometerConnectMs =
                    millis() + PEN_SCAN_IDLE_DELAY_MS;
            }
            glucometerFound = false;
            delete glucometerDevice;
            glucometerDevice = nullptr;
        }

        if (penConnected) {
            sendPendingPenAcks();
            if ((now - lastPenRssiMs) >= 5000) {
                g_lastBleRssi = penClient->getRssi();
                lastPenRssiMs = now;
            }
        }

        if (glucometerConnected) {
            if (racpRequestInFlight &&
                (now - lastRacpRequestMs) >=
                    GLUCOMETER_RACP_TIMEOUT_MS) {
                racpRequestInFlight = false;
                racpDone = false;
                Serial.println(
                    "[BLE] RACP request timed out; live retry scheduled");
            }

            if (!racpRequestInFlight &&
                (now - lastRacpRequestMs) >=
                    GLUCOMETER_LIVE_SYNC_INTERVAL_MS) {
                requestLatestGlucometerRecord();
            }
        }

        if ((!penConnected || !glucometerConnected) &&
            !penFound &&
            !glucometerFound &&
            (now - lastScanMs) >= PEN_SCAN_IDLE_DELAY_MS) {
            scanForMissingDevices(scan);
        }

        vTaskDelay(pdMS_TO_TICKS(200));
    }
}
