#include <Arduino.h>
#include <ctype.h>
#include <math.h>
#include <string.h>

#include "config/app_config.h"
#include "managers/display_state_manager.h"
#include "services/care_plan_service.h"

#if DISPLAY_ENABLED

#include "soc/gpio_struct.h"
#include "../../../common/config/wifi_provisioning_security.h"

namespace {
// Calm, high-contrast patient palette. Red and amber are reserved for states
// that require attention; normal navigation never depends on colour alone.
constexpr uint16_t COLOR_BG = 0x0843;
constexpr uint16_t COLOR_HEADER = 0x1085;
constexpr uint16_t COLOR_PANEL = 0x18E7;
constexpr uint16_t COLOR_PANEL_ALT = 0x2129;
constexpr uint16_t COLOR_BORDER = 0x31CB;
constexpr uint16_t COLOR_TEXT = 0xFFFF;
constexpr uint16_t COLOR_MUTED = 0xA556;
constexpr uint16_t COLOR_ACCENT = 0x2E9A;
constexpr uint16_t COLOR_ACCENT_DARK = 0x1450;
constexpr uint16_t COLOR_OK = 0x364D;
constexpr uint16_t COLOR_OK_DARK = 0x1327;
constexpr uint16_t COLOR_WARN = 0xFD20;
constexpr uint16_t COLOR_WARN_DARK = 0x6200;
constexpr uint16_t COLOR_BAD = 0xF9C7;
constexpr uint16_t COLOR_BAD_DARK = 0x5804;
constexpr uint16_t COLOR_COLD = 0x4D7F;

bool forceFullRedraw = true;
constexpr uint8_t DISPLAY_MODE_DOSE_PROMPT = 100;
constexpr uint8_t DISPLAY_MODE_NOTICE = 101;
constexpr uint32_t DISPLAY_NOTICE_DURATION_MS = 3500;

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

uint8_t visibleMode(const DisplayState& state) {
    if (state.dosePromptActive) {
        return DISPLAY_MODE_DOSE_PROMPT;
    }
    if (state.noticeType != DISPLAY_NOTICE_NONE &&
        (millis() - state.noticeStartedMs) < DISPLAY_NOTICE_DURATION_MS) {
        return DISPLAY_MODE_NOTICE;
    }
    return state.activePage;
}

bool textChanged(const char* left, const char* right, size_t len) {
    return strncmp(left, right, len) != 0;
}

bool displayNeedsRedraw(const DisplayState& current,
                        const DisplayState& previous,
                        const CarePlanView& carePlan,
                        const CarePlanView& previousCarePlan,
                        uint8_t currentMode,
                        uint8_t previousMode,
                        uint32_t lastDrawMs,
                        bool hasPrevious) {
    if (!hasPrevious) return true;
    if (currentMode != previousMode) return true;
    if (carePlan.revision != previousCarePlan.revision &&
        (current.dosePromptActive ||
         current.activePage == DISPLAY_PAGE_DASHBOARD ||
         current.activePage == DISPLAY_PAGE_ALERTS ||
         current.activePage == DISPLAY_PAGE_PRESCRIPTION)) {
        return true;
    }
    if ((current.activePage == DISPLAY_PAGE_DASHBOARD ||
         current.activePage == DISPLAY_PAGE_PRESCRIPTION) &&
        (carePlan.timeAvailable != previousCarePlan.timeAvailable ||
         carePlan.minutesUntilTarget != previousCarePlan.minutesUntilTarget)) {
        return true;
    }

    if (current.dosePromptActive) {
        return current.dosePromptEditing != previous.dosePromptEditing ||
               fabsf(current.promptPenDoseUnits - previous.promptPenDoseUnits) >= 0.05f ||
               current.pendingDoseUnits != previous.pendingDoseUnits ||
               current.dosePromptRemainingSec != previous.dosePromptRemainingSec ||
               textChanged(current.doseEditBuffer, previous.doseEditBuffer, sizeof(current.doseEditBuffer));
    }

    if (currentMode == DISPLAY_MODE_NOTICE) {
        return current.noticeType != previous.noticeType ||
               fabsf(current.noticeDoseUnits - previous.noticeDoseUnits) >= 0.05f ||
               current.wifiConnected != previous.wifiConnected ||
               current.mqttConnected != previous.mqttConnected;
    }

    if (current.hasTelemetry != previous.hasTelemetry ||
        current.doorOpen != previous.doorOpen ||
        fabsf(current.temperatureC - previous.temperatureC) >= 0.05f ||
        fabsf(current.estimatedPercent - previous.estimatedPercent) >= 0.5f ||
        current.glucoseMgDl != previous.glucoseMgDl ||
        current.glucometerSequenceNumber != previous.glucometerSequenceNumber ||
        fabsf(current.doseUnits - previous.doseUnits) >= 0.05f ||
        current.innerBatteryPercent != previous.innerBatteryPercent ||
        current.wifiRssiDbm != previous.wifiRssiDbm ||
        current.bleRssiDbm != previous.bleRssiDbm ||
        current.freeHeapBytes / 1024 != previous.freeHeapBytes / 1024 ||
        textChanged(current.injectedAt, previous.injectedAt, sizeof(current.injectedAt))) {
        return true;
    }

    if (current.wifiConnected != previous.wifiConnected ||
        current.mqttConnected != previous.mqttConnected ||
        current.mqttRetrying != previous.mqttRetrying ||
        current.offlineQueueReady != previous.offlineQueueReady ||
        current.offlineQueueCount != previous.offlineQueueCount ||
        current.lastPublishOk != previous.lastPublishOk ||
        current.mqttState != previous.mqttState) {
        return true;
    }

    bool agePage = current.activePage == DISPLAY_PAGE_DEVICE_STATUS ||
                   current.activePage == DISPLAY_PAGE_QUEUE_STATUS;
    return agePage && (millis() - lastDrawMs) >= 5000;
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
        case '#': { static const uint8_t g[7] = {0x0A,0x0A,0x1F,0x0A,0x1F,0x0A,0x0A}; return g[row]; }
        case '_': { static const uint8_t g[7] = {0x00,0x00,0x00,0x00,0x00,0x00,0x1F}; return g[row]; }
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

uint8_t caseSensitiveGlyphRow(char c, uint8_t row) {
    switch (c) {
        case 'a': { static const uint8_t g[7] = {0x00,0x0E,0x01,0x0F,0x11,0x13,0x0D}; return g[row]; }
        case 'e': { static const uint8_t g[7] = {0x00,0x0E,0x11,0x1F,0x10,0x11,0x0E}; return g[row]; }
        case 'i': { static const uint8_t g[7] = {0x04,0x00,0x0C,0x04,0x04,0x04,0x0E}; return g[row]; }
        case 'm': { static const uint8_t g[7] = {0x00,0x00,0x1A,0x15,0x15,0x15,0x15}; return g[row]; }
        case 'p': { static const uint8_t g[7] = {0x00,0x00,0x1E,0x11,0x1E,0x10,0x10}; return g[row]; }
        case 'r': { static const uint8_t g[7] = {0x00,0x00,0x16,0x19,0x10,0x10,0x10}; return g[row]; }
        case 't': { static const uint8_t g[7] = {0x08,0x08,0x1E,0x08,0x08,0x09,0x06}; return g[row]; }
        case 'u': { static const uint8_t g[7] = {0x00,0x00,0x11,0x11,0x11,0x13,0x0D}; return g[row]; }
        default: return glyphRow(c, row);
    }
}

void drawCaseSensitiveText(int x, int y, const char* text,
                           uint16_t color, uint16_t bg, uint8_t scale) {
    int cursorX = x;
    while (*text != '\0') {
        for (uint8_t row = 0; row < 7; ++row) {
            uint8_t bits = caseSensitiveGlyphRow(*text, row);
            for (uint8_t col = 0; col < 5; ++col) {
                uint16_t pixelColor =
                    (bits & (1 << (4 - col))) ? color : bg;
                rawFillRect(
                    cursorX + col * scale,
                    y + row * scale,
                    scale,
                    scale,
                    pixelColor);
            }
        }
        cursorX += 6 * scale;
        ++text;
    }
}

int textWidth(const char* text, uint8_t scale) {
    return (int)strlen(text) * 6 * scale;
}

void drawCenteredText(int x, int y, int w, const char* text,
                      uint16_t color, uint16_t bg, uint8_t scale) {
    int textX = x + (w - textWidth(text, scale)) / 2;
    if (textX < x) textX = x;
    drawText(textX, y, text, color, bg, scale);
}

void drawOutline(int x, int y, int w, int h, uint16_t color, uint8_t thickness = 1) {
    rawFillRect(x, y, w, thickness, color);
    rawFillRect(x, y + h - thickness, w, thickness, color);
    rawFillRect(x, y, thickness, h, color);
    rawFillRect(x + w - thickness, y, thickness, h, color);
}

void drawPanel(int x, int y, int w, int h, uint16_t bg = COLOR_PANEL) {
    rawFillRect(x, y, w, h, bg);
    drawOutline(x, y, w, h, COLOR_BORDER);
}

void drawProgressBar(int x, int y, int w, int h, uint8_t percent,
                     uint16_t fillColor, uint16_t trackColor) {
    if (percent > 100) percent = 100;
    rawFillRect(x, y, w, h, trackColor);
    int filled = ((w - 4) * percent) / 100;
    if (filled > 0) {
        rawFillRect(x + 2, y + 2, filled, h - 4, fillColor);
    }
    drawOutline(x, y, w, h, COLOR_BORDER);
}

uint16_t tempColor(float tempC) {
    if (isnan(tempC)) return COLOR_MUTED;
    if (tempC < TEMP_MIN_C) return COLOR_COLD;
    if (tempC > TEMP_MAX_C) return COLOR_BAD;
    return COLOR_OK;
}

void drawMetricCard(int x, int y, int w, int h,
                    const char* label, const char* value, const char* status,
                    uint16_t valueColor, uint16_t bg = COLOR_PANEL) {
    drawPanel(x, y, w, h, bg);
    drawText(x + 8, y + 9, label, COLOR_MUTED, bg, 1);
    drawCenteredText(x, y + 31, w, value, valueColor, bg, 2);
    drawCenteredText(x, y + h - 20, w, status, valueColor, bg, 1);
}

bool recentMs(uint32_t timestampMs, uint32_t maxAgeMs) {
    return timestampMs != 0 && (millis() - timestampMs) <= maxAgeMs;
}

uint32_t ageSeconds(uint32_t timestampMs) {
    if (timestampMs == 0) return 0;
    return (millis() - timestampMs) / 1000;
}

const char* carePlanStatusText(CarePlanScheduleStatus status) {
    switch (status) {
        case CARE_PLAN_STATUS_DUE: return "DUE NOW";
        case CARE_PLAN_STATUS_TAKEN: return "RECORDED";
        case CARE_PLAN_STATUS_MISSED: return "DOSE MISSED";
        case CARE_PLAN_STATUS_UPCOMING: return "UPCOMING";
        case CARE_PLAN_STATUS_NONE: return "NO MEDICATION PLAN";
        default: return "WAITING";
    }
}

uint16_t carePlanStatusColor(CarePlanScheduleStatus status) {
    switch (status) {
        case CARE_PLAN_STATUS_DUE: return COLOR_WARN;
        case CARE_PLAN_STATUS_TAKEN: return COLOR_OK;
        case CARE_PLAN_STATUS_MISSED: return COLOR_BAD;
        case CARE_PLAN_STATUS_UPCOMING: return COLOR_ACCENT;
        case CARE_PLAN_STATUS_NONE:
        default: return COLOR_MUTED;
    }
}

uint16_t okBadColor(bool ok) {
    return ok ? COLOR_OK : COLOR_BAD;
}

bool mqttOk(const DisplayState& state) {
    return state.mqttConnected && !state.mqttRetrying;
}

bool bleOk(const DisplayState& state) {
    return state.bleRssiDbm != 0;
}

bool innerOk(const DisplayState& state) {
    return recentMs(state.lastInnerPacketMs, 90000);
}

void drawTopBar(const DisplayState& state) {
    rawFillRect(0, 0, DISPLAY_WIDTH, 28, COLOR_HEADER);
    drawText(10, 8, "DIASMART", COLOR_ACCENT, COLOR_HEADER, 2);

    const bool connected = state.wifiConnected && mqttOk(state);
    const bool retrying = state.wifiConnected && state.mqttRetrying;
    const char* label = connected ? "CONNECTED" : (retrying ? "SYNCING" : "OFFLINE");
    uint16_t chipColor = connected ? COLOR_OK_DARK : (retrying ? COLOR_WARN_DARK : COLOR_BAD_DARK);
    int chipWidth = textWidth(label, 1) + 20;
    int chipX = DISPLAY_WIDTH - chipWidth - 10;
    rawFillRect(chipX, 5, chipWidth, 18, chipColor);
    rawFillRect(chipX + 6, 11, 6, 6, connected ? COLOR_OK : (retrying ? COLOR_WARN : COLOR_BAD));
    drawText(chipX + 16, 10, label, COLOR_TEXT, chipColor, 1);
}

void drawPageTitle(const DisplayState& state, const char* title) {
    drawTopBar(state);
    rawFillRect(0, 28, DISPLAY_WIDTH, 32, COLOR_HEADER);
    drawText(12, 36, title, COLOR_TEXT, COLOR_HEADER, 2);
    rawFillRect(0, 58, DISPLAY_WIDTH, 2, COLOR_ACCENT);
}

void drawStatusRow(int y, const char* label, const char* value, uint16_t color) {
    drawPanel(10, y, 300, 42);
    drawText(20, y + 14, label, COLOR_MUTED, COLOR_PANEL, 1);
    int valueX = 298 - textWidth(value, 2);
    if (valueX < 128) valueX = 128;
    drawText(valueX, y + 11, value, color, COLOR_PANEL, 2);
}

void drawNavigation(const char* first, const char* second) {
    rawFillRect(0, 432, DISPLAY_WIDTH, 48, COLOR_HEADER);
    rawFillRect(0, 432, DISPLAY_WIDTH, 2, COLOR_BORDER);
    drawText(12, 442, first, COLOR_TEXT, COLOR_HEADER, 1);
    drawText(12, 462, second, COLOR_MUTED, COLOR_HEADER, 1);
}

void drawDashboard(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "HOME");

    // Primary task: the next medication is always the strongest visual group.
    drawPanel(10, 70, 300, 92, COLOR_PANEL_ALT);
    drawText(20, 80, "NEXT MEDICATION", COLOR_MUTED, COLOR_PANEL_ALT, 1);
    if (carePlan.available && carePlan.scheduleCount > 0) {
        char doseText[16];
        snprintf(doseText, sizeof(doseText), "%.0fU", carePlan.doseUnits);
        drawText(20, 103, doseText,
                 carePlanStatusColor(carePlan.status), COLOR_PANEL_ALT, 4);

        char insulinText[24];
        snprintf(insulinText, sizeof(insulinText), "%.18s", carePlan.insulinType);
        drawText(104, 101, insulinText, COLOR_TEXT, COLOR_PANEL_ALT, 2);

        char timingText[40];
        if (carePlan.status == CARE_PLAN_STATUS_UPCOMING && carePlan.timeAvailable) {
            if (carePlan.minutesUntilTarget < 60) {
                snprintf(timingText, sizeof(timingText), "%s  DUE IN %u MIN",
                         carePlan.targetTime, carePlan.minutesUntilTarget);
            } else {
                snprintf(timingText, sizeof(timingText), "%s  DUE IN %uH %uM",
                         carePlan.targetTime,
                         carePlan.minutesUntilTarget / 60,
                         carePlan.minutesUntilTarget % 60);
            }
        } else {
            snprintf(timingText, sizeof(timingText), "%s  %s",
                     carePlan.targetTime, carePlanStatusText(carePlan.status));
        }
        drawText(104, 130, timingText,
                 carePlanStatusColor(carePlan.status), COLOR_PANEL_ALT, 1);
    } else {
        drawText(20, 105, "NO MEDICATION PLAN", COLOR_TEXT, COLOR_PANEL_ALT, 2);
        drawText(20, 135,
                 state.wifiConnected ? "CHECK THE DIASMART APP" : "CONNECT TO SYNC A PLAN",
                 COLOR_MUTED, COLOR_PANEL_ALT, 1);
    }

    drawText(12, 174, "INSULIN STORAGE", COLOR_MUTED, COLOR_BG, 1);

    char tempBuf[16];
    const char* tempStatus = "WAITING";
    if (!state.hasTelemetry || isnan(state.temperatureC)) {
        snprintf(tempBuf, sizeof(tempBuf), "--.- C");
    } else {
        snprintf(tempBuf, sizeof(tempBuf), "%.1f C", state.temperatureC);
        tempStatus = state.temperatureC < TEMP_MIN_C
            ? "TOO COLD"
            : (state.temperatureC > TEMP_MAX_C ? "TOO WARM" : "SAFE");
    }
    drawMetricCard(10, 190, 96, 90, "TEMP", tempBuf, tempStatus,
                   tempColor(state.temperatureC));

    const char* doorValue = state.hasTelemetry
        ? (state.doorOpen ? "OPEN" : "CLOSED")
        : "--";
    const char* doorStatus = !state.hasTelemetry
        ? "WAITING"
        : (state.doorOpen ? "CLOSE DOOR" : "SECURE");
    drawMetricCard(112, 190, 96, 90, "DOOR", doorValue, doorStatus,
                   state.doorOpen ? COLOR_WARN : (state.hasTelemetry ? COLOR_OK : COLOR_MUTED),
                   COLOR_PANEL_ALT);

    char stockBuf[16];
    const bool lowStock = state.hasTelemetry && state.estimatedPercent < 20.0f;
    if (state.hasTelemetry) {
        snprintf(stockBuf, sizeof(stockBuf), "%.0f%%", state.estimatedPercent);
    } else {
        snprintf(stockBuf, sizeof(stockBuf), "--%%");
    }
    drawMetricCard(214, 190, 96, 90, "AVAILABLE", stockBuf,
                   !state.hasTelemetry ? "WAITING" : (lowStock ? "LOW" : "GOOD"),
                   lowStock ? COLOR_WARN : (state.hasTelemetry ? COLOR_OK : COLOR_MUTED));
    if (state.hasTelemetry) {
        uint8_t stockPercent = (uint8_t)lroundf(
            state.estimatedPercent < 0.0f ? 0.0f :
            (state.estimatedPercent > 100.0f ? 100.0f : state.estimatedPercent));
        drawProgressBar(224, 250, 76, 8, stockPercent,
                        lowStock ? COLOR_WARN : COLOR_OK, COLOR_HEADER);
    }

    char glucoseBuf[16];
    if (state.hasTelemetry && state.glucoseMgDl > 0) {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "%d", state.glucoseMgDl);
    } else {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "--");
    }
    drawPanel(10, 292, 145, 78);
    drawText(20, 302, "LATEST GLUCOSE", COLOR_MUTED, COLOR_PANEL, 1);
    drawText(20, 326, glucoseBuf, COLOR_TEXT, COLOR_PANEL, 3);
    drawText(88, 338, "MG/DL", COLOR_MUTED, COLOR_PANEL, 1);

    char doseBuf[16];
    if (state.hasTelemetry && state.doseUnits > 0.0f) {
        snprintf(doseBuf, sizeof(doseBuf), "%.0fU", state.doseUnits);
    } else {
        snprintf(doseBuf, sizeof(doseBuf), "--");
    }
    drawPanel(165, 292, 145, 78, COLOR_PANEL_ALT);
    drawText(175, 302, "LAST DOSE", COLOR_MUTED, COLOR_PANEL_ALT, 1);
    drawText(175, 326, doseBuf, COLOR_TEXT, COLOR_PANEL_ALT, 3);

    bool attention = state.doorOpen || lowStock ||
                     (state.hasTelemetry && !isnan(state.temperatureC) &&
                      (state.temperatureC < TEMP_MIN_C || state.temperatureC > TEMP_MAX_C)) ||
                     (state.hasTelemetry &&
                      state.innerBatteryPercent <= INNER_BATTERY_LOW_PERCENT) ||
                     !state.wifiConnected || !mqttOk(state) ||
                     state.offlineQueueCount > 0;
    uint16_t bannerBg = attention ? COLOR_WARN_DARK : COLOR_OK_DARK;
    rawFillRect(10, 382, 300, 36, bannerBg);
    if (!state.hasTelemetry) {
        drawText(20, 395, "WAITING FOR STORAGE UNIT", COLOR_WARN, bannerBg, 1);
    } else if (state.doorOpen) {
        drawText(20, 395, "ACTION NEEDED  CLOSE STORAGE DOOR", COLOR_WARN, bannerBg, 1);
    } else if (!isnan(state.temperatureC) &&
               (state.temperatureC < TEMP_MIN_C || state.temperatureC > TEMP_MAX_C)) {
        drawText(20, 395, "ACTION NEEDED  CHECK TEMPERATURE", COLOR_WARN, bannerBg, 1);
    } else if (lowStock) {
        drawText(20, 395, "ACTION NEEDED  INSULIN IS LOW", COLOR_WARN, bannerBg, 1);
    } else if (state.innerBatteryPercent <= INNER_BATTERY_LOW_PERCENT) {
        drawText(20, 395, "ACTION NEEDED  STORAGE BATTERY LOW", COLOR_WARN, bannerBg, 1);
    } else if (!state.wifiConnected || !mqttOk(state) ||
               state.offlineQueueCount > 0) {
        drawText(20, 395, "SYNC DELAYED  RECORDS ARE SAFE", COLOR_WARN, bannerBg, 1);
    } else if (attention) {
        drawText(20, 395, "CHECK INSULIN STORAGE", COLOR_WARN, bannerBg, 1);
    } else {
        drawText(20, 395, "ALL SYSTEMS READY", COLOR_OK, bannerBg, 1);
    }

    drawNavigation("A HOME    B MEDICATION",
                   "C ALERTS  D DEVICE");
}

