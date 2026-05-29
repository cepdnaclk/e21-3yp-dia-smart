#include <Arduino.h>
#include <BLEDevice.h>
#include <BLEClient.h>
#include <BLEScan.h>
#include <BLEAdvertisedDevice.h>
#include <BLERemoteCharacteristic.h>
#include <BLERemoteService.h>
#include <BLESecurity.h>
#include <esp_gap_ble_api.h>
#include <WiFi.h>
#include <time.h>
#include "include/system_queues.h"
#include "config/app_config.h"

// ---- Shared with event_aggregator_task ----------------------------------- //
volatile int g_lastBleRssi = 0;

// ---- BLE State machine --------------------------------------------------- //
enum BLEManagerState {
    BLE_SCANNING_PEN,
    BLE_CONNECTING_PEN,
    BLE_PEN_CONNECTED,
    BLE_GLUCOMETER_SYNC_START,
    BLE_GLUCOMETER_SYNCING,
    BLE_RECONNECTING_PEN
};

static BLEManagerState    state       = BLE_SCANNING_PEN;
static BLEAdvertisedDevice* penDevice        = nullptr;
static BLEAdvertisedDevice* glucometerDevice  = nullptr;
static BLEClient*           penClient        = nullptr;
static BLEClient*           glucometerClient = nullptr;
static bool penFound         = false;
static bool glucometerFound  = false;
static bool racpDone         = false;
static uint32_t glucSyncTimer = 0;

// ---- Helpers -------------------------------------------------------------- //
static void getTimestamp(char* buf, size_t len) {
    struct tm ti;
    if (getLocalTime(&ti)) {
        strftime(buf, len, "%Y-%m-%dT%H:%M:%SZ", &ti);
    } else {
        snprintf(buf, len, "1970-01-01T%08luZ", millis() / 1000);
    }
}

// ---- BLE scan callback ---------------------------------------------------- //
class MyAdvertisedDeviceCB : public BLEAdvertisedDeviceCallbacks {
    void onResult(BLEAdvertisedDevice advertisedDevice) override {
        if (state == BLE_SCANNING_PEN) {
            if (advertisedDevice.getName() == PEN_BLE_DEVICE_NAME) {
                penDevice = new BLEAdvertisedDevice(advertisedDevice);
                penFound  = true;
                BLEDevice::getScan()->stop();
                Serial.printf("[BLE] Pen found: %s\n",
                              advertisedDevice.getAddress().toString().c_str());
            }
        } else if (state == BLE_GLUCOMETER_SYNC_START) {
            if (advertisedDevice.haveServiceUUID() &&
                advertisedDevice.isAdvertisingService(
                    BLEUUID((uint16_t)GLUCOMETER_SERVICE_UUID))) {
                glucometerDevice = new BLEAdvertisedDevice(advertisedDevice);
                glucometerFound  = true;
                BLEDevice::getScan()->stop();
                Serial.printf("[BLE] Glucometer found: %s\n",
                              advertisedDevice.getAddress().toString().c_str());
            }
        }
    }
};

// ---- Pen notify callback -------------------------------------------------- //
// Pen sends "dose,<N.N>" over BLE notify. Parse and push to doseQueue.
static void onPenDoseNotify(BLERemoteCharacteristic* pChar,
                            uint8_t* pData, size_t length, bool isNotify) {
    if (length == 0) return;

    // Null-terminate safely
    char buf[64] = {};
    size_t copyLen = (length < sizeof(buf) - 1) ? length : sizeof(buf) - 1;
    memcpy(buf, pData, copyLen);

    if (strncmp(buf, "dose,", 5) != 0) return;

    float units = atof(buf + 5);
    if (units <= 0.0f) return;

    DoseReading dose = {};
    dose.doseUnits   = units;
    dose.angleDegrees = 0.0f;
    dose.timestampMs  = millis();
    getTimestamp(dose.injectedAt, sizeof(dose.injectedAt));

    if (xQueueSend(doseQueue, &dose, 0) != pdTRUE) {
        Serial.println("[BLE] doseQueue full — dose dropped");
    } else {
        Serial.printf("[BLE] Pen dose received: %.1f units\n", units);
    }
}

