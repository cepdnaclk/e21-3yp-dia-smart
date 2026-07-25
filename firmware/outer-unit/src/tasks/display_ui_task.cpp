#include <Arduino.h>
#include <ctype.h>
#include <math.h>
#include <string.h>

#include "config/app_config.h"
#include "managers/display_state_manager.h"
#include "services/care_plan_service.h"

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
constexpr uint16_t COLOR_WARN = 0xB145;
constexpr uint16_t COLOR_BAD = 0xF800;
constexpr uint16_t COLOR_COLD = 0x041F;

bool forceFullRedraw = true;

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
    return state.dosePromptActive ? 100 : state.activePage;
}

bool textChanged(const char* left, const char* right, size_t len) {
    return strncmp(left, right, len) != 0;
}

bool displayNeedsRedraw(const DisplayState& current,
                        const DisplayState& previous,
                        const CarePlanView& carePlan,
                        const CarePlanView& previousCarePlan,
                        uint32_t lastDrawMs,
                        bool hasPrevious) {
    if (!hasPrevious) return true;
    if (visibleMode(current) != visibleMode(previous)) return true;
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

uint16_t tempColor(float tempC) {
    if (isnan(tempC)) return COLOR_MUTED;
    if (tempC < TEMP_MIN_C) return COLOR_COLD;
    if (tempC > TEMP_MAX_C) return COLOR_BAD;
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

bool recentMs(uint32_t timestampMs, uint32_t maxAgeMs) {
    return timestampMs != 0 && (millis() - timestampMs) <= maxAgeMs;
}

uint32_t ageSeconds(uint32_t timestampMs) {
    if (timestampMs == 0) return 0;
    return (millis() - timestampMs) / 1000;
}

const char* okBad(bool ok) {
    return ok ? "OK" : "BAD";
}

const char* carePlanStatusText(CarePlanScheduleStatus status) {
    switch (status) {
        case CARE_PLAN_STATUS_DUE: return "DOSE DUE";
        case CARE_PLAN_STATUS_TAKEN: return "TAKEN";
        case CARE_PLAN_STATUS_MISSED: return "MISSED";
        case CARE_PLAN_STATUS_UPCOMING: return "UPCOMING";
        case CARE_PLAN_STATUS_NONE:
        default: return "NO PLAN";
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
    bool allOk = state.wifiConnected && mqttOk(state) && bleOk(state) && innerOk(state);
    uint16_t bg = allOk ? 0x0320 : (state.offlineQueueCount > 0 || state.mqttRetrying ? 0x7BE0 : 0x7800);
    rawFillRect(0, 0, DISPLAY_WIDTH, 24, bg);

    char top[64];
    snprintf(top, sizeof(top), "WIFI %s | MQTT %s | BLE %s | IN %s | Q:%u",
             okBad(state.wifiConnected),
             okBad(mqttOk(state)),
             okBad(bleOk(state)),
             okBad(innerOk(state)),
             state.offlineQueueCount);
    drawText(6, 8, top, COLOR_TEXT, bg, 1);
}

void drawPageTitle(const DisplayState& state, const char* title) {
    drawTopBar(state);
    rawFillRect(0, 24, DISPLAY_WIDTH, 36, COLOR_ACCENT);
    drawText(12, 34, title, COLOR_BG, COLOR_ACCENT, 3);
}

void drawStatusRow(int y, const char* label, const char* value, uint16_t color) {
    rawFillRect(12, y, 296, 36, COLOR_PANEL);
    drawText(22, y + 10, label, COLOR_MUTED, COLOR_PANEL, 2);
    drawText(168, y + 10, value, color, COLOR_PANEL, 2);
}

void drawDashboard(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "HOME");

    char tempBuf[24];
    if (!state.hasTelemetry || isnan(state.temperatureC)) {
        snprintf(tempBuf, sizeof(tempBuf), "--.- C");
    } else {
        snprintf(tempBuf, sizeof(tempBuf), "%.1f C", state.temperatureC);
    }
    drawCard(10, 72, 145, 76, "TEMP", tempBuf, tempColor(state.temperatureC));

    const char* doorValue = state.hasTelemetry ? (state.doorOpen ? "OPEN" : "CLOSED") : "--";
    drawCard(165, 72, 145, 76, "DOOR", doorValue, state.doorOpen ? COLOR_WARN : COLOR_OK, true);

    char stockBuf[24];
    if (state.hasTelemetry) {
        snprintf(stockBuf, sizeof(stockBuf), "%.0f%%", state.estimatedPercent);
    } else {
        snprintf(stockBuf, sizeof(stockBuf), "--%%");
    }
    drawCard(10, 160, 145, 76, "STOCK", stockBuf,
             state.estimatedPercent < 20.0f ? COLOR_WARN : COLOR_TEXT, true);

    char glucoseBuf[24];
    if (state.hasTelemetry && state.glucoseMgDl > 0) {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "%d", state.glucoseMgDl);
    } else {
        snprintf(glucoseBuf, sizeof(glucoseBuf), "--");
    }
    drawCard(165, 160, 145, 76, "GLUCOSE", glucoseBuf, COLOR_TEXT);

    char doseBuf[24];
    if (state.hasTelemetry && state.doseUnits > 0.0f) {
        snprintf(doseBuf, sizeof(doseBuf), "%dU", (int)lroundf(state.doseUnits));
    } else {
        snprintf(doseBuf, sizeof(doseBuf), "--");
    }
    drawCard(10, 248, 145, 76, "LAST DOSE", doseBuf, COLOR_TEXT);

    char innerBatteryBuf[24];
    if (state.hasTelemetry) {
        snprintf(innerBatteryBuf, sizeof(innerBatteryBuf), "%d%%", state.innerBatteryPercent);
    } else {
        snprintf(innerBatteryBuf, sizeof(innerBatteryBuf), "--%%");
    }
    drawCard(165, 248, 145, 76, "INNER BAT", innerBatteryBuf,
             state.innerBatteryPercent <= INNER_BATTERY_LOW_PERCENT ? COLOR_BAD : COLOR_TEXT,
             true);

    rawFillRect(10, 336, 300, 72, COLOR_PANEL);
    if (carePlan.available && carePlan.scheduleCount > 0) {
        drawText(20,
                 346,
                 carePlanStatusText(carePlan.status),
                 carePlanStatusColor(carePlan.status),
                 COLOR_PANEL,
                 2);
        char prescriptionSummary[44];
        snprintf(prescriptionSummary,
                 sizeof(prescriptionSummary),
                 "%s  %.0fU  %.18s",
                 carePlan.targetTime,
                 carePlan.doseUnits,
                 carePlan.insulinType);
        drawText(20, 378, prescriptionSummary, COLOR_TEXT, COLOR_PANEL, 1);
    } else {
        drawText(20, 346, "PRESCRIPTION", COLOR_MUTED, COLOR_PANEL, 2);
        drawText(20, 378, "WAITING FOR CARE PLAN", COLOR_TEXT, COLOR_PANEL, 2);
    }

    rawFillRect(0, 424, DISPLAY_WIDTH, 56, COLOR_BG);
    drawText(12, 442, "B RX  |  C ALERTS  |  D SYSTEM", COLOR_MUTED, COLOR_BG, 1);
}

void drawSystemStatus(const DisplayState& state) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "SYSTEM");

    char value[32];
    drawStatusRow(76,
                  "WIFI",
                  state.wifiConnected ? "ONLINE" : "OFFLINE",
                  state.wifiConnected ? COLOR_OK : COLOR_BAD);

    const char* cloudStatus = state.mqttRetrying
        ? "RETRYING"
        : (state.mqttConnected ? "SYNCED" : "OFFLINE");
    drawStatusRow(122,
                  "CLOUD",
                  cloudStatus,
                  mqttOk(state) ? COLOR_OK : (state.mqttRetrying ? COLOR_WARN : COLOR_BAD));

    drawStatusRow(168,
                  "PEN",
                  bleOk(state) ? "READY" : "WAITING",
                  bleOk(state) ? COLOR_OK : COLOR_WARN);

    drawStatusRow(214,
                  "STORAGE",
                  innerOk(state) ? "READY" : "WAITING",
                  innerOk(state) ? COLOR_OK : COLOR_WARN);

    if (state.hasTelemetry) {
        snprintf(value, sizeof(value), "%d%%", state.innerBatteryPercent);
    } else {
        snprintf(value, sizeof(value), "--");
    }
    drawStatusRow(260,
                  "BATTERY",
                  value,
                  state.hasTelemetry &&
                          state.innerBatteryPercent <= INNER_BATTERY_LOW_PERCENT
                      ? COLOR_BAD
                      : COLOR_TEXT);

    snprintf(value, sizeof(value), "%u", state.offlineQueueCount);
    drawStatusRow(306,
                  "PENDING SYNC",
                  value,
                  state.offlineQueueCount > 0 ? COLOR_WARN : COLOR_OK);

    drawText(12, 442, "A HOME  |  B RX  |  C ALERTS", COLOR_MUTED, COLOR_BG, 1);
}