void drawSystemStatus(const DisplayState& state) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "DEVICE");

    char value[32];
    drawStatusRow(72,
                  "INTERNET",
                  state.wifiConnected ? "CONNECTED" : "UNAVAILABLE",
                  state.wifiConnected ? COLOR_OK : COLOR_BAD);

    const char* cloudStatus = state.mqttRetrying
        ? "WILL RETRY"
        : (state.mqttConnected ? "SYNCED" : "UNAVAILABLE");
    drawStatusRow(120,
                  "DIASMART CLOUD",
                  cloudStatus,
                  mqttOk(state) ? COLOR_OK : (state.mqttRetrying ? COLOR_WARN : COLOR_BAD));

    drawStatusRow(168,
                  "SMART PEN",
                  bleOk(state) ? "CONNECTED" : "WAITING",
                  bleOk(state) ? COLOR_OK : COLOR_WARN);

    drawStatusRow(216,
                  "STORAGE UNIT",
                  innerOk(state) ? "CONNECTED" : "WAITING",
                  innerOk(state) ? COLOR_OK : COLOR_WARN);

    if (state.hasTelemetry) {
        snprintf(value, sizeof(value), "%d%%", state.innerBatteryPercent);
    } else {
        snprintf(value, sizeof(value), "--");
    }
    drawStatusRow(264,
                  "STORAGE BATTERY",
                  value,
                  state.hasTelemetry &&
                          state.innerBatteryPercent <= INNER_BATTERY_LOW_PERCENT
                      ? COLOR_BAD
                      : COLOR_TEXT);

    snprintf(value, sizeof(value), "%u", state.offlineQueueCount);
    drawStatusRow(312,
                  "RECORDS TO SYNC",
                  value,
                  state.offlineQueueCount > 0 ? COLOR_WARN : COLOR_OK);

    drawPanel(10, 370, 300, 46, COLOR_ACCENT_DARK);
    drawText(20, 383, "# SETUP AND DEVICE IDS",
             COLOR_ACCENT, COLOR_ACCENT_DARK, 1);

    drawNavigation("A HOME    B MEDICATION",
                   "C ALERTS  # SETUP");
}

