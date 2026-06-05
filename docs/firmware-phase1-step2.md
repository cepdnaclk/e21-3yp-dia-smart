# Firmware Phase 1 Step 2

Date: 2026-06-05
Branch: `ananthu-dev`
Step: Add shared identity and timing metadata types

## Goal

Prepare the firmware for reliable inter-device event tracking without changing:

- the current backend JSON payload
- the live inner-unit ESP-NOW packet size
- the live pen BLE payload format

## What Changed

- Added shared metadata types in `firmware/common/models/event_identity.h`
  - `SourceIdentity`
  - `SourceTiming`
  - `EventIdentity`
- Added these metadata fields to the internal `TelemetryEvent` model only

## Why This Is Safe

This step does not change:

- `firmware/common/protocols/espnow_packets.h`
- `firmware/common/protocols/ble_packets.h`
- `firmware/outer-unit/src/services/json_serializer_service.cpp`

That means:

- no current inter-device wire format changed
- no backend DTO contract changed
- no outer JSON payload shape changed

## Why This Matters

Later phases need a consistent place to track:

- which device created an event
- which boot/session it belongs to
- which source sequence number produced it
- when the source created it
- when the outer unit received it

Adding these types now lets later protocol work target one shared internal model first, instead of introducing identity fields ad hoc in multiple places.

## Hardware Checkpoint Recommendation

No hardware connection is required for this step.

The next meaningful hardware checkpoint should be when we change a live transport boundary:

- inner <-> outer packet layout, or
- pen <-> outer BLE transfer format

At that point, connect only the units involved in that protocol change, not the full system.

## Explicit Non-Goals

- No serializer changes
- No packet layout changes for inner-unit traffic
- No packet layout changes for pen-unit traffic
- No ACK handling yet
- No scheduling changes yet
