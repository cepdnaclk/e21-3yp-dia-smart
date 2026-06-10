# Firmware Phase 2 Step 3

Date: 2026-06-05
Branch: `ananthu-dev`
Step: Add NVS persistence inside pen dose storage service

## Goal

Make the pen dose storage service persist `PersistentDoseRecord` slots in ESP32
NVS while keeping the service isolated from current runtime tasks.

## What Changed

- Added Arduino ESP32 `Preferences` usage to `PenDoseStorageService`
- Opened a fixed NVS namespace: `dose_store`
- Added a format marker key: `fmt`
- Persisted each record under a fixed slot key:
  - `r00`
  - `r01`
  - ...
  - `r15`
- Loaded all stored records during `begin()`
- Persisted records during:
  - `appendPending`
  - `updateStatus`

## Why This Is Still Contained

This step does not wire the storage service into:

- `main.cpp`
- `dose_detection_task.cpp`
- `ble_transfer_task.cpp`

Current pen behavior is unchanged until the service is explicitly created and
called in a later step.

## Memory And Flash Policy

The record store remains fixed-size at 16 records. This avoids dynamic
allocation and keeps NVS writes bounded.

This step does not add automatic erase, compaction, or overwrite behavior yet.
Those policies should be added only when the runtime path is wired and tested.

## Hardware Checkpoint Recommendation

No hardware is required for this service-only implementation.

The first hardware checkpoint should happen after the service is wired into the
pen startup and dose-detection path:

- connect `pen` only
- confirm `begin()` succeeds
- confirm a detected dose creates a pending record
- restart the pen and confirm the pending record survives reboot

## Explicit Non-Goals

- No dose detection behavior change yet
- No BLE transfer behavior change yet
- No ACK handling yet
- No overwrite policy yet
- No full-system test yet
