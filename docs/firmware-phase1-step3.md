# Firmware Phase 1 Step 3

Date: 2026-06-05
Branch: `ananthu-dev`
Step: Define shared ACK protocol types

## Goal

Define one shared ACK protocol for future reliable inter-device delivery
without changing any current runtime behavior.

## What Changed

- Added shared ACK protocol constants and enums in
  `firmware/common/protocols/ack_protocol.h`
- Added a shared packed `AckMessage` struct in
  `firmware/common/models/ack_message.h`

## What This Enables Later

The later pen, inner, and outer phases now have one common contract for:

- event accepted
- duplicate event
- invalid event
- retry requested
- event not found

It also establishes shared fields for:

- protocol version
- source peer type
- target peer type
- boot/session id
- source sequence
- stable event uid
- retry hint

## Why This Is Safe

This step does not change:

- current ESP-NOW packet layouts
- current pen BLE payload format
- current outer JSON payload
- current backend DTO handling
- current runtime logic in any unit

## Hardware Checkpoint Recommendation

No hardware is required for this step.

The first hardware checkpoint for ACK behavior should be when ACK wiring starts:

- `pen + outer` for dose delivery ACK
- later `inner + outer` for event ACK

## Explicit Non-Goals

- No ACK sending yet
- No ACK receiving yet
- No resend logic yet
- No buffer policy yet
- No scheduler changes yet
