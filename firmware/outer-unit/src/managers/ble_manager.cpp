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
static BLERemoteCharacteristic* penDoseChar  = nullptr;
static bool penFound         = false;
static bool glucometerFound  = false;
static bool racpDone         = false;
static uint32_t glucSyncTimer = 0;
static volatile uint16_t pendingPenAckMask = 0;

// ---- Helpers -------------------------------------------------------------- //
static void getTimestamp(char* buf, size_t len) {
    struct tm ti;
    if (getLocalTime(&ti)) {
        strftime(buf, len, "%Y-%m-%dT%H:%M:%SZ", &ti);
    } else {
        snprintf(buf, len, "1970-01-01T%08luZ", millis() / 1000);
    }
}

static bool getCurrentEpochSec(uint32_t* epochSec) {
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

static void getTimestampFromEpoch(char* buf, size_t len, uint32_t epochSec) {
    time_t injected = (time_t)epochSec;
    struct tm ti;
    gmtime_r(&injected, &ti);
    strftime(buf, len, "%Y-%m-%dT%H:%M:%SZ", &ti);
}

static void writePenTimeSync(BLERemoteCharacteristic* penChar) {
    if (penChar == nullptr || !penChar->canWrite()) {
        Serial.println("[BLE] Pen characteristic does not support time sync write");
        return;
    }

    uint32_t epochSec = 0;
    if (!getCurrentEpochSec(&epochSec)) {
        Serial.println("[BLE] Time sync skipped; outer NTP time is not ready");
        return;
    }

    char payload[24];
    snprintf(payload, sizeof(payload), "t,%lu", (unsigned long)epochSec);
    penChar->writeValue((uint8_t*)payload, strlen(payload), true);
    Serial.printf("[BLE] Time sync sent to pen: %s\n", payload);
}

static void queuePenAck(uint8_t slot) {
    if (slot >= 16) {
        return;
    }
    pendingPenAckMask |= (uint16_t)(1U << slot);
}

static void sendPendingPenAcks() {
    if (penDoseChar == nullptr || !penDoseChar->canWrite()) {
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
        penDoseChar->writeValue((uint8_t*)payload, strlen(payload), true);
        pendingPenAckMask &= (uint16_t)~bit;
        Serial.printf("[BLE] ACK sent to pen for slot %u\n", slot);
    }
}

static bool parseCompactDosePayload(const char* buf, DoseReading* dose) {
    if (buf == nullptr || dose == nullptr || strncmp(buf, "d,", 2) != 0) {
        return false;
    }

    int slot = -1;
    int doseTenths = 0;
    unsigned long takenEpochSec = 0;
    if (sscanf(buf, "d,%d,%d,%lu", &slot, &doseTenths, &takenEpochSec) != 3) {
        return false;
    }

    if (slot < 0 || doseTenths <= 0 || takenEpochSec < 1700000000UL) {
        return false;
    }

    dose->doseUnits = doseTenths / 10.0f;
    dose->angleDegrees = 0.0f;
    dose->timestampMs = millis();
    dose->penRecordSlot = (uint8_t)slot;
    dose->penTakenEpochSec = (uint32_t)takenEpochSec;
    dose->hasPenTakenEpoch = true;
    getTimestampFromEpoch(dose->injectedAt, sizeof(dose->injectedAt), dose->penTakenEpochSec);
    return true;
}

static bool parseLegacyDosePayload(const char* buf, DoseReading* dose) {
    if (buf == nullptr || dose == nullptr || strncmp(buf, "dose,", 5) != 0) {
        return false;
    }

    float units = atof(buf + 5);
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
// Pen sends either legacy "dose,<N.N>" or compact "d,<slot>,<tenths>,<takenEpochSec>".
static void onPenDoseNotify(BLERemoteCharacteristic* pChar,
                            uint8_t* pData, size_t length, bool isNotify) {
    if (length == 0) return;

    // Null-terminate safely
    char buf[64] = {};
    size_t copyLen = (length < sizeof(buf) - 1) ? length : sizeof(buf) - 1;
    memcpy(buf, pData, copyLen);

    DoseReading dose = {};
    if (!parseCompactDosePayload(buf, &dose) && !parseLegacyDosePayload(buf, &dose)) {
        return;
    }

    if (xQueueSend(doseQueue, &dose, 0) != pdTRUE) {
        Serial.println("[BLE] doseQueue full — dose dropped");
    } else {
        Serial.printf("[BLE] Pen dose received: %.1f units injectedAt=%s\n",
                      dose.doseUnits,
                      dose.injectedAt);
        if (dose.hasPenTakenEpoch) {
            queuePenAck(dose.penRecordSlot);
        }
    }
}

// ---- Glucometer measurement callback ------------------------------------ //
// Parses Glucose Measurement characteristic (0x2A18).
// Accu-Chek Guide Me byte layout (verified against legacy working firmware):
//   [0]        flags
//   [1..2]     sequence number (uint16 LE)
//   [3..4]     year (uint16 LE)
//   [5]        month
//   [6]        day
//   [7]        hours
//   [8]        minutes
//   [9]        seconds
//   [10..11]   time offset (int16 LE) — present only if flags & 0x01
//   [12..13]   glucose as raw uint16 LE; mantissa = value & 0x0FFF = mg/dL directly
static void onGlucoseMeasNotify(BLERemoteCharacteristic* pChar,
                                uint8_t* pData, size_t length, bool isNotify) {
    if (length < 14) return;

    uint16_t seqNum     = (uint16_t)(pData[1] | (pData[2] << 8));
    uint16_t rawGlucose = (uint16_t)(pData[12] | (pData[13] << 8));
    int      mgDl       = (int)(rawGlucose & 0x0FFF);  // mantissa = mg/dL directly

    GlucoseReading reading = {};
    reading.sequenceNumber = seqNum;
    reading.valueMgDl      = mgDl;
    reading.timestampMs    = millis();

    if (xQueueSend(glucoseQueue, &reading, 0) != pdTRUE) {
        Serial.println("[BLE] glucoseQueue full — reading dropped");
    } else {
        Serial.printf("[BLE] Glucose: %d mg/dL (seq=%d)\n", mgDl, seqNum);
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

    // Set up BLE security ONCE using BLESecurity object (same pattern as legacy working code).
    // This must be done before any connection attempt.
    BLEDevice::setSecurityCallbacks(new GlucometerSecurityCB());
    BLESecurity* bleSec = new BLESecurity();
    bleSec->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);
    bleSec->setCapability(ESP_IO_CAP_IN);
    bleSec->setInitEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
    bleSec->setRespEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);

    MyAdvertisedDeviceCB* scanCb = new MyAdvertisedDeviceCB();
    BLEScan* pScan = setupScan(scanCb);

    state         = BLE_SCANNING_PEN;
    glucSyncTimer = millis() - GLUCOMETER_SYNC_INTERVAL_MS;  // trigger glucometer sync on first pass

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
                // Even without pen, sync glucometer on schedule
                if ((millis() - glucSyncTimer) >= GLUCOMETER_SYNC_INTERVAL_MS) {
                    Serial.println("[BLE] Pen not found — switching to glucometer sync");
                    state = BLE_GLUCOMETER_SYNC_START;
                } else {
                    Serial.println("[BLE] Pen not found, retrying in 2s...");
                    vTaskDelay(pdMS_TO_TICKS(2000));
                }
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
            penDoseChar = nullptr;

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
                penDoseChar = nullptr;
                state = BLE_SCANNING_PEN;
                break;
            }

            penDoseChar = penChar;
            penChar->registerForNotify(onPenDoseNotify);
            writePenTimeSync(penChar);
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
                penDoseChar = nullptr;
                state = BLE_SCANNING_PEN;
                break;
            }

            // Periodically update RSSI
            g_lastBleRssi = penClient->getRssi();
            sendPendingPenAcks();

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
                sendPendingPenAcks();
                penClient->disconnect();
                penDoseChar = nullptr;
                vTaskDelay(pdMS_TO_TICKS(500));
            }

            // Reset glucSyncTimer so we don't immediately re-trigger after reconnect
            glucSyncTimer = millis();
            // Security is configured once at task init — no need to set again here.

            glucometerFound = false;
            if (glucometerDevice) { delete glucometerDevice; glucometerDevice = nullptr; }

            Serial.println("[BLE] Scanning for glucometer...");
            pScan->clearResults();
            pScan->start(10, false);          // glucometer may take longer to appear
            vTaskDelay(pdMS_TO_TICKS(500));

            if (glucometerFound) {
                state = BLE_GLUCOMETER_SYNCING;
            } else {
                Serial.println("[BLE] Glucometer not found; returning to pen scan");
                state = BLE_SCANNING_PEN;
            }
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

            // Explicitly request encryption THEN wait 4s for pairing/bonding to complete.
            // Without this explicit call the GATT client tries to discover services before
            // the link is encrypted and fails with esp_ble_gattc_get_all_char: Unknown.
            esp_ble_set_encryption(*glucometerDevice->getAddress().getNative(),
                                   ESP_BLE_SEC_ENCRYPT_MITM);
            vTaskDelay(pdMS_TO_TICKS(4000));

            // BLERemoteService::getCharacteristic uses local GATT cache only.
            // Refresh cache after bonding/service-changed and force a fresh search.
            esp_ble_gattc_cache_refresh(*glucometerDevice->getAddress().getNative());
            vTaskDelay(pdMS_TO_TICKS(500));
            glucometerClient->getServices();
            vTaskDelay(pdMS_TO_TICKS(2000));

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
            // Subscribe to RACP INDICATE (0x2A52)
            racpChar->registerForNotify(onRacpIndicate, false);

            // Wait a moment after subscribing before sending RACP command
            vTaskDelay(pdMS_TO_TICKS(1000));

            // RACP 0x01 0x06 = Report Stored Records, Last Record
            // Accu-Chek Guide Me responds to 0x06 (last record) not 0x01 (all records)
            racpDone = false;
            uint8_t racpCmd[2] = {0x01, 0x06};
            racpChar->writeValue(racpCmd, 2, true);
            Serial.println("[BLE] RACP: requested last record (0x01 0x06)");

            // Wait up to 15s for RACP completion indication
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
            // Just clear the scan results and go back to pen scanning.
            // Do NOT call BLEDevice::deinit() — it crashes the BLE stack on ESP32-S3.
            Serial.println("[BLE] Glucometer sync done, back to scanning for pen...");
            pScan->clearResults();
            vTaskDelay(pdMS_TO_TICKS(500));
            glucSyncTimer = millis();
            state = BLE_SCANNING_PEN;
            break;
        }

        } // end switch
    } // end for(;;)
}
