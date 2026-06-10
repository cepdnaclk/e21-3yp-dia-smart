# Firmware Phase 2 Step 16 - Dose Confirmation With Keypad

## Scope

Unit changed: outer unit only.

No inner-unit code, glucometer BLE parsing, or pen firmware was changed in this step.

## Behavior

- Pen dose received by the outer unit is not immediately sent as a dose telemetry event.
- The outer display shows a confirmation screen:
  - `A` confirms the rounded dose.
  - `B` starts integer edit mode.
  - `D` submits the edited integer dose.
  - `C` exits edit mode without cancelling the dose.
  - `#` clears the edit buffer.
- Decimal pen readings are rounded to an integer before display and JSON sending.
  - Example: `7.6` units is shown and sent as `8` units.
- If the patient does not respond within `40s`, the rounded dose is auto-confirmed and sent.
- Manual edits accept integers only.
- There is no main-screen cancel path; the dose is sent by confirm, edit-submit, or timeout.
- Display orientation is rotated 180 degrees for the current enclosure side.

## Keypad Pins

| Keypad Signal | ESP32-S3 GPIO |
| --- | --- |
| R1 | GPIO1 |
| R2 | GPIO2 |
| R3 | GPIO3 |
| R4 | GPIO4 |
| C1 | GPIO35 |
| C2 | GPIO36 |
| C3 | GPIO37 |
| C4 | GPIO38 |

## Build And Upload

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\outer-unit -e esp32-s3-devkitc-1
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\outer-unit -e esp32-s3-devkitc-1 -t upload --upload-port COM9
```

## Verification Logs

Expected serial lines:

```text
[Keypad] Task started
[Keypad] Key pressed: A
[EventAgg] Dose pending confirmation: raw=7.6 rounded=8 timeout=40s
[EventAgg] Dose confirmed by patient: 8 units
```

Timeout path:

```text
[EventAgg] Dose auto-confirmed after timeout: 8 units
```
