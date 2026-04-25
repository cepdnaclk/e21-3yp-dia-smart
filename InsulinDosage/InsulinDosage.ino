#include <Wire.h>
#include <AS5600.h>
#include <BLEDevice.h>
#include <BLEServer.h>
#include <BLEUtils.h>
#include <BLE2902.h>
#include "../config/firmware_config.h"

AS5600 as5600;

BLECharacteristic* pCharacteristic;

#define SERVICE_UUID DOSAGE_BLE_SERVICE_UUID
#define CHARACTERISTIC_UUID DOSAGE_BLE_CHARACTERISTIC_UUID

#define BUTTON_PIN 3

// ESP32-C3 Super Mini default I2C pins in this project.
#define I2C_SDA_PIN 8
#define I2C_SCL_PIN 9

float degreesPerUnit = 15.0f;

float lastAngle = 0.0f;
float totalRotation = 0.0f;
float currentDose = 0.0f;
float lockedDose = 0.0f;

unsigned long lastButtonDebounceMs = 0;

enum State { SETTING, INJECTING };
State state = SETTING;

int lastButtonState = HIGH;

void sendBLE(float dose, const String& status) {
  String data = String(dose, 2) + "," + status;
  pCharacteristic->setValue(data.c_str());
  pCharacteristic->notify();
  Serial.print("BLE Sent: ");
  Serial.println(data);
}

void setup() {
  Serial.begin(115200);
  Wire.begin(I2C_SDA_PIN, I2C_SCL_PIN);

  pinMode(BUTTON_PIN, INPUT_PULLUP);

  BLEDevice::init(DOSAGE_BLE_DEVICE_NAME);
  BLEServer* pServer = BLEDevice::createServer();
  BLEService* pService = pServer->createService(SERVICE_UUID);

  pCharacteristic = pService->createCharacteristic(
    CHARACTERISTIC_UUID,
    BLECharacteristic::PROPERTY_NOTIFY
  );
  pCharacteristic->addDescriptor(new BLE2902());

  pService->start();
  BLEAdvertising* pAdvertising = BLEDevice::getAdvertising();
  pAdvertising->addServiceUUID(SERVICE_UUID);
  pAdvertising->start();

  Serial.println("=== Dosage BLE Server Started ===");
  Serial.printf("Service UUID: %s\n", SERVICE_UUID);
  Serial.printf("Characteristic UUID: %s\n", CHARACTERISTIC_UUID);
}

void loop() {
  int raw = as5600.rawAngle();
  float angle = raw * 360.0f / 4096.0f;
  float diff = angle - lastAngle;

  if (diff > 180.0f) diff -= 360.0f;
  if (diff < -180.0f) diff += 360.0f;

  totalRotation += diff;
  currentDose = totalRotation / degreesPerUnit;

  int currentButtonState = digitalRead(BUTTON_PIN);

  if (
    state == SETTING &&
    currentButtonState == LOW &&
    lastButtonState == HIGH &&
    (millis() - lastButtonDebounceMs) > 50
  ) {
    lastButtonDebounceMs = millis();
    state = INJECTING;

    lockedDose = fabs(currentDose);

    Serial.println("\n>>> BUTTON CLICKED - SENDING DATA INSTANTLY <<<");
    Serial.printf("Injected Dose: %.2f units\n", lockedDose);

    sendBLE(lockedDose, "INJECTED");
  }

  if (state == INJECTING && fabs(currentDose) <= 0.5f) {
    Serial.println(">>> MECHANISM RETURNED TO ZERO. READY FOR NEXT DOSE. <<<\n");

    state = SETTING;
    totalRotation = 0.0f;
    currentDose = 0.0f;
  }

  lastAngle = angle;
  lastButtonState = currentButtonState;

  Serial.printf("Live Dial: %.2f | Button: %d\n", currentDose, currentButtonState);

  delay(200);
}
