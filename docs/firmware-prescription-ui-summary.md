# Outer Unit Prescription UI and Dose Confirmation Summary

Date: 2026-07-24

## Scope

This change adds the first Outer Unit Care Plan and prescription experience while
preserving the existing telemetry, BLE, ESP-NOW, offline queue, sensor, and dose
publishing paths.

The work covers:

- Receiving the backend Care Plan over MQTT.
- Validating and storing the current Care Plan in ESP32 NVS.
- A dedicated prescription display page.
- Visual dose-due notifications.
- Care Plan acknowledgement to the backend.
- A device sync request after MQTT connection.
- Correct and context-sensitive dose confirmation keypad controls.
- Independent completion state for every daily schedule.
- Reminder lifecycle and missed-dose telemetry for the backend.
- Patient-facing upcoming, due, silenced, taken, and missed states.
- Accidental pen-reading cancellation before any backend dose publish.
- Patient-focused Home, Prescription, Alerts, and System navigation.

No backend source files were changed.

## Backend Flow

1. A doctor creates or changes a prescription or dose schedule.
2. The backend generates a versioned Care Plan.
3. The backend publishes the plan to:

   `diasmart/devices/{outerDeviceUid}/care-plan`

4. The Outer Unit validates the device UID, version, schedule count, required
   fields, dose, and time windows.
5. A valid plan is stored in NVS and displayed.
6. The Outer Unit publishes `APPLIED` or `REJECTED` to:

   `diasmart/devices/{outerDeviceUid}/command-ack`

7. On each MQTT connection, the Outer Unit publishes `DEVICE_SYNC_REQUEST` to:

   `diasmart/devices/{outerDeviceUid}/telemetry`

This sync request lets the backend resend the current Care Plan after the device
was offline. The current backend publisher sends Care Plans as non-retained
messages, so reconnect sync remains required.

## Prescription Page

Press `1` from any normal display page to open `PRESCRIPTION`.

The page shows:

- Care Plan status: `UPCOMING`, `DOSE DUE`, `TAKEN`, or `MISSED`.
- Selected schedule number and total schedule count.
- Period and insulin type.
- Prescribed dose.
- Target time.
- Allowed dose window.
- Care Plan version and effective date.
- Time remaining until an upcoming target.
- Active or silenced reminder state.
- Recorded-dose or missed-dose guidance.

When more than one schedule exists:

- `*`: previous schedule.
- `#`: next schedule.

When a dose is due and the backend allows manual reminder stop:

- `C`: silence reminder reopening for the current schedule window.

The screen only shows `C SILENCE` while that action is available. After the
reminder is silenced, `C` returns to the alerts-page action. Silencing a reminder
does not confirm a dose.

Outside an active stoppable reminder, patient navigation is:

- `A`: home.
- `B`: prescription.
- `C`: alerts.
- `D`: system status.

The technical offline-queue page remains available with `0` for service
diagnostics, but it is no longer shown as a normal patient destination.

## Dose Confirmation Controls

The old screen displayed edit-only buttons when edit mode was inactive. Those
buttons had no active handler in that state. The UI now displays only controls
that can be used in the current mode.

### Confirmation Mode

- `A`: confirm and send the rounded pen dose.
- `B`: enter dose editing mode.
- `C`: cancel an accidental pen activation.

The screen also shows the selected prescribed dose, insulin type, and target
time. It displays either `MATCHES CARE PLAN` or `CHECK PRESCRIBED DOSE`.

Cancellation clears the prompt, publishes no dose, does not mark a prescription
as taken, and suppresses the same buffered pen record from reopening the prompt.

### Editing Mode

- `0-9`: enter integer dose units.
- `*`: delete the last digit.
- `#`: clear the full entry.
- `C`: cancel editing and return to confirmation mode.
- `D`: submit the edited dose.

`D`, `*`, and `#` are shown only after at least one digit is entered. This
prevents the display from advertising actions that cannot do anything.

The existing 40-second automatic confirmation remains unchanged.

## Notification Behavior

The firmware evaluates the stored schedule using NTP time and the Care Plan
timezone. `Asia/Colombo` is mapped to UTC+05:30.

At the target time and until the window end:

- The schedule status changes to `DOSE DUE`.
- The prescription page opens automatically.
- The visual reminder reopens at `repeatIntervalMinutes` until the dose is
  confirmed, manually silenced, or the schedule window closes.

When the dose is confirmed:

- The schedule status changes to `TAKEN`.
- The taken state is persisted independently for that schedule and local date.
- Receiving the same Care Plan version again does not clear the taken state.
- A later schedule can be confirmed without erasing an earlier schedule's state.

When an unconfirmed window closes, the schedule changes to `MISSED` and the
result is persisted. Windows that cross midnight use the date on which the
schedule started, preventing a second completion or reminder after midnight.

There is no configured Outer Unit buzzer GPIO in the current hardware
configuration. This change implements visual notification and manual silence,
but it does not drive a physical buzzer.

## Backend Reminder Events

The Outer Unit publishes these events to:

`diasmart/devices/{outerDeviceUid}/telemetry`

- `REMINDER_STARTED`: first visual reminder for the schedule occurrence.
- `REMINDER_REPEATED`: configured reminder repeats, with `repeatNumber`.
- `REMINDER_MANUALLY_STOPPED`: patient pressed `C` while silence was available.
- `DOSE_MISSED`: the allowed window closed without a confirmed dose.
- `POSSIBLE_DOUBLE_DOSE`: another distinct pen dose was confirmed for a schedule
  already marked taken in the same occurrence.

