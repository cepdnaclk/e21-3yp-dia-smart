#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEScan.h>
#include <BLEClient.h>

#include <WiFi.h>
#include <HTTPClient.h>

// =======================
// WiFi Configuration
// =======================
const char* ssid = "Sanjeev";
const char* password = "Sanjeevan2002";
const char* serverUrl = "http://192.168.1.170:3000/api/readings";

// =======================
// Global Sensor Variables
// =======================
float g_temperature = NAN;
String g_door = "";
float g_weight = NAN;
float g_insulin = NAN;   // If you add later
float g_glucose = NAN;   // If you add later

// =======================
// BLE UUIDs
// =======================
#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

BLEAddress *serverAddress;
BLEClient*  pClient;
BLERemoteCharacteristic* pRemoteCharacteristic;

bool connected = false;
bool doConnect = false;

// =======================
// Forward Declaration
// =======================
void notifyCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify);

void sendData(float temp, String door, float weight, float insulin, float glucose);

// =======================
// BLE Scan Callback
// =======================
class MyAdvertisedDeviceCallbacks: public BLEAdvertisedDeviceCallbacks {
  void onResult(BLEAdvertisedDevice advertisedDevice) {

    if (advertisedDevice.getName() == "Inner_Unit") {

      Serial.println("Found Inner Unit!");
      BLEDevice::getScan()->stop();

      serverAddress = new BLEAddress(advertisedDevice.getAddress());
      doConnect = true;
    }
  }
};

// =======================
// Connect To Inner Unit
// =======================
bool connectToServer() {

  pClient = BLEDevice::createClient();

  if (!pClient->connect(*serverAddress)) {
    Serial.println("Connection Failed!");
    return false;
  }

  Serial.println("Connected to Inner Unit!");

  BLERemoteService* pRemoteService =
      pClient->getService(SERVICE_UUID);

  if (pRemoteService == nullptr) {
    Serial.println("Service Not Found!");
    return false;
  }

  pRemoteCharacteristic =
      pRemoteService->getCharacteristic(CHARACTERISTIC_UUID);

  if (pRemoteCharacteristic == nullptr) {
    Serial.println("Characteristic Not Found!");
    return false;
  }

  if (pRemoteCharacteristic->canNotify())
    pRemoteCharacteristic->registerForNotify(notifyCallback);

  connected = true;
  return true;
}

// =======================
// Setup
// =======================
void setup() {

  Serial.begin(115200);
  Serial.println("Outer Unit Starting...");

  // WiFi Connect
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) {
    delay(500);
    Serial.print(".");
  }
  Serial.println("\nWiFi Connected");

  // BLE Init
  BLEDevice::init("");
  BLEScan* pBLEScan = BLEDevice::getScan();
  pBLEScan->setAdvertisedDeviceCallbacks(new MyAdvertisedDeviceCallbacks());
  pBLEScan->setActiveScan(true);
  pBLEScan->start(5, false);
}

// =======================
// Main Loop
// =======================
void loop() {

  if (doConnect == true) {

    if (connectToServer()) {
      Serial.println("Ready to receive data...");
    }

    doConnect = false;
  }

  if (connected && !pClient->isConnected()) {
    Serial.println("Disconnected! Re-scanning...");
    connected = false;
    BLEDevice::getScan()->start(5, false);
  }

  delay(2000);
}

// =======================
// Upload Function
// =======================
void sendData(float temp, String door, float weight, float insulin, float glucose) {

  if (WiFi.status() == WL_CONNECTED) {

    HTTPClient http;
    http.begin(serverUrl);
    http.addHeader("Content-Type", "application/json");

    String json = "{";

    json += "\"temperature\":" + (isnan(temp) ? "null" : String(temp)) + ",";
    json += "\"door_status\":\"" + door + "\",";
    json += "\"insulin_inventory_weight\":" + (isnan(weight) ? "null" : String(weight)) + ",";
    json += "\"insulin_level_value\":" + (isnan(insulin) ? "null" : String(insulin)) + ",";
    json += "\"glucose_value\":" + (isnan(glucose) ? "null" : String(glucose));

    json += "}";

    Serial.println("Sending JSON:");
    Serial.println(json);

    int httpResponseCode = http.POST(json);

    Serial.print("Server Response: ");
    Serial.println(httpResponseCode);

    http.end();
  }
}

// =======================
// BLE Notification Handler
// =======================
void notifyCallback(
  BLERemoteCharacteristic* pBLERemoteCharacteristic,
  uint8_t* pData,
  size_t length,
  bool isNotify)
{
  String received = "";

  for (int i = 0; i < length; i++) {
    received += (char)pData[i];
  }

  Serial.println("Received: " + received);

  if (received.startsWith("Temp:")) {
    g_temperature = received.substring(5).toFloat();
  }
  else if (received.startsWith("Door:")) {
    g_door = received.substring(5);
  }
  else if (received.startsWith("Weight:")) {
    g_weight = received.substring(7).toFloat();

    // Upload only when weight received (last message)
    sendData(g_temperature, g_door, g_weight, g_insulin, g_glucose);
  }
}