# Firmware Phase 2 Step 6 - ACK Saved Pen Doses

## Scope

- Units: pen unit and outer BLE manager.
- Backend JSON shape remains unchanged.
- ACK means the outer accepted the dose into its local `doseQueue`.

## Behavior

- Pen sends stored dose records as `d,<slot>,<doseTenths>,<takenEpochSec>`.
- Outer queues an ACK only after `xQueueSend(doseQueue, ...)` succeeds.
- Outer writes compact ACK payload `a,<slot>` to the pen characteristic from the BLE manager loop.
- Pen receives `a,<slot>` and clears that stored record by marking the slot `DOSE_RECORD_EMPTY`.
- If the outer dose queue is full, no ACK is sent and the pen keeps the stored record.

## Validation

Use the pen unit on COM10 and outer unit on COM9:

```powershell
& "C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe" run -d firmware\pen-unit -e esp32-c3-supermini -t upload --upload-port COM10
& "C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe" run -d firmware\outer-unit -e esp32-s3-devkitc-1 -t upload --upload-port COM9
```

Expected logs:

- Outer: `Pen dose received: ... injectedAt=...`
- Outer: `ACK sent to pen for slot ...`
- Pen: `ACK received; cleared stored dose slot ...`

## Next Step

Add resend policy for `DOSE_RECORD_SENT` records that were notified but not ACKed before disconnect.
