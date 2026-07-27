# Parallel BLE and Live Glucometer Sync

## Scope

This firmware update changes the Outer Unit BLE manager so the insulin pen and
Accu-Chek Guide Me can remain connected at the same time. It also polls the
connected glucometer for its latest record so a new reading enters the existing
backend telemetry path without requiring the meter to be switched off and on.

No backend, database, IoT policy, web dashboard, or mobile application code was
changed in this work.

## Commits

- `860346b` - `Enable parallel BLE client connections`
- `0443bb3` - `Stream live glucometer readings over BLE`

## Firmware Changes

### Independent BLE clients

The previous BLE state machine disconnected the pen before opening a
glucometer session, downloaded one record, disconnected the meter, and then
reconnected the pen.

The Outer Unit now maintains separate client state for:

- The insulin pen custom BLE service and dose notification characteristic.
- The Glucose Service measurement notification characteristic (`0x2A18`).
- The Glucose Service Record Access Control Point (`0x2A52`).

The manager scans only for a missing device. Connection attempts are serialized
to avoid overlapping BLE setup operations, but established pen and glucometer
links remain active concurrently. If one device disconnects, only that device
is scheduled for discovery and reconnection.

Existing pen dose parsing, buffered-dose acknowledgements, glucometer bonding,
and the configured glucometer PIN are preserved.

### Live glucose delivery

While the glucometer is connected, the Outer Unit requests its latest stored
record every five seconds. A request that does not complete within twelve
seconds is released so a later poll can retry.

The measurement callback:

1. Parses the glucometer sequence number and glucose value.
2. Rejects the last accepted sequence number as a duplicate.
3. Immediately writes a new reading to `glucoseQueue`.
4. Lets the event aggregator create glucose telemetry.
5. Publishes the JSON to `diasmart/device/telemetry`.
6. Uses the existing LittleFS offline queue if MQTT is unavailable.

The backend already rejects duplicate readings by device and glucometer
sequence number, which provides a second deduplication layer.

## Verification

Both firmware stages compiled successfully for the Outer Unit.

- RAM: 69,876 bytes of 327,680 bytes, 21.3 percent.
- Flash: 1,557,217 bytes of 3,342,336 bytes, 46.6 percent.

This confirms the code builds. A physical two-device test is still required to
confirm the Accu-Chek firmware's behavior while it remains powered on.

## Hardware Test Checklist

1. Start the Outer Unit with both paired devices available.
2. Confirm logs show `Pen connected in parallel` and
   `Glucometer connected in parallel`.
3. Take a pen dose and confirm its notification is received without dropping
   the glucometer connection.
4. Take a new glucose measurement while the meter stays on.
5. Within the next polling cycle, confirm `Glucose queued immediately` appears
   once for the new sequence number.
6. Confirm the MQTT payload contains the new glucose value and sequence number.
7. Confirm one database row appears without switching the meter off and on.
8. Leave the meter on through several polling cycles and confirm no duplicate
   database rows are created.
9. Power off only the pen, then power it on and confirm it reconnects while the
   glucometer stays connected.
10. Power off only the glucometer, then power it on and confirm it reconnects
    while the pen stays connected.
11. Disconnect Wi-Fi, take a glucose reading, restore Wi-Fi, and confirm the
    queued payload reaches the backend.

## Current Glucose Time Behavior

The Accu-Chek Glucose Measurement packet contains a Base Time:

- Year: bytes 3-4.
- Month, day, hour, minute, and second: bytes 5-9.
- Optional time offset: bytes 10-11 when indicated by the flags byte.

The current production parser only reads the sequence number and glucose value.
It records `millis()` as local reception uptime, but this value is not included
in MQTT telemetry.

The event aggregator assigns the Outer Unit NTP time to the root telemetry
`timestamp`. The backend parses that root timestamp and stores it as the
glucose row's `measured_at`. Therefore, the web dashboard currently displays
the time when the Outer Unit processed the record, not the time saved by the
glucometer.

The legacy glucometer sketch did parse bytes 3-9, but treated the meter Base
Time as UTC and always added 5 hours 30 minutes. That conversion should not be
copied without validating the meter's clock and optional offset behavior on the
physical device.

## Deferred Real Measurement Time Plan

These changes are required in a later, coordinated update.

### Firmware

1. Extend `GlucoseReading` and `TelemetryEvent` with an ISO 8601
   `measuredAt`, a validity flag, and optionally the meter offset in minutes.
2. Parse and validate the Base Time fields and optional time offset from every
   Glucose Measurement packet.
3. Convert the meter time using an explicit offset. If the meter does not send
   one, use a documented configured device/patient timezone rather than an
   implicit UTC conversion.
4. Serialize the source time as `glucose.measuredAt`, for example
   `2026-07-27T14:35:20+05:30`.
5. Keep the root `timestamp` as the Outer Unit event time. This preserves both
   source measurement time and gateway processing time.
6. On an invalid meter clock, omit `glucose.measuredAt` and let the backend
   apply its compatibility fallback. Log the reason for diagnostics.

### IoT payload

1. Add optional `glucose.measuredAt` to the telemetry contract.
2. Prefer a schema version increment for the new field and keep the backend
   compatible with old version-one payloads.
3. Keep the existing MQTT topic. AWS IoT routing or policy changes are not
   needed when only an allowed JSON field changes.
4. Preserve the complete payload in the existing offline queue so delayed
   delivery does not change the original measurement time.

Example future glucose section:

```json
{
  "glucose": {
    "deviceUid": "DS-GLU-0001",
    "valueMgDl": 126,
    "source": "BLE_GLUCOMETER",
    "sequenceNumber": 417,
    "measuredAt": "2026-07-27T14:35:20+05:30"
  }
}
```

### Backend

1. Add `measuredAt` to `GlucoseDTO`.
2. In `TelemetryProcessingService.saveGlucose`, parse
   `glucose.measuredAt` and use the root event time only as a fallback.
3. Reject or fall back from invalid dates and unreasonable future dates.
4. Keep `createdAt`/raw event `receivedAt` as server receipt time.
5. Add ingestion tests for a valid meter time, an old payload without meter
   time, malformed time, future time, and offline replay.

The `glucose_readings.measured_at` column, entity, response DTO, latest-reading
query, and database indexes already exist. No database migration is required
for the minimum change.

### Web dashboard

The web dashboard type and API response already use `measuredAt`. After backend
ingestion is corrected, the existing displays will receive the real meter time.
The follow-up should:

1. Request glucose history sorted by `measuredAt` descending explicitly.
2. Sort chart data chronologically instead of relying on response order.
3. Format timestamps with the intended patient timezone.
4. Change date-range filtering to timezone-aware date conversion instead of
   splitting the raw ISO string at `T`.
5. Add tests for offset timestamps and readings delivered later from the
   offline queue.

The mobile application currently uses mock glucose data and needs separate API
integration before this field affects its UI.

## Optional Reliability Follow-up

Firmware duplicate suppression currently remembers only the most recent
sequence number in RAM. Backend uniqueness prevents duplicate database rows,
but persisting the last accepted meter sequence in NVS would avoid redundant
MQTT traffic after an Outer Unit reboot.