Each event includes a unique event ID, Outer Unit ID, schedule ID, Care Plan
version, schedule window, and UTC timestamp.

Reminder events use the existing LittleFS offline queue when MQTT is unavailable.
Queued records retain their MQTT destination, while older combined telemetry
records remain compatible with the legacy topic.

## Validation and Storage

The firmware accepts at most eight schedules per Care Plan. A larger plan is
rejected instead of being silently truncated.

Required schedule fields:

- `scheduleId`
- `insulinType`
- Positive `doseUnits`
- Valid `windowStart`
- Valid `targetTime`
- Valid `windowEnd`

The plan must match `DEVICE_UID_OUTER`. Older Care Plan versions are rejected.
The stored plan is restored before the display task starts. A future
`effectiveFrom` date cannot trigger an early reminder. A confirmed dose marks a
schedule as taken only when the confirmation occurs inside that schedule's
allowed window.

Care Plan NVS format version 3 stores taken, silenced, and missed dates for every
schedule. Stored version-1 and version-2 plans are migrated when loaded.

The MQTT buffer was increased from 2048 to 8192 bytes to match the backend
subscriber payload limit.

## Existing Functionality Preserved

The following behavior was not changed:

- Combined telemetry JSON format and legacy telemetry topic.
- Sensor event generation.
- BLE pen and glucometer processing.
- ESP-NOW Inner Unit telemetry.
- Offline JSON queue behavior.
- Queue diagnostics and device-health monitoring.
- Dose editing limits and automatic dose confirmation.

## Bluetooth Connection Behavior

The ESP32-S3 hardware can maintain multiple BLE client connections, but the
current firmware deliberately uses scheduled single-device sessions:

1. Keep the pen connected for dose notifications.
2. Every 30 seconds, disconnect the pen.
3. Connect to the glucometer and request stored measurements using RACP.
4. Disconnect the glucometer and reconnect the pen.

The pen buffers dose records and the Outer Unit acknowledges record slots, so a
dose occurring during the short glucometer session can be recovered after pen
reconnection. True simultaneous continuous pen and glucometer connections would
require a multi-client BLE manager redesign and hardware testing. The
glucometer itself is currently used as a periodic stored-record sync device, not
as a continuously connected notification source.

## Backend Data Gaps

The current backend stores `daysOfWeek` on dose schedules but does not include
it in the Care Plan MQTT payload. Prescription start and end dates are also not
included per schedule.

Until the backend adds those fields, the firmware treats published schedules as
daily schedules beginning from the Care Plan effective date.

## Verification

PlatformIO Outer Unit build:

```text
Environment: esp32-s3-devkitc-1
Result: SUCCESS
RAM: 68,468 / 327,680 bytes (20.9%)
Flash: 1,544,913 / 3,342,336 bytes (46.2%)
```

Static checks confirmed that each displayed keypad action has a matching code
handler.

Latest build after cancellation and patient UI improvements:

```text
Environment: esp32-s3-devkitc-1
Result: SUCCESS
RAM: 69,852 / 327,680 bytes (21.3%)
Flash: 1,556,781 / 3,342,336 bytes (46.6%)
```

## Implementation Commits

- `811a68f` - `Fix per-schedule prescription completion state`
- `20251ca` - `Add prescription reminder lifecycle telemetry`
- `932d439` - `Refine patient prescription display states`
- `163d173` - `Add accidental pen dose cancellation`
- `6cb90c6` - `Simplify outer unit patient navigation`

The documentation update is committed separately after these implementation
steps.

## Hardware Test Checklist

1. Boot with no stored Care Plan and press `1`.
2. Confirm the page shows `NO ACTIVE CARE PLAN`.
3. Publish a backend Care Plan for the configured Outer Unit UID.
4. Confirm `APPLIED` appears in backend Care Plan delivery status.
5. Restart the Outer Unit and confirm the prescription is restored.
6. Use `*` and `#` to browse multiple schedules.
7. Set a target time a few minutes ahead and verify the countdown and automatic
   visual reminder.
8. Verify `C SILENCE` appears only while a stoppable reminder is due.
9. Press `C`, confirm `REMINDER SILENCED`, and verify the backend stores
   `REMINDER_MANUALLY_STOPPED`.
10. Allow a test window to close and verify `MISSED` and `DOSE_MISSED`.
11. Trigger a pen dose, press `C CANCEL`, and verify no dose reaches the backend.
12. Confirm the same buffered pen record does not reopen the cancelled prompt.
13. Trigger a pen dose and verify `A` confirmation.
14. Trigger another dose and verify `B`, digits, `*`, `#`, `C`, and `D`.
15. Verify `A HOME`, `B RX`, `C ALERTS`, and `D SYSTEM` from each normal page.
16. Confirm the technical queue page remains available with `0`.
17. Confirm two different schedules can both remain `TAKEN` on the same day.
18. Confirm Home, Prescription, Alerts, System, BLE, ESP-NOW, and telemetry still
    operate normally.

## Remaining Gaps

- No physical buzzer GPIO is configured on the Outer Unit.
- Device telemetry is removed from the current offline queue after MQTT broker
  publish succeeds. Full deletion after application-level `telemetry-ack` is
  still future work.
- The backend Care Plan payload does not include per-schedule `daysOfWeek` or
  prescription end dates, so firmware evaluates published schedules daily.
