#include "BLEDevice.h"
#include <time.h> // Required for safe Timezone math

// Your Accu-Chek Guide Me's unique PIN
const uint32_t ACCU_CHEK_PIN = 836337; 

// Standard Bluetooth SIG UUIDs for Medical Glucose Meters
static BLEUUID glucoseServiceUUID("1808");
static BLEUUID glucoseMeasurementUUID("2A18");
static BLEUUID glucoseContextUUID("2A34");
static BLEUUID racpUUID("2A52");

static boolean doConnect = false;
static boolean connected = false;
static BLERemoteCharacteristic* pMeasurementChar;
static BLERemoteCharacteristic* pContextChar;
static BLERemoteCharacteristic* pRACPChar;
static BLEAdvertisedDevice* myDevice;

// Callback 1: Parses Hex and Outputs Clean JSON with Sri Lanka Time
static void glucoseDataCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify) {
    
    // The standard measurement packet is at least 14 bytes long
    if (length >= 14) {
      uint16_t seqNum = pData[1] | (pData[2] << 8);

      // Extract Base UTC Time
      uint16_t year = pData[3] | (pData[4] << 8);
      uint8_t month = pData[5];
      uint8_t day = pData[6];
      uint8_t hours = pData[7];
      uint8_t minutes = pData[8];
      uint8_t seconds = pData[9];

      // Extract Glucose Value (IEEE 11073 SFLOAT format)
      uint16_t rawGlucose = pData[12] | (pData[13] << 8);
      uint16_t mantissa = rawGlucose & 0x0FFF; 
      
      int glucose_mg_dl = mantissa; 
      float glucose_mmol_l = glucose_mg_dl / 18.0182; // Convert for meter screen matching

      // Safely Convert UTC to Sri Lanka Time (+05:30)
      setenv("TZ", "UTC", 1);
      tzset();
      struct tm t = {0};
      t.tm_year = year - 1900;
      t.tm_mon = month - 1;
      t.tm_mday = day;
      t.tm_hour = hours;
      t.tm_min = minutes;
      t.tm_sec = seconds;
      
      time_t utc_epoch = mktime(&t);
      time_t sl_epoch = utc_epoch + 19800; // Add 5 hours and 30 minutes in seconds
      struct tm *sl_t = gmtime(&sl_epoch);

      // Print the JSON Payload
      Serial.printf("{\"device\":\"Accu-Chek Guide Me\",\"record_id\":%d,\"datetime_sl\":\"%04d-%02d-%02d %02d:%02d:%02d\",\"glucose_mg_dl\":%d,\"glucose_mmol_L\":%.1f}\n", 
          seqNum, 
          sl_t->tm_year + 1900, sl_t->tm_mon + 1, sl_t->tm_mday, 
          sl_t->tm_hour, sl_t->tm_min, sl_t->tm_sec, 
          glucose_mg_dl, glucose_mmol_l);
    }
}

// Callback 2: Silently listens for System Responses
static void racpCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify) {
    if(length >= 4 && pData[0] == 0x06 && pData[3] == 0x01) {
      Serial.println("{\"system_status\": \"Download Complete\"}");
    }
}

// Callback 3: Detects when the meter turns off
class MyClientCallback : public BLEClientCallbacks {
  void onDisconnect(BLEClient* pclient) {
    connected = false;
    Serial.println("{\"system_status\": \"Meter Disconnected. Resuming Background Scan...\"}");
  }
};

// Security Class: Handles the MITM PIN Pairing
class MySecurity : public BLESecurityCallbacks {
  uint32_t onPassKeyRequest(){ return ACCU_CHEK_PIN; }
  void onPassKeyNotify(uint32_t pass_key){}
  bool onConfirmPIN(uint32_t pass_key){ vTaskDelay(50); return true; }
  bool onSecurityRequest(){ return true; }
  void onAuthenticationComplete(esp_ble_auth_cmpl_t cmpl){
    if(!cmpl.success){
      Serial.println("{\"error\": \"Pairing Failed. Check meter screen.\"}");
    }
  }
};

// Scanner Callback
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    if (advertisedDevice.haveServiceUUID() && advertisedDevice.isAdvertisingService(glucoseServiceUUID)) {
      BLEDevice::getScan()->stop();
      myDevice = new BLEAdvertisedDevice(advertisedDevice);
      doConnect = true;
    }
  }
};

void setup() {
  Serial.begin(115200);
  Serial.println("{\"system_status\": \"Dia-Smart Data Hub Booting...\"}");

  BLEDevice::init("Dia-Smart-Hub");
  BLEDevice::setSecurityCallbacks(new MySecurity());

  BLESecurity *pSecurity = new BLESecurity();
  pSecurity->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);
  pSecurity->setCapability(ESP_IO_CAP_IN); 
  pSecurity->setRespEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
  pSecurity->setInitEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);

  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setInterval(1349);
  pBLEScan->setWindow(449);
  pBLEScan->setActiveScan(true);
  
  // Start the very first scan
  pBLEScan->start(5, false);
}

bool connectToMeter() {
    BLEClient* pClient  = BLEDevice::createClient();
    pClient->setClientCallbacks(new MyClientCallback()); // Attached Disconnect Tracker
    pClient->connect(myDevice);
    
    esp_ble_set_encryption(myDevice->getAddress().getNative(), ESP_BLE_SEC_ENCRYPT_MITM);
    delay(5000); 
    
    BLERemoteService* pRemoteService = pClient->getService(glucoseServiceUUID);
    if (pRemoteService == nullptr) return false;

    pMeasurementChar = pRemoteService->getCharacteristic(glucoseMeasurementUUID);
    pContextChar = pRemoteService->getCharacteristic(glucoseContextUUID); 
    pRACPChar = pRemoteService->getCharacteristic(racpUUID);

    if(pMeasurementChar != nullptr && pMeasurementChar->canNotify()) {
        pMeasurementChar->registerForNotify(glucoseDataCallback, true); 
    }
    if(pContextChar != nullptr && pContextChar->canNotify()) {
        pContextChar->registerForNotify(glucoseDataCallback, true); 
    }
    if(pRACPChar != nullptr && pRACPChar->canIndicate()) {
        pRACPChar->registerForNotify(racpCallback, false); 
    }

    delay(2000);
    
    // Command 0x01, 0x01: "Report all stored records"
    uint8_t requestAll[2] = {0x01, 0x01};
    pRACPChar->writeValue(requestAll, 2, true);

    connected = true;
    return true;
}

void loop() {
  // 1. If we found the meter, stop scanning and connect!
  if (doConnect == true) {
    if (connectToMeter()) {
      Serial.println("{\"system_status\": \"Meter Linked! Syncing Data...\"}");
    } else {
      Serial.println("{\"error\": \"Connection Failed. Retrying...\"}");
    }
    doConnect = false;
  }

  // 2. If we are NOT connected, keep scanning the room silently
  if (!connected && !doConnect) {
    BLEScan* pBLEScan = BLEDevice::getScan();
    pBLEScan->start(5, false); // Scan for 5 seconds
    pBLEScan->clearResults();  // CRITICAL: Clear memory so the ESP32 doesn't crash over time
  }
  
  delay(1000); 
}