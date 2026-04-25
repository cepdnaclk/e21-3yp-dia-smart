#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEClient.h>
#include <BLESecurity.h>

#include <WiFi.h>
#include <HTTPClient.h>
#include <time.h>
#include <esp_now.h>
#include <esp_wifi.h>
#include "../config/firmware_config.h"

// =======================
// WiFi / Backend
// =======================
// Auto-generated from config/firmware_config.h

// =======================
// BLE UUIDs (Glucometer only)
// =======================
static BLEUUID glucoseServiceUUID("1808");
static BLEUUID glucoseMeasurementUUID("2A18");
static BLEUUID racpUUID("2A52");
static BLEUUID dosageServiceUUID(DOSAGE_BLE_SERVICE_UUID);
static BLEUUID dosageCharacteristicUUID(DOSAGE_BLE_CHARACTERISTIC_UUID);
const uint32_t ACCU_CHEK_PIN = atoi(GLUCO_BLE_PIN);

const uint32_t INNER_PACKET_MAGIC = 0x494E4E52; // 'INNR'

typedef struct __attribute__((packed)) {
  uint32_t magic;
  uint32_t seq;
  uint8_t door_open;
  float temperature;
  float weight;
} InnerPacket;

// =======================
// Runtime State
// =======================
float g_temperature = NAN;
String g_door = "";
float g_weight = NAN;
float g_insulin = NAN;
float g_glucose = NAN;

bool innerSeen = false;
bool innerConnected = false;
bool innerDataDirty = false;
uint32_t lastInnerSeq = 0;
uint32_t innerRxCount = 0;

BLEAddress* glucoAddress = nullptr;
BLEClient* glucoClient = nullptr;
BLERemoteCharacteristic* glucoMeasureChar = nullptr;
BLERemoteCharacteristic* glucoRacpChar = nullptr;
bool glucoSeen = false;
bool glucoConnected = false;
bool doConnectGluco = false;
bool glucoAnyRecordReceived = false;

BLEAddress* dosageAddress = nullptr;
BLEClient* dosageClient = nullptr;
BLERemoteCharacteristic* dosageNotifyChar = nullptr;
bool dosageSeen = false;
bool dosageConnected = false;
bool doConnectDosage = false;

unsigned long lastInnerNotifyMs = 0;
unsigned long lastScanMs = 0;
unsigned long lastWifiEnsureMs = 0;
unsigned long lastDiagMs = 0;
unsigned long nextInnerRetryMs = 0;
unsigned long nextGlucoRetryMs = 0;
unsigned long lastWifiRetryMs = 0;
unsigned long wifiConnectStartMs = 0;
unsigned long lastSensorUploadMs = 0;
unsigned long lastGlucoRequestMs = 0;

struct GlucoseRecord {
  uint16_t record_id;
  uint16_t glucose_mg_dl;
  float glucose_mmol_l;
  char datetime_sl[24];
};

const int MAX_RECORDS_PER_SYNC = GLUCO_MAX_BUFFER_RECORDS;
const int BATCH_UPLOAD_SIZE = GLUCO_BATCH_SIZE;
GlucoseRecord bufferedRecords[MAX_RECORDS_PER_SYNC];
int bufferedCount = 0;
bool syncDownloadComplete = false;
bool syncUploadDone = false;
uint32_t syncSequence = 0;