// ---- Glucometer measurement callback ------------------------------------- //
// Parses Glucose Measurement characteristic (0x2A18, Bluetooth GATT spec).
static void onGlucoseMeasNotify(BLERemoteCharacteristic* pChar,
                                uint8_t* pData, size_t length, bool isNotify) {
    if (length < 10) return;  // minimum valid length

    uint8_t  flags  = pData[0];
    uint16_t seqNum = (uint16_t)(pData[1] | (pData[2] << 8));

    // Byte layout after flags(1) + sequenceNumber(2) + baseTime(7):
    int offset = 10;
    if (flags & 0x01) offset += 2;  // time offset present (int16)

    GlucoseReading reading = {};
    reading.sequenceNumber = seqNum;
    reading.timestampMs    = millis();

    if ((flags & 0x02) && (int)length >= offset + 2) {
        // Glucose concentration as IEEE 11073 SFLOAT (16-bit)
        uint16_t sfloat   = (uint16_t)(pData[offset] | (pData[offset + 1] << 8));
        int16_t  mantissa = (int16_t)(sfloat & 0x0FFF);
        if (mantissa & 0x0800) mantissa |= (int16_t)0xF000;  // sign-extend 12→16
        int8_t   exponent = (int8_t)((uint8_t)(sfloat >> 12));

        float concentration = (float)mantissa;
        for (int i = 0; i < abs(exponent); i++) {
            concentration = (exponent > 0) ? concentration * 10.0f
                                           : concentration / 10.0f;
        }

        if (flags & 0x04) {
            // Units are mmol/L — convert to mg/dL (1 mmol/L = 18.0182 mg/dL)
            concentration *= 18.0182f;
        }
        reading.valueMgDl = (int)concentration;
    }

    if (xQueueSend(glucoseQueue, &reading, 0) != pdTRUE) {
        Serial.println("[BLE] glucoseQueue full — reading dropped");
    } else {
        Serial.printf("[BLE] Glucose: %d mg/dL (seq=%d)\n",
                      reading.valueMgDl, reading.sequenceNumber);
    }
}

// ---- RACP indication callback -------------------------------------------- //
// 0x2A52: Record Access Control Point.
// Success response: op=0x06, operator=0x00, req_op=0x01, resp=0x01
// The legacy bug was checking buffer[2] — the correct field is buffer[3].
static void onRacpIndicate(BLERemoteCharacteristic* pChar,
                           uint8_t* pData, size_t length, bool isNotify) {
    if (length >= 4 && pData[0] == 0x06 && pData[3] == 0x01) {
        racpDone = true;
        Serial.println("[BLE] RACP: all records fetched successfully");
    } else if (length >= 4) {
        Serial.printf("[BLE] RACP response: op=0x%02X resp=0x%02X\n",
                      pData[0], pData[3]);
    }
}

// ---- BLE security callbacks (glucometer pairing) ------------------------- //
class GlucometerSecurityCB : public BLESecurityCallbacks {
    bool     onConfirmPIN(uint32_t pin) override  { return true; }
    uint32_t onPassKeyRequest()        override   { return GLUCOMETER_BLE_PIN; }
    void     onPassKeyNotify(uint32_t)  override  {}
    bool     onSecurityRequest()       override   { return true; }
    void     onAuthenticationComplete(esp_ble_auth_cmpl_t cmpl) override {
        if (cmpl.success) Serial.println("[BLE] Glucometer paired successfully");
        else              Serial.println("[BLE] Glucometer pairing FAILED");
    }
};

// ---- Scan helpers -------------------------------------------------------- //
static BLEScan* setupScan(BLEAdvertisedDeviceCallbacks* cb) {
    BLEScan* pScan = BLEDevice::getScan();
    pScan->setAdvertisedDeviceCallbacks(cb, false);
    pScan->setActiveScan(true);
    pScan->setInterval(100);
    pScan->setWindow(99);
    return pScan;
}

