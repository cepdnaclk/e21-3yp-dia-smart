#include <BLEDevice.h>
#include <BLEUtils.h>
#include <BLEServer.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include "HX711.h"

#define REED_PIN 4
#define ONE_WIRE_BUS 21
#define DOUT 5
#define CLK 18

#define CALIBRATION_FACTOR 245.0

#define SAMPLES          10
#define NOISE_THRESHOLD  0.3
#define MIN_WEIGHT       0.5
#define TARE_SAMPLES     20

#define SERVICE_UUID        "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
#define CHARACTERISTIC_UUID "beb5483e-36e1-4688-b7f5-ea07361b26a8"

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
HX711 scale;

BLECharacteristic *pCharacteristic;
bool deviceConnected = false;

float lastStableWeight = 0.0;

float getMedianWeight(int samples) {
  float readings[samples];

  for (int i = 0; i < samples; i++) {
    readings[i] = scale.get_units(1);
    delay(10);
  }

  for (int i = 0; i < samples - 1; i++) {
    for (int j = 0; j < samples - i - 1; j++) {
      if (readings[j] > readings[j + 1]) {
        float temp = readings[j];
        readings[j] = readings[j + 1];
        readings[j + 1] = temp;
      }
    }
  }

  return readings[samples / 2];
}

class MyServerCallbacks : public BLEServerCallbacks {
  void onConnect(BLEServer* pServer) {
    deviceConnected = true;
    Serial.println("Client Connected");
  }

  void onDisconnect(BLEServer* pServer) {
    deviceConnected = false;
    Serial.println("Client Disconnected");
    pServer->startAdvertising();
  }
};

void setup() {

  Serial.begin(115200);

  pinMode(REED_PIN, INPUT_PULLUP);

  sensors.begin();

  scale.begin(DOUT, CLK);
  scale.set_scale(CALIBRATION_FACTOR);
  scale.set_gain(128);

  delay(3000);
  scale.tare(TARE_SAMPLES);

  BLEDevice::init("Inner_Unit");

  BLEServer *pServer = BLEDevice::createServer();
  pServer->setCallbacks(new MyServerCallbacks());

  BLEService *pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
                      CHARACTERISTIC_UUID,
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

  float rawWeight = 0;

  if (scale.is_ready()) {
    rawWeight = getMedianWeight(SAMPLES);
  }

  if (abs(rawWeight - lastStableWeight) > NOISE_THRESHOLD) {
    lastStableWeight = rawWeight;
  }

  float weight = lastStableWeight;
  if (weight < MIN_WEIGHT) weight = 0.0;

  if (isnan(temp)) temp = 0.0;
  if (isnan(weight)) weight = 0.0;

  char doorData[20];
  char tempData[20];
  char weightData[20];

  snprintf(doorData, sizeof(doorData),
           "Door:%s",
           reedState == HIGH ? "OPEN" : "CLOSED");

  snprintf(tempData, sizeof(tempData),
           "Temp:%.2fC",
           temp);

  snprintf(weightData, sizeof(weightData),
           "Weight:%.2fg",
           weight);

  Serial.println(doorData);
  Serial.println(tempData);
  Serial.println(weightData);
  Serial.println("------------------");

  if (deviceConnected) {

    pCharacteristic->setValue((uint8_t*)doorData, strlen(doorData));
    pCharacteristic->notify();
    delay(100);

    pCharacteristic->setValue((uint8_t*)tempData, strlen(tempData));
    pCharacteristic->notify();
    delay(100);

    pCharacteristic->setValue((uint8_t*)weightData, strlen(weightData));
    pCharacteristic->notify();
  }

  delay(3000);
}