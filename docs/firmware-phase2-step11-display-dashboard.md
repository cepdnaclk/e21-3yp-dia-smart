# Firmware Phase 2 Step 11 - Outer Display Dashboard

## Goal
Show a simple local dashboard on the outer unit TFT display without changing backend JSON, BLE payloads, MQTT topics, or sensor queues.

## Display Pin Map
This firmware follows the PCB display wiring:

```text
LCD_CS   -> GPIO21
LCD_D0   -> GPIO35
LCD_D1   -> GPIO36
LCD_D2   -> GPIO37
LCD_D3   -> GPIO39
LCD_D4   -> GPIO40
LCD_D5   -> GPIO41
LCD_D6   -> GPIO42
LCD_D7   -> GPIO47
LCD_WR   -> GPIO48
LCD_RS   -> GPIO10
LCD_RST  -> GPIO11
LCD_RD   -> pulled up to 3.3V through 10k
```

The display is configured as write-only 8-bit parallel using `TFT_eSPI`.

## Dashboard Fields
The dashboard shows:

- Fridge temperature from inner unit.
- Door open/closed from inner unit.
- Insulin stock percentage from inner unit.
- Bottle/package weight from inner unit.
- Last glucose value from glucometer.
- Last dose amount from pen.
- Last dose timestamp from pen/outer time sync.
- WiFi RSSI, BLE RSSI, and free heap.

## Design Constraint
The display task reads a copied telemetry snapshot from `display_state_manager`.
It does not consume `telemetryQueue`, `doseQueue`, `glucoseQueue`, or `innerPacketQueue`.
This keeps backend publishing behavior unchanged.

## Verification
- Built outer firmware for `esp32-s3-devkitc-1`.
- Uploaded outer firmware to `COM9`.
- Build memory remained acceptable:
  - RAM: about 20.9%
  - Flash: about 45.0%

## Expected Serial Log
```text
[Display] UI task started
[EventAgg] InnerPacket ...
[EventAgg] New dose: ...
[EventAgg] New glucose: ...
```

