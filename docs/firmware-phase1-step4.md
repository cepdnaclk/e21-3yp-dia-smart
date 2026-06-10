# Firmware Phase 1 Step 4

Date: 2026-06-05
Branch: `ananthu-dev`
Step: Define shared source freshness/state model

## Goal

Define common source-state types for future freshness and missing-data handling,
without changing runtime behavior yet.

## What Changed

- Added `firmware/common/models/source_state.h`
- Defined shared enums:
  - `SourceAvailability`
  - `SourceDataQuality`
  - `SourceKind`
- Defined shared `SourceState`
- Added default freshness timeout constants

## Why This Matters

The outer unit needs a consistent way to decide whether data is:

- fresh
- stale
- unavailable
- valid
- partial
- invalid

This is required before changing payload-building behavior because the backend
JSON must remain deliberate and predictable when some devices are offline.

## Missing Data Policy Target

Later runtime steps should use this model to decide:

- use fresh source data when available
- use cached data only while it is still inside its freshness window
- mark unavailable sources as `UNKNOWN`
- avoid sending fake numeric values
- preserve the existing backend JSON field names and nesting

## Why This Is Safe

This step does not change:

- current ESP-NOW packet layouts
- current pen BLE payload format
- current glucometer sync behavior
- current outer JSON payload
- current backend DTO handling
- current runtime logic in any unit

## Hardware Checkpoint Recommendation

No hardware is required for this step.

The next hardware checkpoint should happen when source-state logic is wired into
the outer unit cache:

- `inner + outer` for storage/inventory freshness
- `pen + outer` for dose freshness
- `outer + glucometer` for glucose freshness

## Explicit Non-Goals

- No cache implementation yet
- No JSON missing-data changes yet
- No ACK/resend behavior yet
- No scheduler changes yet