void drawDeviceSetup(const DisplayState& state) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "DEVICE SETUP");

    drawPanel(10, 72, 300, 170, COLOR_PANEL_ALT);
    drawText(20, 84, "ENTER THESE IDS IN THE APP",
             COLOR_ACCENT, COLOR_PANEL_ALT, 1);
    char idLine[40];
    snprintf(idLine, sizeof(idLine), "OUTER   %s", DEVICE_UID_OUTER);
    drawText(20, 112, idLine, COLOR_TEXT, COLOR_PANEL_ALT, 2);
    snprintf(idLine, sizeof(idLine), "INNER   %s", DEVICE_UID_INNER);
    drawText(20, 144, idLine, COLOR_TEXT, COLOR_PANEL_ALT, 2);
    snprintf(idLine, sizeof(idLine), "PEN     %s", DEVICE_UID_PEN);
    drawText(20, 176, idLine, COLOR_TEXT, COLOR_PANEL_ALT, 2);
    snprintf(idLine, sizeof(idLine), "METER   %s", DEVICE_UID_GLUCOMETER);
    drawText(20, 208, idLine, COLOR_TEXT, COLOR_PANEL_ALT, 2);

    drawPanel(10, 254, 300, 158, COLOR_PANEL);
    drawText(20, 266, "CONNECT PHONE TO SETUP WIFI",
             COLOR_ACCENT, COLOR_PANEL, 1);

    drawText(20, 292, "NETWORK", COLOR_MUTED, COLOR_PANEL, 1);
    char setupSsid[40];
    snprintf(setupSsid, sizeof(setupSsid), "DiaSmart-%s", DEVICE_UID_OUTER);
    drawCaseSensitiveText(20, 310, setupSsid,
                          COLOR_TEXT, COLOR_PANEL, 2);

    drawText(20, 344, "PASSWORD", COLOR_MUTED, COLOR_PANEL, 1);
    drawCaseSensitiveText(20, 364, DIASMART_SETUP_AP_PASSWORD,
                          COLOR_WARN, COLOR_PANEL, 2);
    drawText(20, 394, "THEN RETURN TO THE DIASMART APP",
             COLOR_MUTED, COLOR_PANEL, 1);

    drawNavigation("A HOME    D DEVICE",
                   "USE THESE DETAILS IN SETUP");
}

