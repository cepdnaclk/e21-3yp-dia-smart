# Firmware Phase 2 Step 10 - Outer BLE Scan Scheduler

## Goal
Make the outer unit share BLE time between the pen and glucometer instead of staying attached to one device too long.

## Change
- Outer scans the pen in short windows:
  - `2s` scan window.
  - `3s` idle delay before the next pen scan.
- Outer attempts glucometer sync on a scheduled window:
  - `10s` glucometer scan window.
  - first attempt after about `5s`, so boot starts with a pen scan.
  - `30s` interval between glucometer sync attempts.
- Pen connections are now short sessions:
  - connect to pen.
  - send time sync.
  - receive dose notifications.
  - send ACKs.
  - disconnect after the hold window if no ACKs remain.
- Existing pen and glucometer payload parsing is unchanged.
- Backend JSON is unchanged.

## Why
The pen now advertises slowly when idle and fast when it has data.
The outer should therefore avoid holding a permanent pen connection.
Short sessions let the pen return to low-duty advertising and leave BLE time for the glucometer.

## Expected Logs
```text
[BLE] Glucometer sync due; scanning glucometer before pen
[BLE] Scanning for glucometer...
[BLE] Glucometer not found; returning to pen scan
[BLE] Scanning for pen window...
[BLE] Pen connected, RSSI=-55 dBm
[BLE] Pen session complete; disconnecting to resume scan windows
```

## Inner Unit Note
The inner unit still sends ESP-NOW packets periodically.
Event-triggered inner sending should be a separate step using local change detection plus a heartbeat:
- send immediately on door state change.
- send immediately on meaningful temperature or weight change.
- still send a periodic heartbeat so the outer knows the inner is alive.

## Verification
- Built outer firmware for `esp32-s3-devkitc-1`.
- Uploaded outer firmware to `COM9`.
- Build memory remained acceptable for ESP32-S3:
  - RAM: about 20.3%
  - Flash: about 43.5%
