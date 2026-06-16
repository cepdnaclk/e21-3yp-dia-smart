# Firmware Offline Queue + UI Handoff

## Current Status

Date left off: 2026-06-12.

Branch: `ananthu-dev`

Current git status at handoff:

```text
## ananthu-dev...origin/ananthu-dev [ahead 2]
 M firmware/outer-unit/src/managers/display_state_manager.cpp
 M firmware/outer-unit/src/managers/display_state_manager.h
 M firmware/outer-unit/src/models/display_state.h
 M frontend/web-dashboard/src/services/dashboardService.ts
```

Important:

- The two firmware commits below are already committed locally but not pushed.
- The three `firmware/outer-unit` display-state files are partial uncommitted work.
- `frontend/web-dashboard/src/services/dashboardService.ts` is unrelated user/team work. Do not touch it.
- Do not modify inner unit, pen unit, glucometer BLE logic, backend, or frontend.

## Already Completed And Committed Locally

### Commit 1

```text
b8a25b5 fix(firmware/outer-unit): make mqtt publish report failures
```

What it did:

- Changed `publishTelemetry()` to return `true/false`.
- Changed `connectMQTT()` to return `true/false`.
- Prevented MQTT from blocking forever when WiFi/MQTT is offline.
- Added `mqttState()`.
- Updated `mqtt_publish_task.cpp` to attempt reconnect every 5 seconds instead of blocking.

Files changed:

```text
firmware/outer-unit/src/services/mqtt_service.cpp
firmware/outer-unit/src/services/mqtt_service.h
firmware/outer-unit/src/tasks/mqtt_publish_task.cpp
```

Build result after this commit:

```text
SUCCESS
RAM:   20.5%
Flash: 44.1%
```

### Commit 2

```text
05d4f24 feat(firmware/outer-unit): queue telemetry json while offline
```

What it did:

- Added LittleFS-backed offline JSON queue.
- Stores exact compact backend JSON payloads.
- Max records: `50`.
- Max JSON size: `2048 bytes`.
- Retries queued JSON before sending new telemetry.
- Preserves ordering: if queue has old records, new telemetry is queued too.
- Drops oldest only if queue is full.

Files changed/added:

```text
firmware/outer-unit/src/config/app_config.h
firmware/outer-unit/src/services/offline_json_queue_service.h
firmware/outer-unit/src/services/offline_json_queue_service.cpp
firmware/outer-unit/src/tasks/mqtt_publish_task.cpp
```

Build result after this commit:

```text
SUCCESS
RAM:   20.5% / 327680 bytes
Flash: 45.5% / 3342336 bytes
```

## Exact Place Work Was Left

Work stopped while starting the display/UI status layer.

Partial uncommitted changes exist in:

```text
firmware/outer-unit/src/models/display_state.h
firmware/outer-unit/src/managers/display_state_manager.h
firmware/outer-unit/src/managers/display_state_manager.cpp
```

What was partially added:

- Display status fields:
  - active page
  - WiFi connected
  - MQTT connected
  - MQTT retrying
  - offline queue ready
  - offline queue count
  - last publish OK
  - MQTT state
  - last publish time
  - last inner/glucose/dose activity timestamps
- Display manager API declarations:
  - `updateDisplayConnectivity(...)`
  - `updateDisplayOfflineQueue(...)`
  - `updateDisplayPage(...)`
  - `updateDisplayActivity(...)`
- Partial implementation of those APIs in `display_state_manager.cpp`.

This partial display-state work has not been built or committed yet.

## Next Agent Instructions

Only work inside:

```text
firmware/outer-unit/src
firmware/outer-unit/platformio.ini
docs
```

Do not touch:

```text
firmware/inner-unit
firmware/pen-unit
firmware/common BLE packet format
firmware/outer-unit/src/managers/ble_manager.cpp
firmware/outer-unit/src/tasks/keypad_task.cpp unless strictly needed for page switching
backend
frontend
```

