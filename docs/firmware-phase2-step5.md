# Firmware Phase 2 Step 5 - Send Saved Pen Doses Over BLE

## Scope

- Units: pen unit and outer BLE parser only.
- Send from saved NVS records instead of directly sending live queue events.
- Outer writes current epoch time to the pen after BLE connection.
- Pen uses that time sync to convert saved dose `millis()` into real dose epoch time.
- Extend pen-to-outer BLE payload with dose epoch while keeping backend JSON unchanged.
- Keep backward compatibility with legacy BLE payload `dose,<units>`.

## Behavior

- Dose detection still saves a confirmed dose before queueing.
- The queue now acts as a wake/signal path for BLE transfer.
- When an outer client is connected, BLE scans saved records with `DOSE_RECORD_PENDING`.
- Outer writes compact time-sync payload `t,<epochSec>` to the pen characteristic.
- Pen waits for time sync before notifying stored dose records.
- Each pending record is notified once using compact BLE payload `d,<slot>,<doseTenths>,<takenEpochSec>`.
- `takenEpochSec` is calculated from the saved pen dose timestamp and the outer time sync.
- Outer converts `takenEpochSec` into backend `dose.injectedAt`.
- If the pen reboots after saving a dose but before connecting to outer, exact taken time is not recoverable without RTC hardware.
- After notifying, the record is marked `DOSE_RECORD_SENT`.
- ACK handling is not added yet; that is the next step.

## Validation

Use the pen unit on COM10 and outer unit on COM9:

```powershell
& "C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe" run -d firmware\pen-unit -e esp32-c3-supermini -t upload --upload-port COM10
& "C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe" run -d firmware\outer-unit -e esp32-s3-devkitc-1 -t upload --upload-port COM9
& "C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe" device monitor -p COM10 -b 115200
```

Expected logs:

- Live dial continues while rotating.
- Button click prints `Dose saved and queued`.
- Outer BLE prints `Time sync sent to pen: t,...`.
- Pen BLE prints `Time sync received: epoch=...`.
- Pen BLE prints `Notified stored dose slot ...`.
- Outer BLE prints `Pen dose received: ... injectedAt=...`.

## Next Step

Add an ACK write path from the outer unit so `DOSE_RECORD_SENT` can become `DOSE_RECORD_ACKED`; resend policy should retry non-ACKed records without duplicating backend uploads.
