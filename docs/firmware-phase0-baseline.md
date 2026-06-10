# Firmware Phase 0 Baseline

Date: 2026-06-04
Branch: `ananthu-dev`
Scope: `firmware/inner-unit`, `firmware/outer-unit`, `firmware/pen-unit`

## Purpose

This note records the current firmware behavior before Phase 1 refactoring starts.
It is intended to keep each later firmware commit grounded in a known baseline.

## Current Architecture

### Inner Unit

- Locks Wi-Fi radio to the ESP-NOW channel and then disconnects from Wi-Fi.
- Samples:
  - DS18B20 temperature
  - reed-switch door state
  - HX711 weight
  - estimated remaining percent
- Broadcasts `InnerPacket` over ESP-NOW.
- Has no durable local storage for unsent events.
- Has no application-level ACK from the outer unit.

### Pen Unit

- Reads AS5600 angle and a confirm button.
- Calculates dose from angular movement.
- Pushes `DoseEvent` into a local queue.
- Exposes dose values to the outer unit through BLE notify.
- Does not persist confirmed doses before BLE transfer.
- Does not track delivered versus acknowledged events.

### Outer Unit

- Connects to Wi-Fi and AWS IoT.
- Receives inner-unit data over ESP-NOW.
- Connects to the pen over BLE and receives dose notify payloads.
- Connects to the glucometer over BLE and fetches readings.
- Aggregates latest known state into a `TelemetryEvent`.
- Publishes JSON telemetry to AWS IoT MQTT.

## What Currently Works

- Inner unit can sample and broadcast sensor state.
- Pen unit can detect and notify dose values.
- Outer unit can receive inner packets.
- Outer unit can receive pen dose notifies.
- Outer unit can perform glucometer sync with the current BLE flow.
- Outer unit can publish MQTT telemetry to AWS IoT.
- Spring backend can subscribe to the current telemetry topic.

## Main Gaps

### Delivery Reliability

- Pen doses are not stored durably before transfer.
- Inner ESP-NOW delivery is fire-and-forget.
- Outer MQTT publish has reconnect logic, but no durable replay queue.
- Queue overflow currently drops events silently at runtime except for debug logs.

### Coordination

- Outer unit periodically leaves pen work to sync the glucometer.
- Pen activity is not treated as the highest-priority workflow.
- The system behaves like multiple working prototype links instead of a coordinated hub.

### Data Freshness

- Outer unit carries forward last-known values indefinitely.
- There is no per-source freshness timeout.
- A stale inner or pen value can still appear current in the combined payload.

### Contract Drift Risk

- Shared packet definitions are duplicated between units.
- The `InnerPacket` contract exists separately in inner and outer sources.
- This creates a high risk of future struct mismatch.

### Health Telemetry

- Battery values in the outer aggregator are placeholders.
- Some telemetry looks complete even when the values are not device-measured.

## Known Firmware Design Problems To Fix Next

1. Centralize shared packet definitions into `firmware/common`.
2. Add sequence, timestamp, and source identity fields to inter-device messages.
3. Persist pen events locally before BLE delivery.
4. Add ACK-based delivery between pen and outer.
5. Add freshness tracking in the outer unit.
6. Change outer scheduling so pen sync has higher priority than glucometer polling.
7. Add durable replay for cloud publish failures.

## Baseline Files Reviewed

- `firmware/inner-unit/src/main.cpp`
- `firmware/inner-unit/src/tasks/sensor_sampling_task.cpp`
- `firmware/inner-unit/src/models/inner_event.h`
- `firmware/pen-unit/src/main.cpp`
- `firmware/pen-unit/src/tasks/dose_detection_task.cpp`
- `firmware/pen-unit/src/tasks/ble_transfer_task.cpp`
- `firmware/pen-unit/src/models/dose_event.h`
- `firmware/outer-unit/src/main.cpp`
- `firmware/outer-unit/src/managers/ble_manager.cpp`
- `firmware/outer-unit/src/tasks/event_aggregator_task.cpp`
- `firmware/outer-unit/src/tasks/mqtt_publish_task.cpp`
- `firmware/outer-unit/src/services/mqtt_service.cpp`
- `firmware/outer-unit/src/services/json_serializer_service.cpp`
- `firmware/outer-unit/src/include/system_queues.h`

## Phase 0 Exit Condition

Phase 0 is complete when the baseline and workflow are documented in-repo and the team can start Phase 1 with small isolated commits.