// =======================
// Helpers
// =======================
void ensureWifiConnected() {
  wl_status_t st = WiFi.status();
  if (st == WL_CONNECTED) {
    wifiConnectStartMs = 0;
    return;
  }

  // Portable handling for cores that do not expose WL_CONNECTING.
  // If a connection attempt was started recently, let it continue.
  if (wifiConnectStartMs > 0 && (millis() - wifiConnectStartMs) < 20000) {
    return;
  }

  // If a previous connection attempt timed out, keep radio stable for ESP-NOW and retry later.
  if (wifiConnectStartMs > 0 && (millis() - wifiConnectStartMs) >= 20000) {
    Serial.println("{\"transport\":\"wifi\",\"status\":\"connect_timeout_keep_radio\"}");
    esp_wifi_set_promiscuous(true);
    esp_wifi_set_channel(ESPNOW_CHANNEL, WIFI_SECOND_CHAN_NONE);
    esp_wifi_set_promiscuous(false);
    wifiConnectStartMs = 0;
    return;
  }

  // Throttle retries so BLE scanning/notify handling is not starved.
  if (millis() - lastWifiRetryMs < 8000) {
    return;
  }

  lastWifiRetryMs = millis();
  wifiConnectStartMs = millis();
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.println("{\"transport\":\"wifi\",\"status\":\"connecting\"}");
}

void sendSensorData(float temp, const String& door, float weight, float insulin, float glucose) {
  ensureWifiConnected();
  if (WiFi.status() != WL_CONNECTED) return;

  HTTPClient http;
  http.setTimeout(12000);
  http.begin(BACKEND_READINGS_URL);
  http.addHeader("Content-Type", "application/json");

  String json = "{";
  json += "\"temperature\":" + (isnan(temp) ? String("null") : String(temp, 2)) + ",";
  json += "\"door_status\":\"" + (door.length() ? door : String("CLOSED")) + "\",";
  json += "\"insulin_inventory_weight\":" + (isnan(weight) ? String("null") : String(weight, 2)) + ",";
  json += "\"insulin_level_value\":" + (isnan(insulin) ? String("null") : String(insulin, 2)) + ",";
  json += "\"glucose_value\":" + (isnan(glucose) ? String("null") : String(glucose, 2)) + ",";
  json += "\"skip_db\":true";
  json += "}";

  int code = http.POST(json);
  http.end();
  Serial.printf("{\"upload\":\"sensors\",\"http\":%d}\n", code);
}

void sendDosageData(float dose) {
  ensureWifiConnected();
  if (WiFi.status() != WL_CONNECTED) {
    Serial.println("{\"upload\":\"dosage\",\"status\":\"wifi_not_connected\"}");
    return;
  }

  HTTPClient http;
  http.setTimeout(12000);
  http.begin(BACKEND_DOSAGE_URL);
  http.addHeader("Content-Type", "application/json");

  String json = "{";
  json += "\"value\":" + String(dose, 2) + ",";
  json += "\"source\":\"esp32-c3-ble\"";
  json += "}";

  int code = http.POST(json);
  http.end();
  Serial.printf("{\"upload\":\"dosage\",\"http\":%d,\"dose\":%.2f}\n", code, dose);
}

void onInnerPacketReceived(const uint8_t* data, int len) {
  if (len < (int)sizeof(InnerPacket)) {
    return;
  }

  InnerPacket packet = {};
  memcpy(&packet, data, sizeof(packet));
  if (packet.magic != INNER_PACKET_MAGIC) {
    return;
  }

  if (packet.seq <= lastInnerSeq) {
    return;
  }
  lastInnerSeq = packet.seq;
  innerRxCount += 1;

  innerSeen = true;
  innerConnected = true;
  lastInnerNotifyMs = millis();

  g_door = packet.door_open ? "OPEN" : "CLOSED";
  g_temperature = (packet.temperature <= -900.0f) ? NAN : packet.temperature;
  g_weight = packet.weight;
  innerDataDirty = true;

  Serial.printf(
    "{\"inner\":\"espnow\",\"seq\":%lu,\"door\":\"%s\",\"temp\":%.2f,\"weight\":%.2f}\n",
    (unsigned long)packet.seq,
    g_door.c_str(),
    isnan(g_temperature) ? -999.0f : g_temperature,
    isnan(g_weight) ? -1.0f : g_weight
  );
}

#if defined(ESP_ARDUINO_VERSION_MAJOR) && (ESP_ARDUINO_VERSION_MAJOR >= 3)
void onEspNowRecv(const esp_now_recv_info_t* info, const uint8_t* data, int len) {
  (void)info;
  onInnerPacketReceived(data, len);
}
#else
void onEspNowRecv(const uint8_t* mac, const uint8_t* data, int len) {
  (void)mac;
  onInnerPacketReceived(data, len);
}
#endif