void drawAlertItem(int y, const char* title, const char* action,
                   uint16_t color, uint16_t bg) {
    drawPanel(10, y, 300, 50, bg);
    rawFillRect(10, y, 5, 50, color);
    drawText(24, y + 8, title, color, bg, 2);
    drawText(24, y + 32, action, COLOR_TEXT, bg, 1);
}

void drawAlerts(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "ALERTS");

    int y = 72;
    uint8_t alertCount = 0;

    if (carePlan.available && carePlan.status == CARE_PLAN_STATUS_MISSED) {
        drawAlertItem(y, "DOSE MISSED", "CHECK YOUR MEDICATION PLAN",
                      COLOR_BAD, COLOR_BAD_DARK);
        y += 56;
        alertCount++;
    } else if (carePlan.available && carePlan.status == CARE_PLAN_STATUS_DUE) {
        drawAlertItem(y, "MEDICATION DUE", "OPEN MEDICATION FOR DETAILS",
                      COLOR_WARN, COLOR_WARN_DARK);
        y += 56;
        alertCount++;
    }

    if (state.hasTelemetry && !isnan(state.temperatureC) &&
        (state.temperatureC < TEMP_MIN_C || state.temperatureC > TEMP_MAX_C)) {
        drawAlertItem(y,
                      state.temperatureC < TEMP_MIN_C
                          ? "STORAGE TOO COLD"
                          : "STORAGE TOO WARM",
                      "CHECK THE INSULIN STORAGE UNIT",
                      state.temperatureC < TEMP_MIN_C ? COLOR_COLD : COLOR_BAD,
                      COLOR_PANEL_ALT);
        y += 56;
        alertCount++;
    }

    if (state.hasTelemetry && state.doorOpen) {
        drawAlertItem(y, "STORAGE DOOR OPEN", "CLOSE THE STORAGE DOOR",
                      COLOR_WARN, COLOR_WARN_DARK);
        y += 56;
        alertCount++;
    }

    if (state.hasTelemetry && state.estimatedPercent < 20.0f) {
        drawAlertItem(y, "INSULIN SUPPLY LOW", "REFILL INSULIN SOON",
                      COLOR_WARN, COLOR_WARN_DARK);
        y += 56;
        alertCount++;
    }

    if (state.hasTelemetry &&
        state.innerBatteryPercent <= INNER_BATTERY_LOW_PERCENT) {
        drawAlertItem(y, "STORAGE BATTERY LOW", "CHARGE THE STORAGE UNIT",
                      COLOR_WARN, COLOR_WARN_DARK);
        y += 56;
        alertCount++;
    }

    if (!state.wifiConnected || !mqttOk(state) || state.offlineQueueCount > 0) {
        drawAlertItem(y, "SYNC IS DELAYED", "RECORDS WILL SYNC WHEN ONLINE",
                      COLOR_WARN, COLOR_PANEL_ALT);
        y += 56;
        alertCount++;
    }

    if (!state.hasTelemetry && alertCount == 0) {
        drawAlertItem(y, "STORAGE UNIT WAITING", "KEEP BOTH UNITS POWERED ON",
                      COLOR_WARN, COLOR_PANEL_ALT);
        alertCount++;
    }

    if (alertCount == 0) {
        drawPanel(10, 96, 300, 176, COLOR_OK_DARK);
        drawCenteredText(10, 126, 300, "EVERYTHING", COLOR_OK, COLOR_OK_DARK, 3);
        drawCenteredText(10, 164, 300, "LOOKS GOOD", COLOR_OK, COLOR_OK_DARK, 3);
        drawCenteredText(10, 226, 300, "NO ACTION IS NEEDED",
                         COLOR_TEXT, COLOR_OK_DARK, 1);
    }

    drawNavigation("A HOME    B MEDICATION",
                   "C ALERTS  D DEVICE");
}

