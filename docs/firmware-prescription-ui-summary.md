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
was offline. The current backend publishes Care Plans as non-retained messages.

## Prescription Page

Press `1` from any normal display page to open `PRESCRIPTION`.

The page shows:

- Care Plan status: `UPCOMING`, `DOSE DUE`, or `TAKEN`.
- Selected schedule number and total schedule count.
- Period and insulin type.
- Prescribed dose.
- Target time.
- Allowed dose window.
- Care Plan version and effective date.
- Reminder repeat and buzzer-duration settings.

When more than one schedule exists:

- `*`: previous schedule.
- `#`: next schedule.

Normal navigation remains unchanged:

- `A`: dashboard.
- `B`: device status.
- `C`: alerts.
- `D`: offline queue.

## Dose Confirmation Controls

The old screen displayed edit-only buttons when edit mode was inactive. Those
buttons had no active handler in that state. The UI now displays only controls
that can be used in the current mode.

### Confirmation Mode

- `A`: confirm and send the rounded pen dose.
- `B`: enter dose editing mode.

The screen also shows the selected prescribed dose, insulin type, and target
time. It displays either `MATCHES CARE PLAN` or `CHECK PRESCRIBED DOSE`.

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
  confirmed or the schedule window closes.

When the dose is confirmed:

- The schedule status changes to `TAKEN`.
- The taken state is persisted for that schedule and local date.
- Receiving the same Care Plan version again does not clear the taken state.

There is no configured Outer Unit buzzer GPIO in the current hardware
configuration. This change implements the visual notification and displays the
backend buzzer settings, but it does not drive a physical buzzer.

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

The MQTT buffer was increased from 2048 to 8192 bytes to match the backend
subscriber payload limit.

## Existing Functionality Preserved

The following behavior was not changed:

- Combined telemetry JSON format and legacy telemetry topic.
- Sensor event generation.
- BLE pen and glucometer processing.
- ESP-NOW Inner Unit telemetry.
- Offline JSON queue behavior.
- Existing dashboard, device, alert, and queue pages.
- Dose editing limits and automatic dose confirmation.

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

## Hardware Test Checklist

1. Boot with no stored Care Plan and press `1`.
2. Confirm the page shows `NO ACTIVE CARE PLAN`.
3. Publish a backend Care Plan for the configured Outer Unit UID.
4. Confirm `APPLIED` appears in backend Care Plan delivery status.
5. Restart the Outer Unit and confirm the prescription is restored.
6. Use `*` and `#` to browse multiple schedules.
7. Set a target time a few minutes ahead and verify automatic visual reminder.
8. Trigger a pen dose and verify `A` confirmation.
9. Trigger another dose and verify `B`, digits, `*`, `#`, `C`, and `D`.
10. Confirm dashboard, device, alerts, queue, BLE, ESP-NOW, and telemetry still
    operate normally.