void initEspNowReceiver() {
  if (esp_now_init() != ESP_OK) {
    Serial.println("[ESPNOW] init failed on outer");
    return;
  }
  esp_now_register_recv_cb(onEspNowRecv);
  Serial.printf("[ESPNOW] receiver ready on outer, mac=%s\n", WiFi.macAddress().c_str());
}

bool hasRecordId(uint16_t seqNum) {
  for (int i = 0; i < bufferedCount; i++) {
    if (bufferedRecords[i].record_id == seqNum) return true;
  }
  return false;
}

void bufferGlucoRecord(uint16_t seqNum, const char* dt, int mgdl, float mmol) {
  if (hasRecordId(seqNum)) return;
  if (bufferedCount >= MAX_RECORDS_PER_SYNC) return;

  GlucoseRecord& r = bufferedRecords[bufferedCount++];
  r.record_id = seqNum;
  r.glucose_mg_dl = mgdl;
  r.glucose_mmol_l = mmol;
  snprintf(r.datetime_sl, sizeof(r.datetime_sl), "%s", dt);
  g_glucose = (float)mgdl;
  glucoAnyRecordReceived = true;
}

void uploadGlucoBatch() {
  ensureWifiConnected();
  if (WiFi.status() != WL_CONNECTED || bufferedCount == 0) return;

  String syncId = String("sync-") + String(++syncSequence);
  int uploaded = 0;

  for (int start = 0; start < bufferedCount; start += BATCH_UPLOAD_SIZE) {
    int end = start + BATCH_UPLOAD_SIZE;
    if (end > bufferedCount) end = bufferedCount;

    String payload = "{";
    payload += "\"device\":\"Accu-Chek Guide Me\",";
    payload += "\"sync_id\":\"" + syncId + "\",";
    payload += "\"skip_db\":true,";
    payload += "\"records\":[";

    for (int i = start; i < end; i++) {
      if (i > start) payload += ",";
      payload += "{";
      payload += "\"record_id\":" + String(bufferedRecords[i].record_id) + ",";
      payload += "\"datetime_sl\":\"" + String(bufferedRecords[i].datetime_sl) + "\",";
      payload += "\"glucose_mg_dl\":" + String(bufferedRecords[i].glucose_mg_dl) + ",";
      payload += "\"glucose_mmol_L\":" + String(bufferedRecords[i].glucose_mmol_l, 1);
      payload += "}";
    }
    payload += "]}";

    HTTPClient http;
    http.setTimeout(30000);
    http.begin(BACKEND_GLUCO_BATCH_URL);
    http.addHeader("Content-Type", "application/json");
    int code = http.POST(payload);
    http.end();

    Serial.printf("{\"upload\":\"gluco_batch\",\"chunk\":%d,\"http\":%d}\n", (start / BATCH_UPLOAD_SIZE) + 1, code);
    if (code < 200 || code >= 300) {
      return;
    }
    uploaded += (end - start);
  }

  Serial.printf("{\"upload\":\"gluco_done\",\"records\":%d}\n", uploaded);
  bufferedCount = 0;
  syncUploadDone = true;

  // Push merged sensor state once after glucose sync so dashboard sees latest combined values.
  sendSensorData(g_temperature, g_door, g_weight, g_insulin, g_glucose);
}