// ---- Main BLE task -------------------------------------------------------- //
void bleManagerTask(void* pvParams) {
    BLEDevice::init("DiaSmart-Outer");

    MyAdvertisedDeviceCB* scanCb = new MyAdvertisedDeviceCB();
    BLEScan* pScan = setupScan(scanCb);

    state         = BLE_SCANNING_PEN;
    glucSyncTimer = millis();

    Serial.println("[BLE] Manager task started");

    for (;;) {
        switch (state) {

        // ------------------------------------------------------------------ //
        case BLE_SCANNING_PEN: {
            penFound = false;
            if (penDevice) { delete penDevice; penDevice = nullptr; }

            Serial.println("[BLE] Scanning for pen...");
            pScan->clearResults();
            pScan->start(5, false);           // blocks up to 5 s
            vTaskDelay(pdMS_TO_TICKS(500));   // let callback finish

            if (penFound) {
                state = BLE_CONNECTING_PEN;
            } else {
                Serial.println("[BLE] Pen not found, retrying in 2s...");
                vTaskDelay(pdMS_TO_TICKS(2000));
            }
            break;
        }

        // ------------------------------------------------------------------ //
        case BLE_CONNECTING_PEN: {
            if (penClient) {
                if (penClient->isConnected()) penClient->disconnect();
                delete penClient;
                penClient = nullptr;
            }

            penClient = BLEDevice::createClient();

            Serial.println("[BLE] Connecting to pen...");
            if (!penClient->connect(penDevice)) {
                Serial.println("[BLE] Pen connect failed");
                state = BLE_SCANNING_PEN;
                break;
            }

            BLERemoteService* penSvc =
                penClient->getService(BLEUUID(PEN_BLE_SERVICE_UUID));
            if (!penSvc) {
                Serial.println("[BLE] Pen service not found");
                penClient->disconnect();
                state = BLE_SCANNING_PEN;
                break;
            }

            BLERemoteCharacteristic* penChar =
                penSvc->getCharacteristic(BLEUUID(PEN_BLE_CHAR_UUID));
            if (!penChar || !penChar->canNotify()) {
                Serial.println("[BLE] Pen characteristic not found or no NOTIFY");
                penClient->disconnect();
                state = BLE_SCANNING_PEN;
                break;
            }

            penChar->registerForNotify(onPenDoseNotify);
            g_lastBleRssi = penClient->getRssi();
            Serial.printf("[BLE] Pen connected, RSSI=%d dBm\n", g_lastBleRssi);

            glucSyncTimer = millis();
            state = BLE_PEN_CONNECTED;
            break;
        }

        // ------------------------------------------------------------------ //
        case BLE_PEN_CONNECTED: {
            if (!penClient->isConnected()) {
                Serial.println("[BLE] Pen disconnected unexpectedly");
                state = BLE_SCANNING_PEN;
                break;
            }

            // Periodically update RSSI
            g_lastBleRssi = penClient->getRssi();

            // Trigger glucometer sync on interval
            if ((millis() - glucSyncTimer) >= GLUCOMETER_SYNC_INTERVAL_MS) {
                state = BLE_GLUCOMETER_SYNC_START;
                break;
            }

            vTaskDelay(pdMS_TO_TICKS(500));
            break;
        }

        // ------------------------------------------------------------------ //
        case BLE_GLUCOMETER_SYNC_START: {
            // Must disconnect pen before connecting glucometer
            if (penClient && penClient->isConnected()) {
                Serial.println("[BLE] Disconnecting pen for glucometer sync...");
                penClient->disconnect();
                vTaskDelay(pdMS_TO_TICKS(500));
            }

            // Set security params for PIN-based pairing
            BLEDevice::setEncryptionLevel(ESP_BLE_SEC_ENCRYPT);
            BLEDevice::setSecurityCallbacks(new GlucometerSecurityCB());
            esp_ble_auth_req_t auth   = ESP_LE_AUTH_REQ_SC_MITM_BOND;
            esp_ble_io_cap_t   iocap  = ESP_IO_CAP_IN;
            uint8_t keySize = 16;
            uint8_t initKey = ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK;
            uint8_t rspKey  = ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK;
            esp_ble_gap_set_security_param(ESP_BLE_SM_AUTHEN_REQ_MODE, &auth,    sizeof(auth));
            esp_ble_gap_set_security_param(ESP_BLE_SM_IOCAP_MODE,      &iocap,   sizeof(iocap));
            esp_ble_gap_set_security_param(ESP_BLE_SM_MAX_KEY_SIZE,    &keySize, sizeof(keySize));
            esp_ble_gap_set_security_param(ESP_BLE_SM_SET_INIT_KEY,    &initKey, sizeof(initKey));
            esp_ble_gap_set_security_param(ESP_BLE_SM_SET_RSP_KEY,     &rspKey,  sizeof(rspKey));

            glucometerFound = false;
            if (glucometerDevice) { delete glucometerDevice; glucometerDevice = nullptr; }

            Serial.println("[BLE] Scanning for glucometer...");
            pScan->clearResults();
            pScan->start(10, false);          // glucometer may take longer to appear
            vTaskDelay(pdMS_TO_TICKS(500));

            state = glucometerFound ? BLE_GLUCOMETER_SYNCING : BLE_RECONNECTING_PEN;
            if (!glucometerFound) Serial.println("[BLE] Glucometer not found, going back to pen");
            break;
        }

        // ------------------------------------------------------------------ //
        case BLE_GLUCOMETER_SYNCING: {
            if (glucometerClient) {
                if (glucometerClient->isConnected()) glucometerClient->disconnect();
                delete glucometerClient;
                glucometerClient = nullptr;
            }

            glucometerClient = BLEDevice::createClient();
            Serial.println("[BLE] Connecting to glucometer...");
            if (!glucometerClient->connect(glucometerDevice)) {
                Serial.println("[BLE] Glucometer connect failed");
                state = BLE_RECONNECTING_PEN;
                break;
            }

            // Wait for pairing to complete
            vTaskDelay(pdMS_TO_TICKS(3000));

            BLERemoteService* glucSvc =
                glucometerClient->getService(BLEUUID((uint16_t)GLUCOMETER_SERVICE_UUID));
            if (!glucSvc) {
                Serial.println("[BLE] Glucose service not found");
                glucometerClient->disconnect();
                state = BLE_RECONNECTING_PEN;
                break;
            }

            BLERemoteCharacteristic* measChar =
                glucSvc->getCharacteristic(BLEUUID((uint16_t)GLUCOMETER_MEAS_UUID));
            BLERemoteCharacteristic* racpChar =
                glucSvc->getCharacteristic(BLEUUID((uint16_t)GLUCOMETER_RACP_UUID));

            if (!measChar || !racpChar) {
                Serial.println("[BLE] Glucometer characteristics not found");
                glucometerClient->disconnect();
                state = BLE_RECONNECTING_PEN;
                break;
            }

            // Subscribe to measurement NOTIFY (0x2A18)
            measChar->registerForNotify(onGlucoseMeasNotify, true);
            // Subscribe to RACP INDICATE (0x2A52) — pass false for INDICATE mode
            racpChar->registerForNotify(onRacpIndicate, false);

            vTaskDelay(pdMS_TO_TICKS(500));

            // Send RACP "Report All Stored Records" (Op=0x01, Operator=0x01)
            racpDone = false;
            uint8_t racpCmd[2] = {0x01, 0x01};
            racpChar->writeValue(racpCmd, 2, true);  // with response
            Serial.println("[BLE] RACP: requested all records");

            // Wait up to 15 s for RACP completion indication
            uint32_t racpStart = millis();
            while (!racpDone && (millis() - racpStart) < 15000) {
                vTaskDelay(pdMS_TO_TICKS(200));
            }
            if (!racpDone) Serial.println("[BLE] RACP: timed out");

            glucometerClient->disconnect();
            vTaskDelay(pdMS_TO_TICKS(300));
            state = BLE_RECONNECTING_PEN;
            break;
        }

        // ------------------------------------------------------------------ //
        case BLE_RECONNECTING_PEN: {
            // Deinit and reinit BLE so the pen scan starts fresh
            Serial.println("[BLE] Reinitialising BLE for pen reconnect...");
            BLEDevice::deinit(true);
            vTaskDelay(pdMS_TO_TICKS(500));

            BLEDevice::init("DiaSmart-Outer");
            pScan = setupScan(scanCb);

            glucSyncTimer = millis();
            state = BLE_SCANNING_PEN;
            break;
        }

        } // end switch
    } // end for(;;)
}
