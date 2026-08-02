#pragma once

#include <stddef.h>
#include <stdint.h>
#include <stdio.h>

namespace glucose_time {

inline bool isLeapYear(int year) {
    return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0;
}

inline int daysInMonth(int year, int month) {
    static const uint8_t days[] = {
        31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31
    };
    if (month < 1 || month > 12) {
        return 0;
    }
    return month == 2 && isLeapYear(year) ? 29 : days[month - 1];
}

inline bool isValidDateTime(int year,
                            int month,
                            int day,
                            int hour,
                            int minute,
                            int second) {
    return year >= 1582 &&
           year <= 9999 &&
           day >= 1 &&
           day <= daysInMonth(year, month) &&
           hour >= 0 &&
           hour <= 23 &&
           minute >= 0 &&
           minute <= 59 &&
           second >= 0 &&
           second <= 59;
}

inline int64_t daysFromCivil(int year, unsigned month, unsigned day) {
    year -= month <= 2;
    const int era = (year >= 0 ? year : year - 399) / 400;
    const unsigned yearOfEra = (unsigned)(year - era * 400);
    const unsigned adjustedMonth =
        month > 2 ? month - 3 : month + 9;
    const unsigned dayOfYear =
        (153 * adjustedMonth + 2) / 5 + day - 1;
    const unsigned dayOfEra =
        yearOfEra * 365 + yearOfEra / 4 - yearOfEra / 100 + dayOfYear;
    return (int64_t)era * 146097 + dayOfEra - 719468;
}

inline void civilFromDays(int64_t dayCount,
                          int* year,
                          unsigned* month,
                          unsigned* day) {
    dayCount += 719468;
    const int64_t era =
        (dayCount >= 0 ? dayCount : dayCount - 146096) / 146097;
    const unsigned dayOfEra =
        (unsigned)(dayCount - era * 146097);
    const unsigned yearOfEra =
        (dayOfEra - dayOfEra / 1460 + dayOfEra / 36524 -
         dayOfEra / 146096) /
        365;
    int resolvedYear = (int)yearOfEra + (int)era * 400;
    const unsigned dayOfYear =
        dayOfEra -
        (365 * yearOfEra + yearOfEra / 4 - yearOfEra / 100);
    const unsigned monthPrime = (5 * dayOfYear + 2) / 153;
    const unsigned resolvedDay =
        dayOfYear - (153 * monthPrime + 2) / 5 + 1;
    const unsigned resolvedMonth =
        monthPrime < 10 ? monthPrime + 3 : monthPrime - 9;
    resolvedYear += resolvedMonth <= 2;

    *year = resolvedYear;
    *month = resolvedMonth;
    *day = resolvedDay;
}

inline size_t concentrationOffset(uint8_t flags) {
    return 10U + ((flags & 0x01U) != 0 ? 2U : 0U);
}

// Bluetooth Time Offset adjusts Base Time to the meter's user-facing time.
// utcOffsetMinutes is the separate timezone attached to the resulting ISO time.
inline bool formatMeasuredAt(const uint8_t* packet,
                             size_t length,
                             int utcOffsetMinutes,
                             char* output,
                             size_t outputLength) {
    if (packet == nullptr ||
        output == nullptr ||
        outputLength == 0 ||
        length < 12 ||
        (packet[0] & 0x01U) == 0 ||
        utcOffsetMinutes < -1440 ||
        utcOffsetMinutes > 1440) {
        return false;
    }

    const int year = packet[3] | ((int)packet[4] << 8);
    const int month = packet[5];
    const int day = packet[6];
    const int hour = packet[7];
    const int minute = packet[8];
    const int second = packet[9];
    if (!isValidDateTime(year, month, day, hour, minute, second)) {
        return false;
    }

    const int16_t meterTimeOffsetMinutes =
        (int16_t)(packet[10] | ((uint16_t)packet[11] << 8));
    if (meterTimeOffsetMinutes < -1440 ||
        meterTimeOffsetMinutes > 1440) {
        return false;
    }

    int64_t adjustedSeconds =
        daysFromCivil(year, (unsigned)month, (unsigned)day) * 86400 +
        hour * 3600 +
        minute * 60 +
        second +
        (int64_t)meterTimeOffsetMinutes * 60;

    int64_t adjustedDay = adjustedSeconds / 86400;
    int secondsOfDay = (int)(adjustedSeconds % 86400);
    if (secondsOfDay < 0) {
        secondsOfDay += 86400;
        adjustedDay--;
    }

    int resolvedYear = 0;
    unsigned resolvedMonth = 0;
    unsigned resolvedDay = 0;
    civilFromDays(
        adjustedDay, &resolvedYear, &resolvedMonth, &resolvedDay);
    if (resolvedYear < 1582 || resolvedYear > 9999) {
        return false;
    }

    const int resolvedHour = secondsOfDay / 3600;
    const int resolvedMinute = (secondsOfDay % 3600) / 60;
    const int resolvedSecond = secondsOfDay % 60;
    const char offsetSign = utcOffsetMinutes < 0 ? '-' : '+';
    const int absoluteOffset =
        utcOffsetMinutes < 0 ? -utcOffsetMinutes : utcOffsetMinutes;

    int written = snprintf(
        output,
        outputLength,
        "%04d-%02u-%02uT%02d:%02d:%02d%c%02d:%02d",
        resolvedYear,
        resolvedMonth,
        resolvedDay,
        resolvedHour,
        resolvedMinute,
        resolvedSecond,
        offsetSign,
        absoluteOffset / 60,
        absoluteOffset % 60);
    return written > 0 && (size_t)written < outputLength;
}

}  // namespace glucose_time
