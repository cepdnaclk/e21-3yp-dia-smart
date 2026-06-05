# Firmware Phase 2 Step 4 - Save Pen Dose Before Queueing

## Scope

- Unit: pen unit only.
- Goal: after a confirmed pen click, persist the dose before sending it to the existing BLE queue.
- Backend JSON and outer-unit behavior are unchanged in this step.

## Changes

- Added one global `PenDoseStorageService` instance in the pen runtime.
- Initialised NVS-backed dose storage during `setup()` before FreeRTOS tasks start.
- Halt startup if storage cannot initialise, because the pen must not send unsaved dose data.
- Converted each confirmed `DoseEvent` into a fixed-size `PersistentDoseRecord`.
- Appended the record as `DOSE_RECORD_PENDING` before calling `xQueueSend()`.
- If the BLE queue is full, the saved record remains pending for later resend work.
- If storage is full or unavailable, the dose is not queued.
- Corrected task core selection so ESP32-C3 pen builds use core 0 instead of invalid core 1 pinning.
- Added a boot nonce to pen record IDs to reduce duplicate IDs after reboot.

## Validation Point

Pen unit only:

```powershell
pio run -d firmware\pen-unit -e esp32-c3-supermini -t upload --upload-port COM10
pio device monitor -p COM10 -b 115200
```

Expected serial behavior:

- Startup prints dose storage ready with pending count and capacity.
- A valid click prints `Dose saved and queued`.
- If BLE queue is full, the dose is still saved and remains pending.
- If all 16 slots are already pending, storage rejects new dose queueing.

## Next Step

Add read/resend selection from pending pen records so BLE transfer can send saved records rather than only live queue events.