void drawAlerts(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "ALERTS");

    const char* doseStatus = carePlan.available
        ? carePlanStatusText(carePlan.status)
        : "NO PLAN";
    uint16_t doseColor = carePlan.available
        ? carePlanStatusColor(carePlan.status)
        : COLOR_MUTED;
    drawStatusRow(76, "DOSE", doseStatus, doseColor);

    const char* tempStatus = "OK";
    uint16_t tempStatusColor = COLOR_OK;
    if (!state.hasTelemetry || isnan(state.temperatureC)) {
        tempStatus = "WAITING";
        tempStatusColor = COLOR_MUTED;
    } else if (state.temperatureC < TEMP_MIN_C) {
        tempStatus = "LOW";
        tempStatusColor = COLOR_COLD;
    } else if (state.temperatureC > TEMP_MAX_C) {
        tempStatus = "HIGH";
        tempStatusColor = COLOR_BAD;
    }
    drawStatusRow(122, "TEMP", tempStatus, tempStatusColor);

    drawStatusRow(168,
                  "DOOR",
                  state.hasTelemetry ? (state.doorOpen ? "OPEN" : "CLOSED") : "WAITING",
                  state.doorOpen ? COLOR_WARN : (state.hasTelemetry ? COLOR_OK : COLOR_MUTED));

    bool lowStock = state.hasTelemetry && state.estimatedPercent < 20.0f;
    drawStatusRow(214,
                  "INSULIN",
                  !state.hasTelemetry ? "WAITING" : (lowStock ? "LOW" : "OK"),
                  !state.hasTelemetry ? COLOR_MUTED : (lowStock ? COLOR_WARN : COLOR_OK));

    bool offline = !state.wifiConnected || !mqttOk(state) || state.offlineQueueCount > 0;
    drawStatusRow(260,
                  "SYSTEM",
                  offline ? "CHECK" : "OK",
                  offline ? COLOR_WARN : COLOR_OK);

    bool lowBattery = state.hasTelemetry && state.innerBatteryPercent <= INNER_BATTERY_LOW_PERCENT;
    drawStatusRow(306,
                  "BATTERY",
                  !state.hasTelemetry ? "WAITING" : (lowBattery ? "LOW" : "OK"),
                  !state.hasTelemetry ? COLOR_MUTED : (lowBattery ? COLOR_WARN : COLOR_OK));

    drawText(12, 442, "A HOME  |  B RX  |  D SYSTEM", COLOR_MUTED, COLOR_BG, 1);
}

