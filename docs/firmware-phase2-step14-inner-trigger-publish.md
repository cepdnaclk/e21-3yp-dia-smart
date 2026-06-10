# Firmware Phase 2 Step 14 - Inner Unit Triggered Publish Fix

## Status

Implemented, built, and uploaded for test.

This step fixes the behavior where inner-unit changes, especially door open/close, were received by the outer unit but did not immediately create a backend telemetry event.

## Root Cause

The inner unit was already sending ESP-NOW packets every sample interval.

The problem was in the outer unit:

```text
Outer received InnerPacket
Outer updated lastInner
Outer only published when dose/glucose arrived or 30s periodic tick happened
```

So door open/close could look like it was not sent, because it waited until the next periodic publish or another trigger.

## Fix

The outer unit now compares the newest inner packet against the last published inner snapshot.

The inner unit was also updated so the door reed switch is no longer blocked by the full sensor sampling cycle:

```text
Door poll interval: 50ms
Door debounce: 250ms
Door packet send: immediate after debounce
Full temp/weight/battery sample: every 3000ms
```

It immediately publishes when any of these change:

- First valid inner packet after boot
- Door open/close state changes
- Temperature changes by configured delta
- Weight changes by configured delta
- Inventory percentage changes by configured delta
- Inner battery crosses the low threshold

## Outer Trigger Thresholds

Configured in `firmware/outer-unit/src/config/app_config.h`:

```cpp
#define INNER_TEMP_EVENT_DELTA_C     0.5f
#define INNER_WEIGHT_EVENT_DELTA_G   2.0f
#define INNER_INVENTORY_EVENT_DELTA_PERCENT 2.0f
#define INNER_BATTERY_LOW_PERCENT    20
```

The outer unit processes one queued inner packet per aggregator loop instead of draining the queue and keeping only the newest state. This prevents fast open/close transitions from being collapsed before JSON is built.

## Battery Percentage Test Scale

The inner-unit battery full-scale is now set to `3700mV` because the current test source is treated as a maximum 3.7V battery.

Configured in `firmware/inner-unit/src/config/app_config.h`:

```cpp
#define BATTERY_EMPTY_MV 3300
#define BATTERY_FULL_MV  3700
```

If later testing uses a real Li-ion cell directly, remember that a fully charged one-cell Li-ion is normally about `4.2V`, so this may need to be changed back after power design is finalized.

## Expected Test Behavior

When the door reed switch changes:

```text
Inner serial:
[Sensors] Sent seq=... door=OPEN

Outer serial:
[EventAgg] InnerPacket seq=... door=OPEN
```

Then the outer should immediately build telemetry and publish it instead of waiting for the 30-second periodic tick.

## Upload Commands

Inner unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\inner-unit -e esp32dev -t upload --upload-port COM12
```

Outer unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\outer-unit -e esp32-s3-devkitc-1 -t upload --upload-port COM9
```
