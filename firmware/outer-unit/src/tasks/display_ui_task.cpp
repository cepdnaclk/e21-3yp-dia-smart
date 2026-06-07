#include <Arduino.h>
#include <TFT_eSPI.h>
#include <math.h>
#include "config/app_config.h"
#include "managers/display_state_manager.h"

#if DISPLAY_ENABLED

namespace {
TFT_eSPI tft = TFT_eSPI();

constexpr uint16_t COLOR_BG = TFT_BLACK;
constexpr uint16_t COLOR_PANEL = 0x18E3;
constexpr uint16_t COLOR_PANEL_ALT = 0x2124;
constexpr uint16_t COLOR_TEXT = TFT_WHITE;
constexpr uint16_t COLOR_MUTED = 0xAD55;
constexpr uint16_t COLOR_ACCENT = 0x06FF;
constexpr uint16_t COLOR_OK = 0x07E0;
constexpr uint16_t COLOR_WARN = 0xFD20;
constexpr uint16_t COLOR_BAD = 0xF800;

void drawHeader() {
    tft.fillRect(0, 0, DISPLAY_WIDTH, 54, COLOR_ACCENT);
    tft.setTextColor(TFT_BLACK, COLOR_ACCENT);
    tft.setTextDatum(TL_DATUM);
    tft.setTextFont(4);
    tft.drawString("Dia-Smart", 12, 8);
    tft.setTextFont(2);
    tft.drawString("Outer Unit", 214, 28);
}

void drawCard(int x, int y, int w, int h, const char* label, const char* value,
              uint16_t valueColor = COLOR_TEXT, bool alt = false) {
    uint16_t bg = alt ? COLOR_PANEL_ALT : COLOR_PANEL;
    tft.fillRoundRect(x, y, w, h, 8, bg);
    tft.setTextDatum(TL_DATUM);
    tft.setTextColor(COLOR_MUTED, bg);
    tft.setTextFont(2);
    tft.drawString(label, x + 10, y + 8);
    tft.setTextColor(valueColor, bg);
    tft.setTextFont(4);
    tft.drawString(value, x + 10, y + 30);
}

void drawFooter(const DisplayState& state) {
    tft.fillRect(0, 438, DISPLAY_WIDTH, 42, COLOR_BG);
    tft.setTextDatum(TL_DATUM);
    tft.setTextFont(2);
    tft.setTextColor(COLOR_MUTED, COLOR_BG);

    char line[64];
    if (state.hasTelemetry) {
        snprintf(line, sizeof(line), "WiFi %d dBm | BLE %d dBm | Heap %lu KB",
                 state.wifiRssiDbm,
                 state.bleRssiDbm,
                 (unsigned long)(state.freeHeapBytes / 1024));
    } else {
        snprintf(line, sizeof(line), "Waiting for first telemetry event...");
    }
    tft.drawString(line, 10, 446);
}

uint16_t tempColor(float tempC) {
    if (isnan(tempC)) {
        return COLOR_WARN;
    }
    if (tempC < TEMP_MIN_C || tempC > TEMP_MAX_C) {
        return COLOR_BAD;
    }
    return COLOR_OK;
}

void drawDashboard(const DisplayState& state) {
    tft.fillScreen(COLOR_BG);
    drawHeader();

    char tempBuf[24];
    if (!state.hasTelemetry || isnan(state.temperatureC)) {
        snprintf(tempBuf, sizeof(tempBuf), "--.- C");
    } else {
        snprintf(tempBuf, sizeof(tempBuf), "%.1f C", state.temperatureC);
    }
    drawCard(10, 68, 145, 82, "FRIDGE TEMP", tempBuf, tempColor(state.temperatureC));

    const char* doorValue = state.hasTelemetry ? (state.doorOpen ? "OPEN" : "CLOSED") : "--";
    drawCard(165, 68, 145, 82, "DOOR", doorValue,
             state.doorOpen ? COLOR_WARN : COLOR_OK, true);

    char invBuf[24];
    if (state.hasTelemetry) {
        snprintf(invBuf, sizeof(invBuf), "%.0f%%", state.estimatedPercent);
    } else {
        snprintf(invBuf, sizeof(invBuf), "--%%");
    }
    drawCard(10, 162, 145, 82, "INSULIN STOCK", invBuf,
             state.estimatedPercent < 20.0f ? COLOR_WARN : COLOR_TEXT, true);

    char weightBuf[24];
    if (state.hasTelemetry) {
        snprintf(weightBuf, sizeof(weightBuf), "%.0fg", state.inventoryWeightG);
    } else {
        snprintf(weightBuf, sizeof(weightBuf), "--g");
    }
    drawCard(165, 162, 145, 82, "WEIGHT", weightBuf);

    char glucoseBuf[24];
    if (state.hasTelemetry && state.glucoseMgDl > 0) {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "%d", state.glucoseMgDl);
    } else {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "--");
    }
    drawCard(10, 256, 145, 82, "GLUCOSE mg/dL", glucoseBuf, COLOR_TEXT);

    char doseBuf[24];
    if (state.hasTelemetry && state.doseUnits > 0.0f) {
        snprintf(doseBuf, sizeof(doseBuf), "%.1fu", state.doseUnits);
    } else {
        snprintf(doseBuf, sizeof(doseBuf), "--");
    }
    drawCard(165, 256, 145, 82, "LAST DOSE", doseBuf, COLOR_TEXT, true);

    tft.fillRoundRect(10, 350, 300, 76, 8, COLOR_PANEL);
    tft.setTextFont(2);
    tft.setTextColor(COLOR_MUTED, COLOR_PANEL);
    tft.drawString("LAST DOSE TIME", 20, 360);
    tft.setTextColor(COLOR_TEXT, COLOR_PANEL);
    if (state.hasTelemetry && state.doseUnits > 0.0f) {
        tft.drawString(state.injectedAt, 20, 384);
    } else {
        tft.drawString("No dose received yet", 20, 384);
    }

    tft.setTextColor(COLOR_MUTED, COLOR_PANEL);
    if (state.hasTelemetry && state.glucometerSequenceNumber > 0) {
        char seqBuf[40];
        snprintf(seqBuf, sizeof(seqBuf), "Glucose seq %d", state.glucometerSequenceNumber);
        tft.drawString(seqBuf, 20, 406);
    } else {
        tft.drawString("No glucose reading yet", 20, 406);
    }

    drawFooter(state);
}
}

void displayUiTask(void* parameter) {
    (void)parameter;

    tft.init();
    tft.setRotation(0);
    tft.fillScreen(COLOR_BG);
    tft.setTextDatum(TL_DATUM);
    tft.setTextColor(COLOR_TEXT, COLOR_BG);
    tft.setTextFont(4);
    tft.drawString("Dia-Smart", 54, 190);
    tft.setTextFont(2);
    tft.drawString("Display starting...", 78, 230);
    Serial.println("[Display] UI task started");

    for (;;) {
        DisplayState state = getDisplayStateSnapshot();
        drawDashboard(state);
        vTaskDelay(pdMS_TO_TICKS(DISPLAY_REFRESH_MS));
    }
}

#else

void displayUiTask(void* parameter) {
    (void)parameter;
    vTaskDelete(nullptr);
}

#endif