void drawQueueStatus(const DisplayState& state) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "QUEUE");

    char value[32];
    snprintf(value, sizeof(value), "%u", state.offlineQueueCount);
    drawStatusRow(76, "QUEUED", value, state.offlineQueueCount > 0 ? COLOR_WARN : COLOR_OK);

    drawStatusRow(122, "QUEUE FS", state.offlineQueueReady ? "READY" : "NOT READY",
                  state.offlineQueueReady ? COLOR_OK : COLOR_BAD);

    drawStatusRow(168, "LAST PUB", state.lastPublishOk ? "OK" : "FAIL",
                  state.lastPublishOk ? COLOR_OK : COLOR_BAD);

    drawStatusRow(214, "MQTT", state.mqttRetrying ? "RETRYING" : (state.mqttConnected ? "CONNECTED" : "OFFLINE"),
                  mqttOk(state) ? COLOR_OK : (state.mqttRetrying ? COLOR_WARN : COLOR_BAD));

    if (state.offlineQueueCount > 0 && state.offlineQueueOldestMs != 0) {
        snprintf(value, sizeof(value), "%luS", (unsigned long)ageSeconds(state.offlineQueueOldestMs));
    } else {
        snprintf(value, sizeof(value), "0S");
    }
    drawStatusRow(260, "OLDEST AGE", value, state.offlineQueueCount > 0 ? COLOR_WARN : COLOR_OK);

    drawText(12, 442, "A HOME  |  D SYSTEM", COLOR_MUTED, COLOR_BG, 1);
}

