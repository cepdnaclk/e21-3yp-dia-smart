#include <Arduino.h>
#include <ctype.h>
#include <math.h>

#include "config/app_config.h"
#include "managers/display_state_manager.h"

#if DISPLAY_ENABLED

#include "soc/gpio_struct.h"

namespace {
constexpr uint16_t COLOR_BG = 0x0000;
constexpr uint16_t COLOR_PANEL = 0x18E3;
constexpr uint16_t COLOR_PANEL_ALT = 0x2124;
constexpr uint16_t COLOR_TEXT = 0xFFFF;
constexpr uint16_t COLOR_MUTED = 0xAD55;
constexpr uint16_t COLOR_ACCENT = 0x07FF;
constexpr uint16_t COLOR_OK = 0x07E0;
constexpr uint16_t COLOR_WARN = 0xFFE0;
constexpr uint16_t COLOR_BAD = 0xF800;

constexpr uint32_t pinBit(uint8_t pin) {
    return 1UL << pin;
}

constexpr uint32_t DATA_MASK =
    pinBit(DISPLAY_PIN_LCD_D0) |
    pinBit(DISPLAY_PIN_LCD_D1) |
    pinBit(DISPLAY_PIN_LCD_D2) |
    pinBit(DISPLAY_PIN_LCD_D3) |
    pinBit(DISPLAY_PIN_LCD_D4) |
    pinBit(DISPLAY_PIN_LCD_D5) |
    pinBit(DISPLAY_PIN_LCD_D6) |
    pinBit(DISPLAY_PIN_LCD_D7);

constexpr uint32_t WR_BIT = pinBit(DISPLAY_PIN_LCD_WR);
constexpr uint32_t RS_BIT = pinBit(DISPLAY_PIN_LCD_RS);
constexpr uint32_t CS_BIT = pinBit(DISPLAY_PIN_LCD_CS);
constexpr uint32_t RST_BIT = pinBit(DISPLAY_PIN_LCD_RST);

uint32_t dataMaskFor(uint8_t value) {
    uint32_t mask = 0;
    if (value & 0x01) mask |= pinBit(DISPLAY_PIN_LCD_D0);
    if (value & 0x02) mask |= pinBit(DISPLAY_PIN_LCD_D1);
    if (value & 0x04) mask |= pinBit(DISPLAY_PIN_LCD_D2);
    if (value & 0x08) mask |= pinBit(DISPLAY_PIN_LCD_D3);
    if (value & 0x10) mask |= pinBit(DISPLAY_PIN_LCD_D4);
    if (value & 0x20) mask |= pinBit(DISPLAY_PIN_LCD_D5);
    if (value & 0x40) mask |= pinBit(DISPLAY_PIN_LCD_D6);
    if (value & 0x80) mask |= pinBit(DISPLAY_PIN_LCD_D7);
    return mask;
}

void rawWrite8(uint8_t value) {
    GPIO.out_w1tc = DATA_MASK | WR_BIT;
    GPIO.out_w1ts = dataMaskFor(value);
    GPIO.out_w1ts = WR_BIT;
}

void rawCommand(uint8_t command) {
    GPIO.out_w1tc = RS_BIT;
    rawWrite8(command);
    GPIO.out_w1ts = RS_BIT;
}

void rawData(uint8_t data) {
    GPIO.out_w1ts = RS_BIT;
    rawWrite8(data);
}

void rawData16(uint16_t data) {
    rawData((uint8_t)(data >> 8));
    rawData((uint8_t)(data & 0xFF));
}

void rawSetWindow(uint16_t x0, uint16_t y0, uint16_t x1, uint16_t y1) {
    rawCommand(0x2A);
    rawData16(x0);
    rawData16(x1);

    rawCommand(0x2B);
    rawData16(y0);
    rawData16(y1);

    rawCommand(0x2C);
}

void rawFillRect(uint16_t x, uint16_t y, uint16_t w, uint16_t h, uint16_t color) {
    if (w == 0 || h == 0 || x >= DISPLAY_WIDTH || y >= DISPLAY_HEIGHT) {
        return;
    }
    if ((uint32_t)x + w > DISPLAY_WIDTH) w = DISPLAY_WIDTH - x;
    if ((uint32_t)y + h > DISPLAY_HEIGHT) h = DISPLAY_HEIGHT - y;

    rawSetWindow(x, y, x + w - 1, y + h - 1);
    uint8_t hi = (uint8_t)(color >> 8);
    uint8_t lo = (uint8_t)(color & 0xFF);
    for (uint32_t i = 0; i < (uint32_t)w * h; ++i) {
        rawData(hi);
        rawData(lo);
    }
}

void rawFillScreen(uint16_t color) {
    rawFillRect(0, 0, DISPLAY_WIDTH, DISPLAY_HEIGHT, color);
}

uint8_t glyphRow(char c, uint8_t row) {
    c = (char)toupper((unsigned char)c);
    switch (c) {
        case 'A': { static const uint8_t g[7] = {0x0E,0x11,0x11,0x1F,0x11,0x11,0x11}; return g[row]; }
        case 'B': { static const uint8_t g[7] = {0x1E,0x11,0x11,0x1E,0x11,0x11,0x1E}; return g[row]; }
        case 'C': { static const uint8_t g[7] = {0x0E,0x11,0x10,0x10,0x10,0x11,0x0E}; return g[row]; }
        case 'D': { static const uint8_t g[7] = {0x1E,0x11,0x11,0x11,0x11,0x11,0x1E}; return g[row]; }
        case 'E': { static const uint8_t g[7] = {0x1F,0x10,0x10,0x1E,0x10,0x10,0x1F}; return g[row]; }
        case 'F': { static const uint8_t g[7] = {0x1F,0x10,0x10,0x1E,0x10,0x10,0x10}; return g[row]; }
        case 'G': { static const uint8_t g[7] = {0x0E,0x11,0x10,0x17,0x11,0x11,0x0F}; return g[row]; }
        case 'H': { static const uint8_t g[7] = {0x11,0x11,0x11,0x1F,0x11,0x11,0x11}; return g[row]; }
        case 'I': { static const uint8_t g[7] = {0x1F,0x04,0x04,0x04,0x04,0x04,0x1F}; return g[row]; }
        case 'J': { static const uint8_t g[7] = {0x01,0x01,0x01,0x01,0x11,0x11,0x0E}; return g[row]; }
        case 'K': { static const uint8_t g[7] = {0x11,0x12,0x14,0x18,0x14,0x12,0x11}; return g[row]; }
        case 'L': { static const uint8_t g[7] = {0x10,0x10,0x10,0x10,0x10,0x10,0x1F}; return g[row]; }
        case 'M': { static const uint8_t g[7] = {0x11,0x1B,0x15,0x15,0x11,0x11,0x11}; return g[row]; }
        case 'N': { static const uint8_t g[7] = {0x11,0x19,0x15,0x13,0x11,0x11,0x11}; return g[row]; }
        case 'O': { static const uint8_t g[7] = {0x0E,0x11,0x11,0x11,0x11,0x11,0x0E}; return g[row]; }
        case 'P': { static const uint8_t g[7] = {0x1E,0x11,0x11,0x1E,0x10,0x10,0x10}; return g[row]; }
        case 'Q': { static const uint8_t g[7] = {0x0E,0x11,0x11,0x11,0x15,0x12,0x0D}; return g[row]; }
        case 'R': { static const uint8_t g[7] = {0x1E,0x11,0x11,0x1E,0x14,0x12,0x11}; return g[row]; }
        case 'S': { static const uint8_t g[7] = {0x0F,0x10,0x10,0x0E,0x01,0x01,0x1E}; return g[row]; }
        case 'T': { static const uint8_t g[7] = {0x1F,0x04,0x04,0x04,0x04,0x04,0x04}; return g[row]; }
        case 'U': { static const uint8_t g[7] = {0x11,0x11,0x11,0x11,0x11,0x11,0x0E}; return g[row]; }
        case 'V': { static const uint8_t g[7] = {0x11,0x11,0x11,0x11,0x0A,0x0A,0x04}; return g[row]; }
        case 'W': { static const uint8_t g[7] = {0x11,0x11,0x11,0x15,0x15,0x1B,0x11}; return g[row]; }
        case 'X': { static const uint8_t g[7] = {0x11,0x0A,0x04,0x04,0x04,0x0A,0x11}; return g[row]; }
        case 'Y': { static const uint8_t g[7] = {0x11,0x0A,0x04,0x04,0x04,0x04,0x04}; return g[row]; }
        case 'Z': { static const uint8_t g[7] = {0x1F,0x02,0x04,0x04,0x08,0x10,0x1F}; return g[row]; }
        case '0': { static const uint8_t g[7] = {0x0E,0x11,0x13,0x15,0x19,0x11,0x0E}; return g[row]; }
        case '1': { static const uint8_t g[7] = {0x04,0x0C,0x04,0x04,0x04,0x04,0x0E}; return g[row]; }
        case '2': { static const uint8_t g[7] = {0x0E,0x11,0x01,0x02,0x04,0x08,0x1F}; return g[row]; }
        case '3': { static const uint8_t g[7] = {0x1E,0x01,0x01,0x0E,0x01,0x01,0x1E}; return g[row]; }
        case '4': { static const uint8_t g[7] = {0x02,0x06,0x0A,0x12,0x1F,0x02,0x02}; return g[row]; }
        case '5': { static const uint8_t g[7] = {0x1F,0x10,0x10,0x1E,0x01,0x01,0x1E}; return g[row]; }
        case '6': { static const uint8_t g[7] = {0x0E,0x10,0x10,0x1E,0x11,0x11,0x0E}; return g[row]; }
        case '7': { static const uint8_t g[7] = {0x1F,0x01,0x02,0x04,0x08,0x08,0x08}; return g[row]; }
        case '8': { static const uint8_t g[7] = {0x0E,0x11,0x11,0x0E,0x11,0x11,0x0E}; return g[row]; }
        case '9': { static const uint8_t g[7] = {0x0E,0x11,0x11,0x0F,0x01,0x01,0x0E}; return g[row]; }
        case '-': { static const uint8_t g[7] = {0x00,0x00,0x00,0x1F,0x00,0x00,0x00}; return g[row]; }
        case '.': { static const uint8_t g[7] = {0x00,0x00,0x00,0x00,0x00,0x0C,0x0C}; return g[row]; }
        case ':': { static const uint8_t g[7] = {0x00,0x0C,0x0C,0x00,0x0C,0x0C,0x00}; return g[row]; }
        case '%': { static const uint8_t g[7] = {0x19,0x1A,0x02,0x04,0x08,0x0B,0x13}; return g[row]; }
        case '/': { static const uint8_t g[7] = {0x01,0x02,0x02,0x04,0x08,0x08,0x10}; return g[row]; }
        case '|': { static const uint8_t g[7] = {0x04,0x04,0x04,0x04,0x04,0x04,0x04}; return g[row]; }
        default: return 0x00;
    }
}

void drawChar(int x, int y, char c, uint16_t color, uint16_t bg, uint8_t scale) {
    for (uint8_t row = 0; row < 7; ++row) {
        uint8_t bits = glyphRow(c, row);
        for (uint8_t col = 0; col < 5; ++col) {
            uint16_t pixelColor = (bits & (1 << (4 - col))) ? color : bg;
            rawFillRect(x + col * scale, y + row * scale, scale, scale, pixelColor);
        }
    }
}

void drawText(int x, int y, const char* text, uint16_t color, uint16_t bg, uint8_t scale) {
    int cursorX = x;
    while (*text != '\0') {
        drawChar(cursorX, y, *text, color, bg, scale);
        cursorX += 6 * scale;
        ++text;
    }
}

uint16_t tempColor(float tempC) {
    if (isnan(tempC)) return COLOR_WARN;
    if (tempC < TEMP_MIN_C || tempC > TEMP_MAX_C) return COLOR_BAD;
    return COLOR_OK;
}

void drawCard(int x, int y, int w, int h, const char* label, const char* value,
              uint16_t valueColor, bool alt = false) {
    uint16_t bg = alt ? COLOR_PANEL_ALT : COLOR_PANEL;
    rawFillRect(x, y, w, h, bg);
    rawFillRect(x, y, w, 3, COLOR_ACCENT);
    drawText(x + 8, y + 10, label, COLOR_MUTED, bg, 2);
    drawText(x + 8, y + 36, value, valueColor, bg, 3);
}

void drawDashboard(const DisplayState& state) {
    rawFillScreen(COLOR_BG);
    rawFillRect(0, 0, DISPLAY_WIDTH, 54, COLOR_ACCENT);
    drawText(12, 12, "DIA-SMART", COLOR_BG, COLOR_ACCENT, 4);

    char tempBuf[24];
    if (!state.hasTelemetry || isnan(state.temperatureC)) {
        snprintf(tempBuf, sizeof(tempBuf), "--.- C");
    } else {
        snprintf(tempBuf, sizeof(tempBuf), "%.1f C", state.temperatureC);
    }
    drawCard(10, 68, 145, 82, "TEMP", tempBuf, tempColor(state.temperatureC));

    const char* doorValue = state.hasTelemetry ? (state.doorOpen ? "OPEN" : "CLOSED") : "--";
    drawCard(165, 68, 145, 82, "DOOR", doorValue, state.doorOpen ? COLOR_WARN : COLOR_OK, true);

    char stockBuf[24];
    if (state.hasTelemetry) {
        snprintf(stockBuf, sizeof(stockBuf), "%.0f%%", state.estimatedPercent);
    } else {
        snprintf(stockBuf, sizeof(stockBuf), "--%%");
    }
    drawCard(10, 162, 145, 82, "STOCK", stockBuf,
             state.estimatedPercent < 20.0f ? COLOR_WARN : COLOR_TEXT, true);

    char weightBuf[24];
    if (state.hasTelemetry) {
        snprintf(weightBuf, sizeof(weightBuf), "%.0fG", state.inventoryWeightG);
    } else {
        snprintf(weightBuf, sizeof(weightBuf), "--G");
    }
    drawCard(165, 162, 145, 82, "WEIGHT", weightBuf, COLOR_TEXT);

    char glucoseBuf[24];
    if (state.hasTelemetry && state.glucoseMgDl > 0) {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "%d", state.glucoseMgDl);
    } else {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "--");
    }
    drawCard(10, 256, 145, 82, "GLUCOSE", glucoseBuf, COLOR_TEXT);

    char doseBuf[24];
    if (state.hasTelemetry && state.doseUnits > 0.0f) {
        snprintf(doseBuf, sizeof(doseBuf), "%.1fU", state.doseUnits);
    } else {
        snprintf(doseBuf, sizeof(doseBuf), "--");
    }
    drawCard(165, 256, 145, 82, "DOSE", doseBuf, COLOR_TEXT, true);

    rawFillRect(10, 350, 300, 76, COLOR_PANEL);
    drawText(20, 360, "LAST DOSE TIME", COLOR_MUTED, COLOR_PANEL, 2);
    if (state.hasTelemetry && state.doseUnits > 0.0f) {
        drawText(20, 386, state.injectedAt, COLOR_TEXT, COLOR_PANEL, 2);
    } else {
        drawText(20, 386, "NO DOSE YET", COLOR_TEXT, COLOR_PANEL, 2);
    }

    rawFillRect(0, 438, DISPLAY_WIDTH, 42, COLOR_BG);
    char footer[48];
    if (state.hasTelemetry) {
        snprintf(footer, sizeof(footer), "WIFI %d | BLE %d | HEAP %luK",
                 state.wifiRssiDbm,
                 state.bleRssiDbm,
                 (unsigned long)(state.freeHeapBytes / 1024));
    } else {
        snprintf(footer, sizeof(footer), "WAITING FOR TELEMETRY");
    }
    drawText(10, 450, footer, COLOR_MUTED, COLOR_BG, 1);
}

void rawBusInit() {
    pinMode(DISPLAY_PIN_LCD_CS, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_RS, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_RST, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_WR, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D0, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D1, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D2, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D3, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D4, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D5, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D6, OUTPUT);
    pinMode(DISPLAY_PIN_LCD_D7, OUTPUT);

    GPIO.out_w1ts = CS_BIT | RS_BIT | WR_BIT | RST_BIT;
    delay(20);
    GPIO.out_w1tc = RST_BIT;
    delay(50);
    GPIO.out_w1ts = RST_BIT;
    delay(150);
    GPIO.out_w1tc = CS_BIT;
}

void rawDisplayInit() {
    rawBusInit();
    rawCommand(0x01);
    delay(150);
    rawCommand(0x11);
    delay(150);
    rawCommand(0x3A);
    rawData(0x55);
    rawCommand(0x36);
    rawData(0x40); // Portrait, corrected RGB order for the verified bus.
    rawCommand(0x13);
    rawCommand(0x29);
    delay(80);
}
}

void displayUiTask(void* parameter) {
    (void)parameter;
    Serial.println("[Display] Raw portrait dashboard task started");
    rawDisplayInit();
    rawFillScreen(COLOR_BG);

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
