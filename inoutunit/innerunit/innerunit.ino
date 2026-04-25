#include <WiFi.h>
#include <esp_now.h>
#include <esp_err.h>
#include <OneWire.h>
#include <DallasTemperature.h>
#include "HX711.h"
#include "../../config/firmware_config.h"

// CHANGE HERE if wiring/pins differ on your inner-unit board.
#define REED_PIN DOOR_SENSOR_PIN
#define ONE_WIRE_BUS TEMP_SENSOR_PIN
#define DOUT HX711_DOUT_PIN
#define CLK HX711_CLK_PIN

// CHANGE HERE after load-cell calibration for this hardware setup.
#define CALIBRATION_FACTOR LOAD_CELL_CALIBRATION

const uint32_t INNER_PACKET_MAGIC = 0x494E4E52; // 'INNR'

typedef struct __attribute__((packed)) {
  uint32_t magic;
  uint32_t seq;
  uint8_t door_open;
  float temperature;
  float weight;
} InnerPacket;

OneWire oneWire(ONE_WIRE_BUS);
DallasTemperature sensors(&oneWire);
HX711 scale;
float lastValidWeight = 0.0f;

uint32_t seqNo = 0;
bool espNowReady = false;
bool espNowPeerAdded = false;
const uint8_t BROADCAST_MAC[6] = {0xFF, 0xFF, 0xFF, 0xFF, 0xFF, 0xFF};
unsigned long lastWifiRetryMs = 0;

void initEspNow() {
  esp_err_t initErr = esp_now_init();
  if (initErr != ESP_OK) {
    Serial.printf("[ESPNOW] init failed: %s (%d)\n", esp_err_to_name(initErr), (int)initErr);
    return;
  }
  espNowReady = true;

  esp_now_peer_info_t peerInfo = {};
  memcpy(peerInfo.peer_addr, BROADCAST_MAC, 6);
  // Keep channel dynamic (0) to follow current interface channel.
  peerInfo.channel = 0;
  peerInfo.encrypt = false;

  if (!esp_now_is_peer_exist(peerInfo.peer_addr)) {
    esp_err_t addErr = esp_now_add_peer(&peerInfo);
    if (addErr != ESP_OK) {
      Serial.printf("[ESPNOW] add broadcast peer failed: %s (%d)\n", esp_err_to_name(addErr), (int)addErr);
    } else {
      espNowPeerAdded = true;
    }
  } else {
    espNowPeerAdded = true;
  }

  Serial.printf("[ESPNOW] ready channel_mode=%d peer_added=%s\n", peerInfo.channel, espNowPeerAdded ? "true" : "false");
}

void ensureWifiForEspNow() {
  WiFi.mode(WIFI_STA);
  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);

  unsigned long startMs = millis();
  while (WiFi.status() != WL_CONNECTED && (millis() - startMs) < 12000) {
    delay(250);
  }

  if (WiFi.status() == WL_CONNECTED) {
    Serial.printf("[WIFI] connected ip=%s channel=%d\n", WiFi.localIP().toString().c_str(), WiFi.channel());
  } else {
    Serial.println("[WIFI] connect timeout; ESP-NOW may fail if channel mismatched");
  }
}

void setup() {
  Serial.begin(115200);
  Serial.println("Inner unit booting...");

  pinMode(REED_PIN, INPUT_PULLUP);
  sensors.begin();

  scale.begin(DOUT, CLK);
  scale.set_scale(CALIBRATION_FACTOR);

  delay(2000);
  if (scale.is_ready()) {
    scale.tare();
    Serial.println("[HX711] tare complete");
  } else {
    Serial.println("[HX711] not ready during setup; check DOUT/CLK wiring and power");
  }

  ensureWifiForEspNow();
  initEspNow();
  Serial.println("Inner unit ready (ESP-NOW)");
}

void maintainWifiForEspNow() {
  if (WiFi.status() == WL_CONNECTED) return;
  if (millis() - lastWifiRetryMs < 8000) return;
  lastWifiRetryMs = millis();
  Serial.println("[WIFI] reconnecting for ESP-NOW channel alignment");
  ensureWifiForEspNow();
}

void loop() {
  maintainWifiForEspNow();

  int reedState = digitalRead(REED_PIN);

  sensors.requestTemperatures();
  float temp = sensors.getTempCByIndex(0);
  if (temp == DEVICE_DISCONNECTED_C || temp < -100 || temp == 85.0) {
    temp = NAN;
  }

  float weight = NAN;
  if (scale.is_ready()) {
    weight = scale.get_units(3);
    if (weight < 0) {
      weight = 0;
    }
    lastValidWeight = weight;
  } else {
    // Keep stream stable even if HX711 momentarily drops.
    weight = lastValidWeight;
    Serial.println("[HX711] not ready in loop");
  }

  InnerPacket packet = {};
  packet.magic = INNER_PACKET_MAGIC;
  packet.seq = ++seqNo;
  packet.door_open = (reedState == HIGH) ? 1 : 0;
  packet.temperature = isnan(temp) ? -999.0f : temp;
  packet.weight = weight;

  esp_err_t result = ESP_FAIL;
  if (espNowReady && espNowPeerAdded) {
    result = esp_now_send(BROADCAST_MAC, (uint8_t*)&packet, sizeof(packet));
  }

  if (result != ESP_OK && WiFi.status() != WL_CONNECTED) {
    // Most common reason for failed broadcast delivery here is channel drift.
    maintainWifiForEspNow();
  }

  Serial.printf(
    "{\"tx\":\"espnow\",\"seq\":%lu,\"door\":\"%s\",\"temp\":%.2f,\"weight\":%.2f,\"wifi\":%d,\"ch\":%d,\"send_ok\":%s,\"err\":\"%s\",\"err_code\":%d}\n",
    (unsigned long)packet.seq,
    packet.door_open ? "OPEN" : "CLOSED",
    packet.temperature,
    packet.weight,
    (int)WiFi.status(),
    WiFi.channel(),
    (result == ESP_OK) ? "true" : "false",
    esp_err_to_name(result),
    (int)result
  );

  delay(3000);
}