// =======================
// BLE Callbacks
// =======================
void glucoDataCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify
) {
  if (length < 14) return;

  uint16_t seqNum = pData[1] | (pData[2] << 8);
  uint16_t year = pData[3] | (pData[4] << 8);
  uint8_t month = pData[5];
  uint8_t day = pData[6];
  uint8_t hours = pData[7];
  uint8_t minutes = pData[8];
  uint8_t seconds = pData[9];

  uint16_t rawGlucose = pData[12] | (pData[13] << 8);
  uint16_t mantissa = rawGlucose & 0x0FFF;
  int glucoseMgDl = mantissa;
  float glucoseMmol = glucoseMgDl / 18.0182;

  setenv("TZ", "UTC", 1);
  tzset();
  struct tm t = {0};
  t.tm_year = year - 1900;
  t.tm_mon = month - 1;
  t.tm_mday = day;
  t.tm_hour = hours;
  t.tm_min = minutes;
  t.tm_sec = seconds;

  time_t utcEpoch = mktime(&t);
  time_t slEpoch = utcEpoch + 19800;
  struct tm* sl = gmtime(&slEpoch);

  char dt[24];
  snprintf(dt, sizeof(dt), "%04d-%02d-%02d %02d:%02d:%02d",
    sl->tm_year + 1900, sl->tm_mon + 1, sl->tm_mday,
    sl->tm_hour, sl->tm_min, sl->tm_sec);

  bufferGlucoRecord(seqNum, dt, glucoseMgDl, glucoseMmol);
  Serial.printf("{\"device\":\"Accu-Chek Guide Me\",\"record_id\":%d,\"datetime_sl\":\"%s\",\"glucose_mg_dl\":%d}\n",
    seqNum, dt, glucoseMgDl);
}

void racpCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify
) {
  (void)pBLERemoteCharacteristic;
  (void)isNotify;
  if (length >= 4 && pData[0] == 0x06 && pData[3] == 0x01) {
    syncDownloadComplete = true;
    Serial.println("{\"system_status\":\"Download Complete\"}");
  }
}

void requestAllGlucometerRecords() {
  if (!glucoConnected || !glucoRacpChar) return;
  uint8_t requestAll[2] = {0x01, 0x01};
  glucoRacpChar->writeValue(requestAll, 2, true);
  lastGlucoRequestMs = millis();
  Serial.println("[BLE] Requested all glucometer records");
}

void dosageCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify
) {
  (void)pBLERemoteCharacteristic;
  (void)isNotify;

  if (length == 0) return;

  String payload;
  for (size_t i = 0; i < length; i++) {
    payload += (char)pData[i];
  }

  int split = payload.indexOf(',');
  String doseText = split >= 0 ? payload.substring(0, split) : payload;
  String status = split >= 0 ? payload.substring(split + 1) : "";
  doseText.trim();
  status.trim();
  status.toUpperCase();

  float dose = doseText.toFloat();
  if (dose <= 0.0f) return;

  if (status.length() == 0 || status == "INJECTED") {
    g_insulin = dose;
    Serial.printf("{\"dosage\":\"ble\",\"payload\":\"%s\",\"dose\":%.2f}\n", payload.c_str(), dose);
    sendDosageData(dose);
    sendSensorData(g_temperature, g_door, g_weight, g_insulin, g_glucose);
  }
}

class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {
    if (!advertisedDevice.haveServiceUUID()) {
      return;
    }

    if (!glucoSeen && advertisedDevice.isAdvertisingService(glucoseServiceUUID)) {
      if (glucoAddress) {
        delete glucoAddress;
        glucoAddress = nullptr;
      }
      glucoAddress = new BLEAddress(advertisedDevice.getAddress());
      glucoSeen = true;
      doConnectGluco = true;
      Serial.println("[SCAN] Glucometer found");
    }

    if (!dosageSeen && advertisedDevice.isAdvertisingService(dosageServiceUUID)) {
      if (dosageAddress) {
        delete dosageAddress;
        dosageAddress = nullptr;
      }
      dosageAddress = new BLEAddress(advertisedDevice.getAddress());
      dosageSeen = true;
      doConnectDosage = true;
      Serial.println("[SCAN] Dosage BLE device found");
    }
  }
};

