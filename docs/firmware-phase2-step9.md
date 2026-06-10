# Firmware Phase 2 Step 9 - Pen Pending-Aware Advertising

## Goal
Reduce unnecessary BLE activity from the pen while keeping dose delivery reliable.

## Change
- Pen storage now exposes `pendingDoseCount()`.
- The pen treats both `PENDING` and `SENT` dose slots as active until ACK clears them.
- Pen BLE advertising switches by stored dose state:
  - `FAST` advertising when at least one dose is waiting for delivery or ACK.
  - `SLOW` advertising when no dose records are active.
- Dose payload remains unchanged: `d,<slot>,<doseTenths>,<takenEpochSec>`.
- Backend JSON remains unchanged because the outer unit behavior and serializer are untouched.

## Reliability Note
Records marked `SENT` are retried on later connections until the outer sends `a,<slot>`.
This avoids a stuck slot if BLE notification succeeds but the ACK is lost.

## Expected Logs
```text
[BLE] Ready as "Dose_ESP32_C3"
[BLE] Advertising SLOW (pending=0)
[BLE] Advertising FAST (pending=1)
[BLE] ACK received; cleared stored dose slot 0
[BLE] Advertising SLOW (pending=0)
```

## Verification
- Built pen firmware for `esp32-c3-supermini`.
- Uploaded pen firmware to `COM10`.
- Build memory remained acceptable for ESP32-C3:
  - RAM: about 11.9%
  - Flash: about 74.8%

