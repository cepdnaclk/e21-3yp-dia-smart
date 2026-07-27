#include <assert.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>

#include "../../common/utils/glucose_time_utils.h"

namespace {

void setBaseTime(uint8_t* packet,
                 int year,
                 int month,
                 int day,
                 int hour,
                 int minute,
                 int second) {
    packet[3] = (uint8_t)(year & 0xFF);
    packet[4] = (uint8_t)((year >> 8) & 0xFF);
    packet[5] = (uint8_t)month;
    packet[6] = (uint8_t)day;
    packet[7] = (uint8_t)hour;
    packet[8] = (uint8_t)minute;
    packet[9] = (uint8_t)second;
}

void setMeterOffset(uint8_t* packet, int16_t minutes) {
    packet[10] = (uint8_t)(minutes & 0xFF);
    packet[11] = (uint8_t)(((uint16_t)minutes >> 8) & 0xFF);
}

void expectTimestamp(const uint8_t* packet,
                     size_t length,
                     int utcOffsetMinutes,
                     const char* expected) {
    char actual[32] = {};
    assert(glucose_time::formatMeasuredAt(
        packet, length, utcOffsetMinutes, actual, sizeof(actual)));
    assert(strcmp(actual, expected) == 0);
}

}  // namespace

int main() {
    uint8_t guidePacket[15] = {0x03};
    setBaseTime(guidePacket, 2026, 7, 27, 14, 35, 20);
    setMeterOffset(guidePacket, 0);
    guidePacket[12] = 126;
    expectTimestamp(
        guidePacket, sizeof(guidePacket), 330,
        "2026-07-27T14:35:20+05:30");
    assert(glucose_time::concentrationOffset(guidePacket[0]) == 12);

    uint8_t forwardRollover[15] = {0x03};
    setBaseTime(forwardRollover, 2026, 12, 31, 23, 30, 0);
    setMeterOffset(forwardRollover, 90);
    expectTimestamp(
        forwardRollover, sizeof(forwardRollover), 330,
        "2027-01-01T01:00:00+05:30");

    uint8_t backwardRollover[15] = {0x03};
    setBaseTime(backwardRollover, 2026, 1, 1, 0, 15, 0);
    setMeterOffset(backwardRollover, -30);
    expectTimestamp(
        backwardRollover, sizeof(backwardRollover), 330,
        "2025-12-31T23:45:00+05:30");

    uint8_t leapDay[15] = {0x03};
    setBaseTime(leapDay, 2024, 2, 29, 8, 0, 0);
    setMeterOffset(leapDay, 0);
    expectTimestamp(
        leapDay, sizeof(leapDay), -300,
        "2024-02-29T08:00:00-05:00");

    uint8_t invalidDate[15] = {0x03};
    setBaseTime(invalidDate, 2025, 2, 29, 8, 0, 0);
    setMeterOffset(invalidDate, 0);
    char output[32] = {};
    assert(!glucose_time::formatMeasuredAt(
        invalidDate, sizeof(invalidDate), 330, output, sizeof(output)));

    uint8_t missingMeterOffset[13] = {0x02};
    setBaseTime(missingMeterOffset, 2026, 7, 27, 14, 35, 20);
    assert(glucose_time::concentrationOffset(missingMeterOffset[0]) == 10);
    assert(!glucose_time::formatMeasuredAt(
        missingMeterOffset,
        sizeof(missingMeterOffset),
        330,
        output,
        sizeof(output)));

    uint8_t invalidMeterOffset[15] = {0x03};
    setBaseTime(invalidMeterOffset, 2026, 7, 27, 14, 35, 20);
    setMeterOffset(invalidMeterOffset, INT16_MIN);
    assert(!glucose_time::formatMeasuredAt(
        invalidMeterOffset,
        sizeof(invalidMeterOffset),
        330,
        output,
        sizeof(output)));

    puts("glucose time simulation passed");
    return 0;
}
