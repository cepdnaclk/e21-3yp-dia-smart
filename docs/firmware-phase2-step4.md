# Firmware Phase 2 Step 4 - Pen Save Before Queue

## Scope

- Unit: pen unit only.
- Keep legacy live dial, raw angle, button debounce, and BLE payload behavior.
- Add local persistence only after a dose is already confirmed.

## Behavior

- Confirmed dose is converted to a fixed-size `PersistentDoseRecord`.
- Pen stores up to 8 pending dose records in NVS.
- Dose is queued to the existing BLE task only after storage succeeds.
- If BLE queue is full, the stored record remains pending for future resend work.
- If storage is unavailable or full, the dose is not queued.

## Hardware Test

Use the pen unit on COM10:

```powershell
& "C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe" run -d firmware\pen-unit -e esp32-c3-supermini -t upload --upload-port COM10
& "C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe" device monitor -p COM10 -b 115200
```

Expected logs:

- `[Main] Dose storage ready: ... pending / 8 capacity`
- Live dial continues every 200 ms.
- Valid button release prints `Dose saved and queued`.

## Next Step

Wire BLE transfer to drain saved pending records and wait for an outer-unit ACK before marking them delivered.
