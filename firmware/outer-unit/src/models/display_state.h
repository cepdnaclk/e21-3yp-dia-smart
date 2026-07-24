#pragma once

#include <stdint.h>

enum DisplayPage : uint8_t {
    DISPLAY_PAGE_DASHBOARD = 0,
    DISPLAY_PAGE_DEVICE_STATUS = 1,
    DISPLAY_PAGE_ALERTS = 2,
    DISPLAY_PAGE_QUEUE_STATUS = 3,
    DISPLAY_PAGE_PRESCRIPTION = 4
};

struct DisplayState {
    bool hasTelemetry;
    bool doorOpen;
    float temperatureC;
    float inventoryWeightG;
    float estimatedPercent;
    int glucoseMgDl;
    int glucometerSequenceNumber;
    float doseUnits;
    int innerBatteryPercent;
    int wifiRssiDbm;
    int bleRssiDbm;
    uint32_t freeHeapBytes;
    uint32_t lastUpdateMs;
    char timestamp[32];
    char injectedAt[32];

    uint8_t activePage;
    bool wifiConnected;
    bool mqttConnected;
    bool mqttRetrying;
    bool offlineQueueReady;
    uint16_t offlineQueueCount;
    uint32_t offlineQueueOldestMs;
    bool lastPublishOk;
    int mqttState;
    uint32_t lastPublishMs;
    uint32_t lastInnerPacketMs;
    uint32_t lastGlucoseMs;
    uint32_t lastDoseMs;

    bool dosePromptActive;
    bool dosePromptEditing;
    float promptPenDoseUnits;
    int pendingDoseUnits;
    int originalDoseUnits;
    uint8_t dosePromptRemainingSec;
    char doseEditBuffer[8];
};

