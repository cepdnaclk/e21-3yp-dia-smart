# Firmware Phase 2 Step 7 - Fair BLE Pen/Glucometer Scheduling

## Scope

- Unit: outer BLE manager only.
- Backend JSON shape remains unchanged.
- Pen ACK/time-sync behavior remains unchanged.

## Problem

Outer could get stuck in glucometer scan retry mode:

- Pen not found once.
- Outer switched to glucometer sync.
- If glucometer was off, outer kept scanning only for glucometer.
- Pen could no longer reconnect and send stored dose records.

## Change

- Glucometer scan is now a bounded attempt.
- If glucometer is not found, outer immediately returns to pen scanning.
- When glucometer is found, outer still syncs it and then returns to pen scanning.

## Expected Behavior

- Pen can reconnect and send saved doses.
- Glucometer can still be synced when it is on/advertising.
- BLE is cooperative, not truly parallel: outer alternates between pen and glucometer because the current firmware uses one BLE manager task and disconnects pen before glucometer sync.