void drawPrescription(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    drawPageTitle(state, "PRESCRIPTION");

    if (!carePlan.available || carePlan.scheduleCount == 0) {
        rawFillRect(12, 82, 296, 132, COLOR_PANEL);
        drawText(30, 106, "NO ACTIVE CARE PLAN", COLOR_MUTED, COLOR_PANEL, 2);
        drawText(30, 148, "WAITING FOR BACKEND", COLOR_TEXT, COLOR_PANEL, 2);
        drawText(30, 180, "OR DEVICE SYNC", COLOR_TEXT, COLOR_PANEL, 2);
        drawText(12, 442, "A HOME  |  C ALERTS  |  D SYSTEM", COLOR_MUTED, COLOR_BG, 1);
        return;
    }

    uint16_t statusColor = carePlanStatusColor(carePlan.status);
    rawFillRect(12, 72, 296, 42, COLOR_PANEL);
    drawText(22, 86, carePlanStatusText(carePlan.status), statusColor, COLOR_PANEL, 2);
    char countText[16];
    snprintf(countText, sizeof(countText), "%u/%u",
             carePlan.selectedScheduleIndex + 1,
             carePlan.scheduleCount);
    drawText(258, 86, countText, COLOR_MUTED, COLOR_PANEL, 2);

    rawFillRect(12, 126, 296, 82, COLOR_PANEL_ALT);
    drawText(22, 138, carePlan.period, COLOR_MUTED, COLOR_PANEL_ALT, 2);
    char insulinText[24];
    snprintf(insulinText, sizeof(insulinText), "%.22s", carePlan.insulinType);
    drawText(22, 172, insulinText, COLOR_TEXT, COLOR_PANEL_ALT, 2);

    char doseText[20];
    snprintf(doseText, sizeof(doseText), "%.1fU", carePlan.doseUnits);
    drawCard(12, 220, 142, 76, "DOSE", doseText, statusColor);
    drawCard(166, 220, 142, 76, "TARGET", carePlan.targetTime, COLOR_TEXT, true);

    rawFillRect(12, 308, 296, 50, COLOR_PANEL);
    char windowText[32];
    snprintf(windowText, sizeof(windowText), "%s - %s",
             carePlan.windowStart,
             carePlan.windowEnd);
    drawText(22, 318, "WINDOW", COLOR_MUTED, COLOR_PANEL, 2);
    drawText(142, 318, windowText, COLOR_TEXT, COLOR_PANEL, 2);

    rawFillRect(12, 370, 296, 54, COLOR_PANEL_ALT);
    char planText[48];
    snprintf(planText, sizeof(planText), "V%lu FROM %s",
             (unsigned long)carePlan.version,
             carePlan.effectiveFrom);
    drawText(22, 380, planText, COLOR_MUTED, COLOR_PANEL_ALT, 1);
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
        snprintf(stateText, sizeof(stateText), "REMINDER SILENCED");
    } else if (carePlan.status == CARE_PLAN_STATUS_DUE) {
        snprintf(stateText, sizeof(stateText), "REMINDER ACTIVE");
    } else if (carePlan.status == CARE_PLAN_STATUS_TAKEN) {
        snprintf(stateText, sizeof(stateText), "DOSE RECORDED TODAY");
    } else if (carePlan.status == CARE_PLAN_STATUS_MISSED) {
        snprintf(stateText, sizeof(stateText), "MISSED - CHECK CARE PLAN");
    } else {
        snprintf(stateText, sizeof(stateText), "WAITING FOR DEVICE TIME");
    }
    drawText(22, 402, stateText, statusColor, COLOR_PANEL_ALT, 1);

    rawFillRect(0, 432, DISPLAY_WIDTH, 48, COLOR_BG);
    if (carePlan.scheduleCount > 1) {
        drawText(12, 438, "* PREV  # NEXT", COLOR_MUTED, COLOR_BG, 1);
    } else {
        drawText(12, 438, "PRESCRIPTION", COLOR_MUTED, COLOR_BG, 1);
    }
    if (carePlan.status == CARE_PLAN_STATUS_DUE &&
        carePlan.manualStopAllowed &&
        !carePlan.reminderSilenced) {
        drawText(12, 458, "C SILENCE  |  A HOME  |  D SYSTEM", COLOR_WARN, COLOR_BG, 1);
    } else {
        drawText(12, 458, "A HOME  |  C ALERTS  |  D SYSTEM", COLOR_MUTED, COLOR_BG, 1);
    }
}

