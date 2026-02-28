#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include "HX711.h"

// ---------------- PIN DEFINITIONS ----------------
#define REED_PIN 4
#define ONE_WIRE_BUS 2
#define DOUT 5
#define CLK 18

#define CALIBRATION_FACTOR 245.0   // 🔴 Your value

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
HX711 scale;

BLECharacteristic *pCharacteristic;
bool deviceConnected = false;

// BLE Callback
class MyServerCallbacks: public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
  };
  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    pServer->startAdvertising();
  }
};

void setup() {
  Serial.begin(115200);

  pinMode(REED_PIN, INPUT_PULLUP);
  sensors.begin();

  scale.begin(DOUT, CLK);
  scale.set_scale(CALIBRATION_FACTOR);

  delay(2000);        // 🔥 Allow system to stabilize
  scale.tare();       // 🔥 Tare AFTER stabilization

  // BLE Setup
  BLEDevice::init("Inner_Unit");
  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
                      BLECharacteristic::PROPERTY_READ |
                      BLECharacteristic::PROPERTY_NOTIFY
                    );

  pService->start();
  pServer->getAdvertising()->start();

  Serial.println("Inner Unit Ready");
}

void loop() {

  int reedState = digitalRead(REED_PIN);

  sensors.requestTemperatures();
  float temp = sensors.getTempCByIndex(0);

  float weight = scale.get_units(3);

  // 🔥 Prevent negative drift
  if (weight < 0) weight = 0;

  char data[100];
  sprintf(data, "Door:%s,Temp:%.2fC,Weight:%.2fg",
          reedState == HIGH ? "OPEN" : "CLOSED",
          temp,
          weight);

  Serial.println(data);

  if (deviceConnected) {
    pCharacteristic->setValue(data);
    pCharacteristic->notify();
  }

  delay(3000);
}