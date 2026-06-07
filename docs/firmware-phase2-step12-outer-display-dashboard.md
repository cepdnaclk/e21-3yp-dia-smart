# Firmware Phase 2 Step 12 - Outer Unit Display Dashboard

## Status

Working and uploaded on the outer unit ESP32-S3.

This step adds the outer-unit TFT dashboard using a direct raw 8-bit parallel bus driver. The display was confirmed working after replacing the first PCB pin map with a safer GPIO map and avoiding TFT_eSPI / Arduino_GFX controller initialization issues.

## Final Display Wiring

Use this wiring for the 3.5 inch Arduino UNO style TFT shield:

| TFT Signal | ESP32-S3 GPIO |
| --- | --- |
| LCD_CS | GPIO9 |
| LCD_RS / DC | GPIO8 |
| LCD_RST | GPIO6 |
| LCD_WR | GPIO7 |
| LCD_RD | Pull up to 3.3V through 10k |
| LCD_D0 | GPIO12 |
| LCD_D1 | GPIO13 |
| LCD_D2 | GPIO14 |
| LCD_D3 | GPIO15 |
| LCD_D4 | GPIO16 |
| LCD_D5 | GPIO17 |
| LCD_D6 | GPIO18 |
| LCD_D7 | GPIO21 |

Do not use the earlier GPIO35, GPIO36, GPIO37, GPIO39, GPIO40, GPIO41, GPIO42, GPIO47 map for this display. Those pins caused unreliable display bring-up on the ESP32-S3 board and can conflict with board-specific flash/PSRAM routing.

## LCD_RD Pull-Up

The display is used in write-only mode. `LCD_RD` must not float.

Correct connection:

```text
LCD_RD -> one side of 10k resistor
other side of 10k resistor -> ESP32-S3 3.3V
```

No firmware GPIO is assigned for `LCD_RD`.

## Firmware Approach

The final working implementation does not use `TFT_eSPI` or `Arduino_GFX`.

Reason:

- `TFT_eSPI` stayed white with this shield and wiring.
- `Arduino_GFX` displayed but orientation/layout was not acceptable.
- The raw bus test proved that the data/control wiring works.
- The manual raw renderer is smaller, predictable, and avoids hidden display buffers.

The driver writes directly to the ESP32 GPIO output registers and sends the minimum LCD commands:

```text
0x01 software reset
0x11 sleep out
0x3A pixel format = 16-bit RGB565
0x36 MADCTL = 0x40
0x13 normal display mode
0x29 display on
```

`MADCTL = 0x40` is the confirmed portrait orientation and corrected color order for this wiring.

## Dashboard Content

The current outer display shows:

- Fridge temperature
- Door state
- Insulin stock percentage
- Inventory weight
- Last glucose value
- Last dose
- Last dose time
- WiFi RSSI, BLE RSSI, and heap footer

The dashboard reads from `DisplayState`, so it does not change the backend JSON payload and does not interfere with MQTT publishing.

## Memory Check

Latest outer-unit build after the raw dashboard:

```text
Flash/app partition used: 1,460,497 / 3,342,336 bytes
Flash usage: 43.7%
Static RAM used: 66,636 / 327,680 bytes
Static RAM usage: 20.3%
```

There is enough space for keypad support and manual patient workflows if we keep the implementation lightweight.

Recommended limits for the next keypad/menu work:

- Avoid full-screen frame buffers or image assets.
- Avoid large `String` allocations in UI code.
- Use a small keypad task stack, around 3072 to 4096 bytes.
- Use a simple UI state machine instead of a heavy UI library.
- Keep patient actions in small queues/events and persist only necessary confirmation data.

## Next Firmware Step

Add the 4x4 keypad as a separate small step:

1. Add keypad pin config.
2. Add keypad scan task with debounce.
3. Add simple UI state machine.
4. Add screens for dose confirmation and manual dose entry.
5. Keep backend JSON unchanged unless backend explicitly adds fields for manual confirmation events.

## Useful Commands

Upload outer unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\outer-unit -e esp32-s3-devkitc-1 -t upload --upload-port COM9
```

Monitor outer unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe device monitor -p COM9 -b 115200
```