void drawQueueStatus(const DisplayState& state) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "SERVICE DIAGNOSTICS");

    char value[32];
    snprintf(value, sizeof(value), "%u", state.offlineQueueCount);
    drawStatusRow(72, "QUEUED RECORDS", value,
                  state.offlineQueueCount > 0 ? COLOR_WARN : COLOR_OK);

    drawStatusRow(120, "QUEUE STORAGE",
                  state.offlineQueueReady ? "READY" : "NOT READY",
                  state.offlineQueueReady ? COLOR_OK : COLOR_BAD);

    drawStatusRow(168, "LAST PUBLISH",
                  state.lastPublishOk ? "OK" : "FAILED",
                  state.lastPublishOk ? COLOR_OK : COLOR_BAD);

    drawStatusRow(216, "MQTT",
                  state.mqttRetrying
                      ? "RETRYING"
                      : (state.mqttConnected ? "CONNECTED" : "OFFLINE"),
                  mqttOk(state) ? COLOR_OK : (state.mqttRetrying ? COLOR_WARN : COLOR_BAD));

    if (state.offlineQueueCount > 0 && state.offlineQueueOldestMs != 0) {
        snprintf(value, sizeof(value), "%luS", (unsigned long)ageSeconds(state.offlineQueueOldestMs));
    } else {
        snprintf(value, sizeof(value), "0S");
    }
    drawStatusRow(264, "OLDEST AGE", value,
                  state.offlineQueueCount > 0 ? COLOR_WARN : COLOR_OK);

    drawPanel(10, 326, 300, 66, COLOR_PANEL_ALT);
    drawText(20, 340, "TECHNICIAN VIEW", COLOR_ACCENT, COLOR_PANEL_ALT, 2);
    drawText(20, 372, "PRESS A TO RETURN HOME", COLOR_MUTED, COLOR_PANEL_ALT, 1);

    drawNavigation("A HOME",
                   "0 SERVICE DIAGNOSTICS");
}

