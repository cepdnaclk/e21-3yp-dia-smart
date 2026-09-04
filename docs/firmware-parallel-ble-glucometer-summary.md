# Parallel BLE and Live Glucometer Sync

## Scope

This firmware update gives the insulin pen and Accu-Chek Guide Me independent
BLE clients. The pen remains connected for notifications. The glucometer uses
a short, clean connection for each latest-record request so subsequent readings
can be received reliably.

The measurement-time follow-up adds one backward-compatible backend DTO field
and glucose persistence fallback. Database, IoT policy, web dashboard, and
mobile application code remain unchanged.

## Commits

- `860346b` - `Enable parallel BLE client connections`
- `0443bb3` - `Stream live glucometer readings over BLE`
- `f6f89d1` - `Preserve glucometer measurement timestamps`
- `60b3b87` - `Ingest glucometer source timestamps`
- Current fix - end each glucometer session after its RACP request completes or
  times out, then reconnect for the next request

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
to avoid overlapping BLE setup operations. The pen connection remains active,
while each glucometer connection performs one record request and then closes.
A glucometer failure or timeout resets only the glucometer client.

Existing pen dose parsing, buffered-dose acknowledgements, glucometer bonding,
and the configured glucometer PIN are preserved.

### Live glucose delivery

After connecting to the glucometer, the Outer Unit requests its latest stored
record once. When the RACP response arrives, or when the request times out after
twelve seconds, the Outer Unit disconnects only the glucometer. It scans and
opens a fresh session later. This avoids relying on repeated RACP requests in a
stale BLE session.

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
2. Confirm logs show `Pen connected in parallel`, `RACP latest-record request
   sent`, and `Glucometer session finished: RACP complete`.
3. Take a pen dose and confirm its notification is received while glucometer
   sessions open and close independently.
4. Take three glucose measurements, allowing a fresh glucometer connection for
   each one.
5. Confirm `Glucose queued immediately` appears once for each new sequence
   number.
6. Confirm each MQTT payload contains the new glucose value and sequence number.
7. Confirm exactly three database rows appear without rebooting either unit.
8. Confirm old sequence numbers do not create duplicate database rows.
9. Power-cycle only the pen and confirm it reconnects normally.
10. Power-cycle only the glucometer and confirm later sessions still work.
11. Disconnect Wi-Fi, take a glucose reading, restore Wi-Fi, and confirm the
    queued payload reaches the backend.

## Glucose Measurement Time

Commits `f6f89d1` and `60b3b87` implement the real meter measurement time
without replacing the existing event clock.

- Root `timestamp` remains the Outer Unit NTP event time.
- Storage, inventory, health, and other Inner Unit data still use root event
  time.
- `glucose.measuredAt` contains the validated Accu-Chek user-facing
  measurement time.
- Missing or malformed meter time falls back to root event time in the backend.
- The MQTT topic and version-one payload compatibility remain unchanged.
- No database migration was required.

Example glucose section:

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

The firmware parser follows Bluetooth Glucose Service Base Time plus Time
Offset behavior and attaches the configured `+05:30` deployment timezone.
Host simulation covers normal records, leap dates, date rollover, invalid
dates/offsets, missing offsets, and flags-dependent packet layout.

Verification:

- Host C++ simulation passed with warnings treated as errors.
- Outer Unit firmware build passed.
- Inner Unit firmware build passed.
- Focused backend tests passed, 9 of 9.
- Full backend suite passed, 116 of 116.

Physical Accu-Chek and pen testing is still required. Full implementation and
hardware validation details are in
`docs/glucometer-measurement-time-handoff.md`.

## Optional Reliability Follow-up

Firmware duplicate suppression currently remembers only the most recent
sequence number in RAM. Backend uniqueness prevents duplicate database rows,
but persisting the last accepted meter sequence in NVS would avoid redundant
MQTT traffic after an Outer Unit reboot.
