# Firmware Phase 2 Step 2

Date: 2026-06-05
Branch: `ananthu-dev`
Step: Add pen dose storage service skeleton

## Goal

Add a small pen-side storage service API around `PersistentDoseRecord` before
wiring dose detection into any storage behavior.

## What Changed

- Added `firmware/pen-unit/src/services/storage_service.h`
- Implemented `PenDoseStorageService` in `storage_service.cpp`
- Added fixed-capacity record operations:
  - `begin`
  - `appendPending`
  - `read`
  - `updateStatus`
  - `countByStatus`
  - `clearVolatileMirror`

## Current Boundary

This step uses an in-RAM volatile mirror only. It does not write to flash/NVS yet.

The service is not wired into:

- `dose_detection_task.cpp`
- `ble_transfer_task.cpp`
- `main.cpp`

## Why This Is Safe

The current pen behavior is unchanged:

- confirmed doses still go to the existing FreeRTOS queue
- BLE transfer still uses the existing notify payload
- no persistent flash writes occur
- no outer-unit behavior changes
- no backend payload changes

## Why This Matters

The next pen steps need one controlled service boundary for:

- save-before-send behavior
- pending record lookup
- sent/ACKed status transitions
- future NVS/flash persistence

Adding this boundary first keeps the later runtime change smaller.

## Hardware Checkpoint Recommendation

No hardware is required for this skeleton step.

The first pen hardware checkpoint should happen after the service is wired into
dose detection with save-before-queue behavior:

- connect `pen` only
- confirm dose detection still works
- confirm stored pending record count changes as expected

## Explicit Non-Goals

- No NVS/flash persistence yet
- No dose detection behavior change yet
- No BLE transfer behavior change yet
- No ACK handling yet