class MySecurity : public BLESecurityCallbacks {
  uint32_t onPassKeyRequest() { return ACCU_CHEK_PIN; }
  void onPassKeyNotify(uint32_t pass_key) {}
  bool onConfirmPIN(uint32_t pass_key) { return true; }
  bool onSecurityRequest() { return true; }
  void onAuthenticationComplete(esp_ble_auth_cmpl_t cmpl) {}
};

// =======================
// Connectors
// =======================
bool connectGlucometer() {
  if (!glucoAddress) return false;
  Serial.println("[BLE] Connecting to glucometer...");

  glucoClient = BLEDevice::createClient();
  if (!glucoClient->connect(*glucoAddress)) {
    Serial.println("[BLE] Glucometer connect failed");
    return false;
  }

  esp_ble_set_encryption(glucoAddress->getNative(), ESP_BLE_SEC_ENCRYPT_MITM);
  delay(2000);

  BLERemoteService* svc = glucoClient->getService(glucoseServiceUUID);
  if (!svc) {
    Serial.println("[BLE] Glucometer service not found");
    glucoClient->disconnect();
    return false;
  }

  glucoMeasureChar = svc->getCharacteristic(glucoseMeasurementUUID);
  glucoRacpChar = svc->getCharacteristic(racpUUID);

  if (!glucoMeasureChar || !glucoRacpChar) {
    Serial.println("[BLE] Glucometer characteristics not found");
    glucoClient->disconnect();
    return false;
  }

  glucoMeasureChar->registerForNotify(glucoDataCallback, true);
  glucoRacpChar->registerForNotify(racpCallback, false);

  glucoConnected = true;
  syncDownloadComplete = false;
  syncUploadDone = false;
  glucoAnyRecordReceived = false;
  bufferedCount = 0;
  requestAllGlucometerRecords();
  Serial.println("[BLE] Connected to glucometer and requested all records");
  return true;
}

bool connectDosageDevice() {
  if (!dosageAddress) return false;
  Serial.println("[BLE] Connecting to dosage BLE device...");

  dosageClient = BLEDevice::createClient();
  if (!dosageClient->connect(*dosageAddress)) {
    Serial.println("[BLE] Dosage device connect failed");
    return false;
  }

  BLERemoteService* svc = dosageClient->getService(dosageServiceUUID);
  if (!svc) {
    Serial.println("[BLE] Dosage service not found");
    dosageClient->disconnect();
    return false;
  }

  dosageNotifyChar = svc->getCharacteristic(dosageCharacteristicUUID);
  if (!dosageNotifyChar) {
    Serial.println("[BLE] Dosage characteristic not found");
    dosageClient->disconnect();
    return false;
  }

  dosageNotifyChar->registerForNotify(dosageCallback, true);
  dosageConnected = true;
  Serial.println("[BLE] Connected to dosage BLE device");
  return true;
}

// =======================
// Setup / Loop
// =======================
void setup() {
  Serial.begin(115200);
  Serial.println("Dia-Smart outer hub booting...");

  WiFi.mode(WIFI_STA);
  // Initial blocking attempt to lock channel before starting ESP-NOW receiver.
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  unsigned long wifiStart = millis();
  while (WiFi.status() != WL_CONNECTED && (millis() - wifiStart) < 12000) {
    delay(250);
  }
  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("[WIFI] connected ip=%s channel=%d\n", WiFi.localIP().toString().c_str(), WiFi.channel());
  } else {
    Serial.println("[WIFI] initial connect timeout; continuing with retries");
  }
  ensureWifiConnected();
  initEspNowReceiver();

  BLEDevice::init("Dia-Smart-Hub");
  BLEDevice::setSecurityCallbacks(new MySecurity());

  BLESecurity* sec = new BLESecurity();
  sec->setAuthenticationMode(ESP_LE_AUTH_REQ_SC_MITM_BOND);
  sec->setCapability(ESP_IO_CAP_IN);
  sec->setRespEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);
  sec->setInitEncryptionKey(ESP_BLE_ENC_KEY_MASK | ESP_BLE_ID_KEY_MASK);

  BLEScan* scan = BLEDevice::getScan();
  scan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  scan->setActiveScan(true);
}