void drawDosePrompt(const DisplayState& state, const CarePlanView& carePlan) {
    if (forceFullRedraw) rawFillScreen(COLOR_BG);
    rawFillRect(0, 0, DISPLAY_WIDTH, 54, COLOR_WARN);
    drawText(12, 12, "CONFIRM DOSE", COLOR_BG, COLOR_WARN, 3);

    rawFillRect(12, 72, 296, 126, COLOR_PANEL);
    drawText(28, 92, "PEN", COLOR_MUTED, COLOR_PANEL, 2);

    char penBuf[24];
    snprintf(penBuf, sizeof(penBuf), "%.1fU", state.promptPenDoseUnits);
    drawText(88, 86, penBuf, COLOR_TEXT, COLOR_PANEL, 4);

    char sendBuf[24];
    snprintf(sendBuf, sizeof(sendBuf), "SEND %dU", state.pendingDoseUnits);
    drawText(28, 144, sendBuf, COLOR_OK, COLOR_PANEL, 4);

    rawFillRect(12, 214, 296, 92, state.dosePromptEditing ? COLOR_PANEL_ALT : COLOR_PANEL);
    if (state.dosePromptEditing) {
        drawText(28, 230, "EDIT INTEGER UNITS", COLOR_MUTED, COLOR_PANEL_ALT, 2);
        char editBuf[24];
        if (state.doseEditBuffer[0] != '\0') {
            snprintf(editBuf, sizeof(editBuf), "%sU", state.doseEditBuffer);
        } else {
            snprintf(editBuf, sizeof(editBuf), "_");
        }
        drawText(28, 260, editBuf, COLOR_TEXT, COLOR_PANEL_ALT, 4);
    } else {
        if (carePlan.available && carePlan.scheduleCount > 0) {
            char prescribedText[32];
            snprintf(prescribedText, sizeof(prescribedText), "PRESCRIBED %.1fU",
                     carePlan.doseUnits);
            drawText(28, 226, prescribedText, COLOR_ACCENT, COLOR_PANEL, 2);

            char scheduleText[40];
            snprintf(scheduleText, sizeof(scheduleText), "%.16s AT %s",
                     carePlan.insulinType,
                     carePlan.targetTime);
            drawText(28, 254, scheduleText, COLOR_TEXT, COLOR_PANEL, 1);

            float difference = fabsf(state.pendingDoseUnits - carePlan.doseUnits);
            drawText(28, 278,
                     difference <= 0.5f ? "MATCHES CARE PLAN" : "CHECK PRESCRIBED DOSE",
                     difference <= 0.5f ? COLOR_OK : COLOR_WARN,
                     COLOR_PANEL,
                     1);
        } else {
            drawText(28, 232, "NO PRESCRIPTION LOADED", COLOR_MUTED, COLOR_PANEL, 2);
            drawText(28, 270, "VERIFY PEN READING", COLOR_TEXT, COLOR_PANEL, 2);
        }
    }

    rawFillRect(12, 322, 296, 74, COLOR_PANEL);
    if (state.dosePromptEditing) {
        if (state.doseEditBuffer[0] != '\0') {
            drawText(22, 336, "D SUBMIT  * DELETE", COLOR_TEXT, COLOR_PANEL, 2);
            drawText(22, 370, "# CLEAR  C BACK", COLOR_MUTED, COLOR_PANEL, 2);
        } else {
            drawText(22, 336, "ENTER DOSE WITH 0-9", COLOR_TEXT, COLOR_PANEL, 2);
            drawText(22, 370, "C BACK", COLOR_MUTED, COLOR_PANEL, 2);
        }
    } else {
        drawText(28, 332, "A CONFIRM  B EDIT", COLOR_TEXT, COLOR_PANEL, 2);
        char autoBuf[24];
        snprintf(autoBuf, sizeof(autoBuf), "AUTO CONFIRM %uS", state.dosePromptRemainingSec);
        drawText(28, 362, "C CANCEL", COLOR_BAD, COLOR_PANEL, 2);
        drawText(166, 370, autoBuf, COLOR_WARN, COLOR_PANEL, 1);
    }

    rawFillRect(0, 438, DISPLAY_WIDTH, 42, COLOR_BG);
    drawText(10, 450, "CONFIRMED SEND VALUE GOES TO BACKEND", COLOR_MUTED, COLOR_BG, 1);
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

    for (;;) {
        DisplayState state = getDisplayStateSnapshot();
        CarePlanView carePlan = getCarePlanViewSnapshot();
        if (displayNeedsRedraw(state,
                               previousState,
                               carePlan,
                               previousCarePlan,
                               lastDrawMs,
                               hasPreviousState)) {
            forceFullRedraw = !hasPreviousState || visibleMode(state) != visibleMode(previousState);
            if (state.dosePromptActive) {
                drawDosePrompt(state, carePlan);
            } else {
                switch (state.activePage) {
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
