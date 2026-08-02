# Glucometer Measurement Time Implementation Record

## Repository State

- Repository: `C:\Personal\3yp\e21-3yp-dia-smart`
- Working branch: `ananthu-dev`
- Firmware commit: `f6f89d1` - `Preserve glucometer measurement timestamps`
- Backend commit: `60b3b87` - `Ingest glucometer source timestamps`
- The code has passed host simulation, firmware builds, and backend tests.
- The timestamp firmware has not yet been flashed or physically tested with
  the Accu-Chek Guide Me.

## Implemented Behavior

The system now keeps two different timestamps for their correct purposes:

1. Root `timestamp` remains the Outer Unit NTP event time.
2. `glucose.measuredAt` is the date and time stored in the Accu-Chek record.

The root timestamp is still used for raw events, Inner Unit storage,
inventory, health, battery, and other existing sensor processing. The glucose
source timestamp is never copied into those sections.

Example telemetry:

```json
{
  "timestamp": "2026-07-27T09:05:30Z",
  "glucose": {
    "deviceUid": "DS-GLU-0001",
    "valueMgDl": 126,
    "source": "BLE_GLUCOMETER",
    "sequenceNumber": 417,
    "measuredAt": "2026-07-27T14:35:20+05:30"
  }
}
```

If the meter time is missing or invalid, firmware omits
`glucose.measuredAt`. The backend then stores the root event time for
compatibility with old payloads.

## Bluetooth Time Interpretation

The implementation follows the official Bluetooth Glucose Service 1.0.1:

<https://www.bluetooth.com/wp-content/uploads/Files/Specification/HTML/GLS_v1.0.1/out/en/index-en.html>

- Bytes 3-9 contain Base Time.
- Flags bit 0 indicates a signed Time Offset in bytes 10-11.
- User-facing measurement time is `Base Time + Time Offset`.
- The first record in an RACP report operation must contain Time Offset.
- The meter Time Offset is not an ISO 8601 timezone.
- After applying the meter adjustment, firmware attaches the configured
  Sri Lanka timezone, `+05:30`.

The legacy sketch's unconditional UTC-to-Sri-Lanka addition was not reused.

## Firmware Changes

### Parser

`firmware/common/utils/glucose_time_utils.h`:

- Validates year, month, day, leap year, hour, minute, and second.
- Reads and validates the signed Bluetooth Time Offset.
- Handles forward and backward day/year rollover.
- Formats ISO 8601 with an explicit timezone.
- Calculates the glucose concentration position from the packet flags instead
  of assuming fixed bytes 12-13.

### Data propagation

- `GlucoseReading` carries `measuredAt` and `hasMeasuredAt`.
- `TelemetryEvent` carries `glucoseMeasuredAt` and
  `hasGlucoseMeasuredAt`.
- The BLE callback parses the source time before queuing the reading.
- The event aggregator copies it only when a new valid glucose reading exists.
- The JSON serializer emits optional `glucose.measuredAt`.
- Root `event.timestamp` generation is unchanged.
- `GLUCOMETER_UTC_OFFSET_MINUTES` is currently `330`.

## Backend Changes

`GlucoseDTO` now accepts optional `measuredAt`.

Only glucose persistence changed:

```java
glucose.setMeasuredAt(
        parseTimestamp(dto.getMeasuredAt(), eventTime)
);
```

This gives:

- Valid source timestamp: store the Accu-Chek measurement time.
- Missing source timestamp: store root event time.
- Malformed source timestamp: store root event time.

Storage and inventory still call `setMeasuredAt(eventTime)`. Raw events still
call `setEventTime(eventTime)`. No database migration was needed because
`glucose_readings.measured_at` and the API response field already existed.

The web dashboard already reads the backend `measuredAt` field, so no frontend
contract change was required.

## Automated Verification

### Host C++ simulation

Command:

```powershell
$env:PATH='C:\msys64\ucrt64\bin;' + $env:PATH
& 'C:\msys64\ucrt64\bin\g++.exe' `
  -std=c++17 -Wall -Wextra -Werror -pedantic `
  'C:/Personal/3yp/e21-3yp-dia-smart/firmware/outer-unit/test/glucose_time_simulation.cpp' `
  -o 'C:/Personal/3yp/e21-3yp-dia-smart/firmware/outer-unit/.pio/glucose_time_sim.exe'
& 'C:\Personal\3yp\e21-3yp-dia-smart\firmware\outer-unit\.pio\glucose_time_sim.exe'
```

Result:

```text
glucose time simulation passed
```

Covered normal Guide Me time, leap day, positive and negative rollover,
missing Time Offset, invalid date, invalid offset, timezone formatting, and
flags-dependent concentration location.

### Backend

- Focused `TelemetryProcessingServiceTest`: 9 passed.
- Complete Spring backend suite: 116 passed, 0 failures, 0 errors.

The added tests prove:

- Glucose uses valid meter `measuredAt`.
- Raw root event time remains unchanged.
- Old payloads fall back to root event time.
- Invalid meter timestamps fall back to root event time.

### Firmware builds

Outer Unit:

- Build result: success.
- RAM: 69,876 / 327,680 bytes, 21.3 percent.
- Flash: 1,558,729 / 3,342,336 bytes, 46.6 percent.

Inner Unit:

- Build result: success.
- RAM: 43,696 / 327,680 bytes, 13.3 percent.
- Flash: 734,841 / 1,310,720 bytes, 56.1 percent.

## Remaining Hardware Validation

Software verification is complete, but physical behavior still needs testing:

1. Set a known date/time on the Accu-Chek Guide Me.
2. Flash the tested Outer Unit firmware after explicit user approval.
3. Keep the insulin pen connected and take a glucose reading.
4. Confirm serial output prints the expected `measuredAt`.
5. Confirm MQTT contains root `timestamp` and glucose `measuredAt`.
6. Confirm the database glucose row uses the meter time.
7. Confirm storage and inventory rows continue using root event time.
8. Confirm the dashboard shows the glucose measurement time.
9. Repeat while Wi-Fi is offline and verify delayed MQTT delivery preserves
   the original meter measurement time.

## Constraints For Future Work

- Do not replace root `timestamp` with glucose time.
- Do not apply glucose time to Inner Unit sensor rows.
- Keep the backend fallback for old firmware.
- Do not claim physical verification until the Accu-Chek and pen are tested
  together.
- If the deployment timezone changes, update
  `GLUCOMETER_UTC_OFFSET_MINUTES` or replace it with device configuration.