void drawPrescription(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "MEDICATION");

    if (!carePlan.available || carePlan.scheduleCount == 0) {
        drawPanel(10, 86, 300, 190, COLOR_PANEL_ALT);
        drawCenteredText(10, 116, 300, "NO MEDICATION", COLOR_TEXT,
                         COLOR_PANEL_ALT, 3);
        drawCenteredText(10, 150, 300, "PLAN AVAILABLE", COLOR_TEXT,
                         COLOR_PANEL_ALT, 3);
        if (state.wifiConnected && mqttOk(state)) {
            drawCenteredText(10, 210, 300, "CHECK THE DIASMART APP",
                             COLOR_MUTED, COLOR_PANEL_ALT, 1);
            drawCenteredText(10, 230, 300, "OR ASK YOUR CARE TEAM",
                             COLOR_MUTED, COLOR_PANEL_ALT, 1);
        } else {
            drawCenteredText(10, 210, 300, "OFFLINE - CONNECT TO SYNC",
                             COLOR_WARN, COLOR_PANEL_ALT, 1);
            drawCenteredText(10, 230, 300, "A MEDICATION PLAN",
                             COLOR_MUTED, COLOR_PANEL_ALT, 1);
        }
        drawPanel(10, 294, 300, 70, COLOR_PANEL);
        drawText(20, 310, "WHAT TO DO", COLOR_ACCENT, COLOR_PANEL, 1);
        drawText(20, 338, "KEEP THE DEVICE POWERED ON", COLOR_TEXT, COLOR_PANEL, 1);
        drawNavigation("A HOME    B MEDICATION",
                       "C ALERTS  D DEVICE");
        return;
    }

    uint16_t statusColor = carePlanStatusColor(carePlan.status);
    uint16_t statusBg = carePlan.status == CARE_PLAN_STATUS_MISSED
        ? COLOR_BAD_DARK
        : (carePlan.status == CARE_PLAN_STATUS_DUE
            ? COLOR_WARN_DARK
            : (carePlan.status == CARE_PLAN_STATUS_TAKEN
                ? COLOR_OK_DARK
                : COLOR_PANEL_ALT));
    rawFillRect(10, 70, 300, 42, statusBg);
    drawText(20, 84, carePlanStatusText(carePlan.status),
             statusColor, statusBg, 2);
    char countText[16];
    snprintf(countText, sizeof(countText), "DOSE %u/%u",
             carePlan.selectedScheduleIndex + 1,
             carePlan.scheduleCount);
    int countX = 300 - textWidth(countText, 1);
    drawText(countX, 88, countText, COLOR_MUTED, statusBg, 1);

    drawPanel(10, 124, 300, 100, COLOR_PANEL_ALT);
    drawText(20, 136, "PRESCRIBED", COLOR_MUTED, COLOR_PANEL_ALT, 1);
    char doseText[20];
    snprintf(doseText, sizeof(doseText), "%.1fU", carePlan.doseUnits);
    drawText(20, 166, doseText, statusColor, COLOR_PANEL_ALT, 3);

    char insulinText[24];
    snprintf(insulinText, sizeof(insulinText), "%.14s", carePlan.insulinType);
    drawText(132, 158, insulinText, COLOR_TEXT, COLOR_PANEL_ALT, 2);
    drawText(132, 190, carePlan.period, COLOR_MUTED, COLOR_PANEL_ALT, 1);

    drawPanel(10, 236, 145, 68, COLOR_PANEL);
    drawText(20, 246, "TARGET TIME", COLOR_MUTED, COLOR_PANEL, 1);
    drawCenteredText(10, 272, 145, carePlan.targetTime,
                     COLOR_TEXT, COLOR_PANEL, 2);

    drawPanel(165, 236, 145, 68, COLOR_PANEL_ALT);
    drawText(175, 246, "ALLOWED WINDOW", COLOR_MUTED, COLOR_PANEL_ALT, 1);
    char windowText[32];
    snprintf(windowText, sizeof(windowText), "%s - %s",
             carePlan.windowStart,
             carePlan.windowEnd);
    drawCenteredText(165, 272, 145, windowText,
                     COLOR_TEXT, COLOR_PANEL_ALT, 1);

    drawPanel(10, 316, 300, 78, statusBg);
    char stateText[48];
    if (carePlan.status == CARE_PLAN_STATUS_UPCOMING &&
        carePlan.timeAvailable) {
        if (carePlan.minutesUntilTarget < 60) {
            snprintf(stateText,
                     sizeof(stateText),
                     "DUE IN %u MIN",
                     carePlan.minutesUntilTarget);
        } else {
            snprintf(stateText,
                     sizeof(stateText),
                     "DUE IN %uH %uM",
                     carePlan.minutesUntilTarget / 60,
                     carePlan.minutesUntilTarget % 60);
        }
    } else if (carePlan.status == CARE_PLAN_STATUS_DUE &&
               carePlan.reminderSilenced) {
        snprintf(stateText, sizeof(stateText), "REMINDER PAUSED");
    } else if (carePlan.status == CARE_PLAN_STATUS_DUE) {
        snprintf(stateText, sizeof(stateText), "TAKE YOUR MEDICATION NOW");
    } else if (carePlan.status == CARE_PLAN_STATUS_TAKEN) {
        snprintf(stateText, sizeof(stateText), "DOSE RECORDED TODAY");
    } else if (carePlan.status == CARE_PLAN_STATUS_MISSED) {
        snprintf(stateText, sizeof(stateText), "CHECK YOUR MEDICATION PLAN");
    } else {
        snprintf(stateText, sizeof(stateText), "WAITING FOR DEVICE TIME");
    }
    drawCenteredText(10, 330, 300, stateText, statusColor, statusBg, 1);

    if (carePlan.scheduleCount > 1) {
        drawCenteredText(10, 368, 300, "* PREVIOUS    # NEXT",
                         COLOR_TEXT, statusBg, 1);
    } else {
        drawCenteredText(10, 368, 300, "TODAYS MEDICATION",
                         COLOR_MUTED, statusBg, 1);
    }

    if (carePlan.status == CARE_PLAN_STATUS_DUE &&
        carePlan.manualStopAllowed &&
        !carePlan.reminderSilenced) {
        drawNavigation("A HOME    C PAUSE REMINDER",
                       "* PREVIOUS  # NEXT");
    } else {
        drawNavigation("A HOME    B MEDICATION",
                       "C ALERTS  D DEVICE");
    }
}

