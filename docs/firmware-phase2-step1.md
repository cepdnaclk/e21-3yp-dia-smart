# Firmware Phase 2 Step 1

Date: 2026-06-05
Branch: `ananthu-dev`
Step: Add pen persistent dose record model

## Goal

Define the pen-side persistent dose record contract before changing the current
dose detection or BLE transfer behavior.

## What Changed

- Added `firmware/pen-unit/src/models/persistent_dose_record.h`
- Defined `DoseRecordStatus`
- Defined `PersistentDoseRecord`
- Added small fixed record capacity constants

## Why This Matters

The pen must eventually save a confirmed dose before it tries to connect or
transfer data to the outer unit. This model provides the fixed-size record shape
needed for that storage work.

## Why This Is Safe

This step does not change:

- current dose detection task
- current BLE notify task
- current pen queue behavior
- current outer-unit behavior
- current backend JSON payload

## Memory Constraint

The pen board is a Seeed Studio XIAO ESP32C3 class device, so the record model
is intentionally fixed-size and small. The initial record capacity is 16 records,
which is enough for short offline periods without turning the pen into a large
storage system.

## Hardware Checkpoint Recommendation

No hardware is required for this model-only step.

The next hardware checkpoint should happen after storage is wired into the pen
dose detection path:

- connect `pen` only for local detection/storage verification
- connect `pen + outer` only when BLE pending-record transfer is added

## Explicit Non-Goals

- No NVS/flash writes yet
- No ACK handling yet
- No BLE protocol change yet
- No outer-unit changes yet
