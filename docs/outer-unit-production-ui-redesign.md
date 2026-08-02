# Outer Unit Patient UI Redesign

Date: 2026-08-03

## Scope

This change redesigns the Outer Unit's 320 x 480 portrait TFT interface as a
patient-facing product UI. It does not change backend payloads, MQTT topics,
Care Plan storage, BLE behavior, ESP-NOW behavior, or offline-queue behavior.

The existing raw RGB565 renderer remains in use. No framebuffer, image asset,
dynamic UI framework, or additional library was introduced.

## Interaction model

Normal navigation uses four stable physical-key shortcuts:

- `A`: Home
- `B`: Medication
- `C`: Alerts
- `D`: Device

The service diagnostics page remains available through `0`, but it is not
advertised as a normal patient destination.

From the Device page:

- `#`: Device Setup details and kit IDs

On a Medication page with multiple schedules:

- `*`: previous schedule
- `#`: next schedule

When a stoppable medication reminder is due, `C` pauses that reminder. The
screen only advertises this contextual action while it is available.

## Patient screens

### Home

Home prioritizes the next medication and groups storage information under an
explicit `INSULIN STORAGE` heading. Temperature, door, and insulin availability
remain separate detailed cards:

- Temperature shows the current value and `SAFE`, `TOO COLD`, `TOO WARM`, or
  `WAITING`.
- Door shows `OPEN`, `CLOSED`, or `WAITING`, followed by an action/status.
- Insulin availability shows percentage, a progress bar, and `GOOD`, `LOW`, or
  `WAITING`.

Latest glucose and last recorded dose are secondary cards. A single banner
shows the most important current action instead of exposing MQTT, BLE, queue,
and heap implementation details.

### Medication

The Medication page shows:

- Upcoming, due, recorded, or missed status
- Dose and insulin type
- Target time and allowed window
- Time remaining until an upcoming dose
- Selected schedule number
- Previous/next controls only when there is more than one schedule

Care Plan version and backend-oriented data are removed from the patient page.
The no-plan state distinguishes an online device from one that is offline.

### Alerts

Alerts show only conditions that require attention. Each item states the
problem and a short recovery action. When there are no active alerts, the page
shows `EVERYTHING LOOKS GOOD`.

### Device

Patient-facing device status uses:

- Internet
- DiaSmart Cloud
- Smart Pen
- Storage Unit
- Storage battery
- Records to sync

MQTT and queue filesystem details remain on the service diagnostics page.

### Device Setup

The Device page offers `# SETUP` without changing the four primary navigation
keys. The current frontend registration wizard uses manual fields, so Device
Setup shows the exact text needed by that flow:

- Outer Unit ID
- Inner Unit ID
- Pen Unit ID
- Glucometer ID
- Outer Unit setup Wi-Fi network
- Case-sensitive setup Wi-Fi password

The setup network and password use a case-sensitive font so the TFT does not
accidentally convert credentials to uppercase. A QR can be added later together
with the matching frontend scanner/parser.

## Dose confirmation

The required 40-second automatic dose recording behavior is preserved.

The redesigned confirmation screen always shows:

- Dose detected by the pen
- Dose that will be recorded
- Prescribed dose and insulin type when a Care Plan is available
- A clear match or mismatch message
- Exact seconds until automatic recording
- A decreasing visual countdown bar
- Current physical-key actions

Normal confirmation controls remain:

- `A`: record now
- `B`: edit dose
- `C`: cancel and do not send

Edit controls remain:

- `0-9`: enter integer units
- `*`: delete
- `#`: clear
- `C`: return to the detected dose
- `D`: save the edited dose

The countdown remains visible in both confirmation and editing modes. After a
manual record, automatic record, or cancellation, a short outcome screen
confirms what happened and whether the record is ready to sync.

## Usability principles

- Hick's Law: four normal destinations and one dominant task per screen.
- Fitts's Law: large action regions and consistent mapping to physical keys.
- Miller's Law: information is grouped into a few meaningful sections without
  removing necessary storage details.
- Gestalt principles: proximity, common regions, alignment, similarity, and
  semantic color establish hierarchy.
- Jakob's Law: familiar patient language replaces technical implementation
  terms.
- Nielsen's heuristics: visible status, real-world language, user control,
  consistency, error prevention, visible actions, expert diagnostics,
  minimalist patient screens, recovery guidance, and contextual instructions.

Critical states use both wording and color. Green represents normal/confirmed
states, amber represents attention, and red is reserved for serious mismatch or
missed-dose states.

## Build verification

PlatformIO Outer Unit build with the Device Setup text page:

```text
Environment: esp32-s3-devkitc-1
Result: SUCCESS
RAM: 70,484 / 327,680 bytes (21.5%)
Flash: 1,622,981 / 3,342,336 bytes (48.6%)
```

## Hardware validation checklist

1. Boot with and without a stored Care Plan.
2. Verify Home with fresh Inner Unit telemetry and while telemetry is waiting.
3. Verify safe, high, and low temperature states.
4. Verify door-open and low-stock actions.
5. Verify online, reconnecting, and offline banners.
6. Verify Home, Medication, Alerts, and Device mappings from every normal page.
7. Verify `0` opens diagnostics and `A` returns Home.
8. Open Device, press `#`, and confirm all four IDs match the configured kit.
9. Confirm the setup network and password preserve uppercase and lowercase.
10. Use the displayed text in the frontend's manual registration flow.
11. Verify single- and multi-schedule Medication layouts.
12. Verify due, upcoming, recorded, missed, and paused reminder states.
13. Trigger matching and mismatching pen doses.
14. Verify the full 40-second countdown in confirmation and editing modes.
15. Verify manual record, edited record, automatic record, and cancellation
    outcome screens.
16. Confirm backend dose data and offline retry behavior are unchanged.
17. Inspect all text and colors on the physical TFT under expected lighting.

The firmware was compiled successfully but this redesign has not yet been
flashed to or visually validated on physical hardware.