void drawDosePrompt(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    uint16_t headerBg = state.dosePromptEditing ? COLOR_ACCENT_DARK : COLOR_WARN_DARK;
    uint16_t headerColor = state.dosePromptEditing ? COLOR_ACCENT : COLOR_WARN;
    rawFillRect(0, 0, DISPLAY_WIDTH, 60, headerBg);
    drawText(12, 10,
             state.dosePromptEditing ? "EDIT DOSE" : "CONFIRM DOSE",
             headerColor, headerBg, 3);
    drawText(12, 42,
             state.dosePromptEditing
                 ? "USE THE NUMBER KEYS"
                 : "CHECK BEFORE RECORDING",
             COLOR_TEXT, headerBg, 1);

    drawPanel(10, 72, 145, 92, COLOR_PANEL);
    drawText(20, 82, "DETECTED BY PEN", COLOR_MUTED, COLOR_PANEL, 1);
    char detectedBuf[16];
    snprintf(detectedBuf, sizeof(detectedBuf), "%.1fU", state.promptPenDoseUnits);
    drawCenteredText(10, 112, 145, detectedBuf, COLOR_TEXT, COLOR_PANEL, 3);

    drawPanel(165, 72, 145, 92, COLOR_PANEL_ALT);
    drawText(175, 82, "WILL RECORD", COLOR_MUTED, COLOR_PANEL_ALT, 1);
    char recordBuf[16];
    if (state.dosePromptEditing && state.doseEditBuffer[0] != '\0') {
        snprintf(recordBuf, sizeof(recordBuf), "%sU", state.doseEditBuffer);
    } else if (state.dosePromptEditing) {
        snprintf(recordBuf, sizeof(recordBuf), "_");
    } else {
        snprintf(recordBuf, sizeof(recordBuf), "%dU", state.pendingDoseUnits);
    }
    drawCenteredText(165, 112, 145, recordBuf,
                     state.dosePromptEditing ? COLOR_ACCENT : COLOR_OK,
                     COLOR_PANEL_ALT, 3);

    drawPanel(10, 176, 300, 70, COLOR_PANEL_ALT);
    drawText(20, 186, "MEDICATION PLAN", COLOR_MUTED, COLOR_PANEL_ALT, 1);
    bool hasPlan = carePlan.available && carePlan.scheduleCount > 0;
    if (hasPlan) {
        char prescribedText[24];
        snprintf(prescribedText, sizeof(prescribedText), "%.1fU  %.14s",
                 carePlan.doseUnits, carePlan.insulinType);
        drawText(20, 207, prescribedText, COLOR_TEXT, COLOR_PANEL_ALT, 2);
        char scheduleText[24];
        snprintf(scheduleText, sizeof(scheduleText), "TARGET %s", carePlan.targetTime);
        drawText(20, 230, scheduleText, COLOR_MUTED, COLOR_PANEL_ALT, 1);
    } else {
        drawText(20, 207, "NO PLAN IS LOADED", COLOR_WARN, COLOR_PANEL_ALT, 2);
        drawText(20, 230, "VERIFY THE PEN READING", COLOR_MUTED, COLOR_PANEL_ALT, 1);
    }

    int selectedUnits = state.pendingDoseUnits;
    if (state.dosePromptEditing && state.doseEditBuffer[0] != '\0') {
        selectedUnits = atoi(state.doseEditBuffer);
    }
    bool matchesPlan = hasPlan &&
        fabsf((float)selectedUnits - carePlan.doseUnits) <= 0.5f;
    uint16_t matchBg = !hasPlan
        ? COLOR_WARN_DARK
        : (matchesPlan ? COLOR_OK_DARK : COLOR_BAD_DARK);
    uint16_t matchColor = !hasPlan
        ? COLOR_WARN
        : (matchesPlan ? COLOR_OK : COLOR_BAD);
    rawFillRect(10, 258, 300, 42, matchBg);
    if (!hasPlan) {
        drawCenteredText(10, 272, 300, "NO PLAN - CHECK THE DOSE",
                         matchColor, matchBg, 1);
    } else if (matchesPlan) {
        drawCenteredText(10, 272, 300, "MATCHES MEDICATION PLAN",
                         matchColor, matchBg, 1);
    } else {
        drawCenteredText(10, 268, 300, "DOSE DIFFERS FROM PLAN",
                         matchColor, matchBg, 1);
        drawCenteredText(10, 284, 300, "CHECK BEFORE RECORDING",
                         COLOR_TEXT, matchBg, 1);
    }

    drawPanel(10, 312, 300, 66, COLOR_PANEL);
    char autoBuf[32];
    snprintf(autoBuf, sizeof(autoBuf), "AUTO RECORD IN %u SECONDS",
             state.dosePromptRemainingSec);
    drawCenteredText(10, 324, 300, autoBuf,
                     state.dosePromptRemainingSec <= 10 ? COLOR_WARN : COLOR_TEXT,
                     COLOR_PANEL, 1);
    uint32_t timeoutSeconds = DOSE_CONFIRM_TIMEOUT_MS / 1000U;
    uint8_t remainingPercent = timeoutSeconds == 0
        ? 0
        : (uint8_t)(((uint32_t)state.dosePromptRemainingSec * 100U) /
                    timeoutSeconds);
    drawProgressBar(24, 350, 272, 12, remainingPercent,
                    state.dosePromptRemainingSec <= 10 ? COLOR_WARN : COLOR_ACCENT,
                    COLOR_HEADER);

    if (state.dosePromptEditing) {
        drawPanel(10, 390, 300, 34, COLOR_ACCENT_DARK);
        drawCenteredText(10, 401, 300,
                         state.doseEditBuffer[0] != '\0'
                             ? "D SAVE   * DELETE   # CLEAR"
                             : "ENTER DOSE WITH 0-9",
                         COLOR_TEXT, COLOR_ACCENT_DARK, 1);
        drawNavigation("C BACK TO DETECTED DOSE",
                       "AUTO RECORD REMAINS ACTIVE");
    } else {
        drawPanel(10, 390, 145, 34, COLOR_OK_DARK);
        drawCenteredText(10, 401, 145, "A RECORD NOW",
                         COLOR_OK, COLOR_OK_DARK, 1);
        drawPanel(165, 390, 145, 34, COLOR_ACCENT_DARK);
        drawCenteredText(165, 401, 145, "B EDIT DOSE",
                         COLOR_ACCENT, COLOR_ACCENT_DARK, 1);
        drawNavigation("A RECORD   B EDIT",
                       "C CANCEL - DO NOT SEND");
    }
}