void loop() {
  unsigned long now = millis();
  BLEScan* scan = BLEDevice::getScan();

  if (now - lastWifiEnsureMs > 10000) {
    ensureWifiConnected();
    lastWifiEnsureMs = now;
  }

  if ((!glucoSeen || !dosageSeen) && (now - lastScanMs > 4000)) {
    scan->start(3, false);
    scan->clearResults();
    lastScanMs = now;
  }

  // Prefer glucometer sync before dosage BLE connection to reduce BLE contention.
  if (doConnectGluco && !glucoConnected && now >= nextGlucoRetryMs) {
    if (dosageConnected && dosageClient && dosageClient->isConnected()) {
      Serial.println("[BLE] Temporarily disconnecting dosage listener before glucometer sync");
      dosageClient->disconnect();
      dosageConnected = false;
    }

    if (!connectGlucometer()) {
      nextGlucoRetryMs = now + 5000;
      glucoSeen = false;
    }
    doConnectGluco = false;
  }

  if (doConnectDosage && !dosageConnected && !glucoConnected && !doConnectGluco) {
    if (!connectDosageDevice()) {
      dosageSeen = false;
    }
    doConnectDosage = false;
  }

  // Mark inner disconnected if no ESP-NOW packets for too long.
  if (innerConnected && (now - lastInnerNotifyMs > 15000)) {
    innerConnected = false;
  }

  if (innerDataDirty && (now - lastSensorUploadMs > 1200)) {
    sendSensorData(g_temperature, g_door, g_weight, g_insulin, g_glucose);
    innerDataDirty = false;
    lastSensorUploadMs = now;
  }

  if (glucoConnected && glucoClient && !glucoClient->isConnected()) {
    Serial.println("[BLE] Glucometer disconnected");
    glucoConnected = false;
    glucoSeen = false;
    doConnectGluco = false;
  }

  if (glucoConnected && !syncDownloadComplete && (now - lastGlucoRequestMs > 12000)) {
    // Retry download request if meter connected but has not started/finished transfer.
    requestAllGlucometerRecords();
  }

  if (dosageConnected && dosageClient && !dosageClient->isConnected()) {
    Serial.println("[BLE] Dosage BLE device disconnected");
    dosageConnected = false;
    dosageSeen = false;
    doConnectDosage = false;
  }

  if (syncDownloadComplete && !syncUploadDone) {
    uploadGlucoBatch();
  }

  if (syncUploadDone && glucoConnected && glucoClient && glucoClient->isConnected()) {
    Serial.println("[BLE] Glucometer sync done; disconnecting to restore dosage listener");
    glucoClient->disconnect();
    glucoConnected = false;
    doConnectDosage = true;
  }

  if (now - lastDiagMs > 10000) {
    Serial.printf(
      "{\"diag\":\"outer\",\"wifiStatus\":%d,\"wifiChannel\":%d,\"innerSeen\":%s,\"innerConnected\":%s,\"innerAgeMs\":%lu,\"innerRxCount\":%lu,\"lastInnerSeq\":%lu,\"glucoSeen\":%s,\"glucoConnected\":%s,\"buffered\":%d}\n",
      (int)WiFi.status(),
      WiFi.channel(),
      innerSeen ? "true" : "false", // seen via ESP-NOW packets
      innerConnected ? "true" : "false", // recent packet within timeout
      now - lastInnerNotifyMs,
      (unsigned long)innerRxCount,
      (unsigned long)lastInnerSeq,
      glucoSeen ? "true" : "false",
      glucoConnected ? "true" : "false",
      bufferedCount
    );
    Serial.printf(
      "{\"diag\":\"dosage_ble\",\"seen\":%s,\"connected\":%s,\"last_dose\":%.2f}\n",
      dosageSeen ? "true" : "false",
      dosageConnected ? "true" : "false",
      isnan(g_insulin) ? -1.0f : g_insulin
    );
    lastDiagMs = now;
  }

  delay(300);
}