Especially do not touch:

```text
frontend/web-dashboard/src/services/dashboardService.ts
```

It is currently modified and unrelated.

## Required Next Work

### Step 3: Finish Display Status State

Finish and verify these files:

```text
firmware/outer-unit/src/models/display_state.h
firmware/outer-unit/src/managers/display_state_manager.h
firmware/outer-unit/src/managers/display_state_manager.cpp
```

Make sure telemetry updates preserve:

- active page
- dose prompt state
- WiFi/MQTT/queue status
- last activity timestamps

### Step 4: Wire MQTT Status Into Display State

Update:

```text
firmware/outer-unit/src/tasks/mqtt_publish_task.cpp
```

Needed behavior:

- Call `updateDisplayConnectivity(...)` after MQTT connection attempts and publish attempts.
- Call `updateDisplayOfflineQueue(...)` after:
  - queue begin
  - enqueue
  - successful pop
  - failed retry
- Show status:
  - WiFi connected/offline
  - MQTT connected/offline/retrying
  - queue count
  - last publish OK/fail

Do not change the JSON payload structure.

### Step 5: Add UI Pages

Update:

```text
firmware/outer-unit/src/tasks/display_ui_task.cpp
firmware/outer-unit/src/tasks/event_aggregator_task.cpp
```

Required page keys:

```text
A = Dashboard
B = Device Status
C = Alerts
D = Queue / Sync Status
```

Current dose confirmation screen must keep priority. If dose prompt is active, show dose prompt regardless of selected page.

### Page Contents

#### A Dashboard

- Last dose integer units and injected time.
- Last glucose value and sequence.
- Inner temperature.
- Door state.
- Stock percent.
- Top status bar.

#### B Device Status

- WiFi connected/offline.
- WiFi RSSI.
- MQTT connected/retrying/offline.
- MQTT state code.
- BLE RSSI.
- Inner last packet age.
- Free heap.

#### C Alerts

Show color-coded alerts:

- Offline / queued data.
- Temperature high/low.
- Door open.
- Low insulin stock.
- Low inner battery.

#### D Queue / Sync Status

- Offline queue count.
- Queue ready/not ready.
- Last publish OK/fail.
- MQTT retrying/connected.
- Optional later: force retry, but do not implement force retry unless asked.

### Top Bar

Show top bar on all non-dose pages:

```text
WiFi OK/BAD | MQTT OK/BAD | BLE OK/BAD | IN OK/BAD | Q:N
```

Use text-safe symbols only, no Unicode checkmarks.

## Important Existing Behavior To Preserve

Do not break:

- Pen BLE dose receive.
- Pen time sync and ACK behavior.
- Dose confirmation:
  - Decimal dose is rounded to integer.
  - `A` confirms.
  - `B` edit mode.
  - digits only for edited dose.
  - `D` submit edit.
  - `#` clear edit buffer.
  - auto-send after `40s`.
  - no cancel path.
- Glucometer BLE connection and sequence dedupe.
- Inner ESP-NOW receive and JSON update.
- Backend JSON field names and structure.

## Build And Upload Commands

Build:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\outer-unit -e esp32-s3-devkitc-1
```

Upload outer:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\outer-unit -e esp32-s3-devkitc-1 -t upload --upload-port COM9
```

Monitor outer:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe device monitor -p COM9 -b 115200
```

## Recommended Commit Plan

Commit 3:

```text
feat(firmware/outer-unit): expose mqtt queue status to display
```

Commit 4:

```text
feat(firmware/outer-unit): add status and alert display pages
```

Commit 5:

```text
docs(firmware): document offline queue and display pages
```

After all commits:

```powershell
git push origin ananthu-dev
git checkout develop
git pull origin develop
git merge ananthu-dev --no-edit
git push origin develop
git checkout ananthu-dev
git merge develop --no-edit
git push origin ananthu-dev
```

Only do the branch sync after build and hardware upload are successful.