void drawDoseNotice(const DisplayState& state) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    const bool cancelled = state.noticeType == DISPLAY_NOTICE_DOSE_CANCELLED;
    const bool automatic = state.noticeType == DISPLAY_NOTICE_DOSE_AUTO_RECORDED;
    uint16_t noticeColor = cancelled ? COLOR_WARN : COLOR_OK;
    uint16_t noticeBg = cancelled ? COLOR_WARN_DARK : COLOR_OK_DARK;

    rawFillRect(0, 0, DISPLAY_WIDTH, 60, COLOR_HEADER);
    drawCenteredText(0, 18, DISPLAY_WIDTH, "DIASMART",
                     COLOR_ACCENT, COLOR_HEADER, 2);

    drawPanel(20, 92, 280, 220, noticeBg);
    drawCenteredText(20, 126, 280, "DOSE",
                     noticeColor, noticeBg, 3);
    drawCenteredText(20, 162, 280,
                     cancelled ? "CANCELLED" : "RECORDED",
                     noticeColor, noticeBg, 3);

    if (!cancelled) {
        char doseText[20];
        snprintf(doseText, sizeof(doseText), "%.0f UNITS", state.noticeDoseUnits);
        drawCenteredText(20, 218, 280, doseText,
                         COLOR_TEXT, noticeBg, 2);
        drawCenteredText(20, 260, 280,
                         automatic ? "RECORDED AUTOMATICALLY" : "CONFIRMED BY YOU",
                         COLOR_MUTED, noticeBg, 1);
    } else {
        drawCenteredText(20, 230, 280, "NOT SENT OR RECORDED",
                         COLOR_TEXT, noticeBg, 1);
    }

    drawPanel(20, 330, 280, 56, COLOR_PANEL);
    drawCenteredText(20, 344, 280,
                     cancelled
                         ? "RETURNING TO HOME"
                         : (state.mqttConnected
                             ? "READY TO SYNC SECURELY"
                             : "SAVED - WILL SYNC LATER"),
                     state.mqttConnected ? COLOR_OK : COLOR_WARN,
                     COLOR_PANEL, 1);
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
    rawData(0x80); // Portrait rotated 180 degrees for current enclosure side.
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
    DisplayState previousState = {};
    CarePlanView previousCarePlan = {};
    bool hasPreviousState = false;
    uint32_t lastDrawMs = 0;
    uint8_t previousMode = 0xFF;

    for (;;) {
        DisplayState state = getDisplayStateSnapshot();
        CarePlanView carePlan = getCarePlanViewSnapshot();
        uint8_t currentMode = visibleMode(state);
        if (displayNeedsRedraw(state,
                               previousState,
                               carePlan,
                               previousCarePlan,
                               currentMode,
                               previousMode,
                               lastDrawMs,
                               hasPreviousState)) {
            forceFullRedraw = !hasPreviousState || currentMode != previousMode;
            if (state.dosePromptActive) {
                drawDosePrompt(state, carePlan);
            } else if (currentMode == DISPLAY_MODE_NOTICE) {
                drawDoseNotice(state);
            } else {
                switch (state.activePage) {
                    case DISPLAY_PAGE_DEVICE_SETUP:
                        drawDeviceSetup(state);
                        break;
                    case DISPLAY_PAGE_DEVICE_STATUS:
                        drawSystemStatus(state);
                        break;
                    case DISPLAY_PAGE_ALERTS:
                        drawAlerts(state, carePlan);
                        break;
                    case DISPLAY_PAGE_QUEUE_STATUS:
                        drawQueueStatus(state);
                        break;
                    case DISPLAY_PAGE_PRESCRIPTION:
                        drawPrescription(state, carePlan);
                        break;
                    case DISPLAY_PAGE_DASHBOARD:
                    default:
                        drawDashboard(state, carePlan);
                        break;
                }
            }
            previousState = state;
            previousCarePlan = carePlan;
            hasPreviousState = true;
            lastDrawMs = millis();
            previousMode = currentMode;
        }
        vTaskDelay(pdMS_TO_TICKS(DISPLAY_REFRESH_MS));
    }
}

#else

void displayUiTask(void* parameter) {
    (void)parameter;
    vTaskDelete(nullptr);
}

#endif
