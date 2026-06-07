# Firmware Phase 2 Step 8 - Glucometer Sequence Dedupe

## Scope

- Unit: outer BLE manager only.
- Backend JSON shape remains unchanged.
- Pen BLE flow is unchanged.

## Behavior

- Outer tracks the last accepted glucometer sequence number in RAM.
- If the glucometer notifies the same sequence number again, outer ignores it.
- The dedupe state updates only after the reading is successfully queued to `glucoseQueue`.
- If `glucoseQueue` is full, the sequence is not marked accepted, so the reading can be retried later.

## Validation

Expected serial behavior:

- First new reading logs `Glucose: <value> mg/dL (seq=<n>)`.
- Repeated notification with the same sequence logs `Duplicate glucose seq=<n> ignored`.

## Next Step

Persist the last accepted glucometer sequence in NVS so duplicates are still suppressed after outer reboot.
