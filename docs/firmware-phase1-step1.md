# Firmware Phase 1 Step 1

Date: 2026-06-05
Branch: `ananthu-dev`
Step: Centralize shared firmware packet structs

## Goal

Move the shared inter-unit packet definitions into `firmware/common` without changing the outer unit JSON payload sent to the backend.

## Backend Contract Constraint

The outer unit must keep emitting the same JSON shape currently consumed by the Spring backend MQTT DTOs:

- `eventId`
- `eventType`
- `trigger`
- `timestamp`
- `schemaVersion`
- `sequenceNumber`
- `replayedEvent`
- `patient`
- `gateway`
- `storage`
- `inventory`
- `glucose`
- `dose`
- `battery`

This step does not change field names, nesting, or serializer logic in `firmware/outer-unit/src/services/json_serializer_service.cpp`.

## What Changed

- Added shared `EventTrigger` enum in `firmware/common/config/event_types.h`
- Added shared `InnerPacket` contract in `firmware/common/protocols/espnow_packets.h`
- Added shared `GlucoseReading` and `DoseReading` contracts in `firmware/common/protocols/ble_packets.h`
- Added shared `TelemetryEvent` model in `firmware/common/models/telemetry_event.h`
- Converted unit-local headers into wrappers around the shared headers

## Why This Matters

Before this step, inner and outer units had duplicate struct definitions for the same packet contracts.
That creates drift risk and can silently break inter-device communication.

After this step, the shared structs have one source of truth while the rest of the firmware behavior stays unchanged.

## Explicit Non-Goals

- No JSON payload shape changes
- No backend DTO changes
- No ACK protocol yet
- No scheduling changes yet
- No persistent storage changes yet

## Files Added

- `firmware/common/config/event_types.h`
- `firmware/common/protocols/espnow_packets.h`
- `firmware/common/protocols/ble_packets.h`
- `firmware/common/models/telemetry_event.h`

## Files Updated

- `firmware/inner-unit/src/models/inner_event.h`
- `firmware/outer-unit/src/models/telemetry_event.h`
- `firmware/outer-unit/src/include/system_queues.h`
