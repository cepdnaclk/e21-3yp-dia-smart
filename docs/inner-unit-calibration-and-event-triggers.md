# Inner Unit Calibration and Event Trigger Tuning

## Where to Change Values
Edit:

```text
firmware/inner-unit/src/config/app_config.h
```

This file now keeps the inner unit hardware pins, sensor calibration values, and event-trigger thresholds in one place.

## Load Cell Calibration
Use these values:

```cpp
#define LOAD_CELL_CALIBRATION    245.0f
#define HX711_AVERAGES           3
#define FULL_BOTTLE_WEIGHT_G     300.0f
```

Calibration process:

1. Remove all weight from the load cell.
2. Boot the inner unit so `scale.tare()` zeros the current reading.
3. Place a known weight on the load cell, for example `100g` or `500g`.
4. Watch the serial monitor reading.
5. Adjust `LOAD_CELL_CALIBRATION` until the printed weight matches the known weight.
6. After calibration, place a full insulin bottle/package and set `FULL_BOTTLE_WEIGHT_G` to that measured value.

Tuning notes:

- If the shown weight is too high, increase or decrease `LOAD_CELL_CALIBRATION` based on the HX711 direction observed on your setup.
- If readings jump too much, increase `HX711_AVERAGES` from `3` to `5` or `10`.
- Higher `HX711_AVERAGES` gives smoother values but slower sampling.

## Temperature Tuning
Use these values:

```cpp
#define TEMP_MIN_C               2.0f
#define TEMP_MAX_C               8.0f
#define TEMP_EVENT_DELTA_C       0.5f
```

Meaning:

- `TEMP_MIN_C` and `TEMP_MAX_C` define the safe fridge range.
- `TEMP_EVENT_DELTA_C` controls when a changed temperature is important enough to send.

Suggested start:

- Keep safe range at `2.0C` to `8.0C`.
- Use `0.5C` event delta first.
- If packets are too frequent, increase to `1.0C`.

## Weight and Inventory Event Tuning
Use these values:

```cpp
#define WEIGHT_EVENT_DELTA_G     2.0f
#define INVENTORY_EVENT_DELTA_PERCENT 2.0f
```

Meaning:

- `WEIGHT_EVENT_DELTA_G` sends when measured weight changes enough.
- `INVENTORY_EVENT_DELTA_PERCENT` sends when calculated stock percent changes enough.

Suggested start:

- Start with `2g` for weight.
- Start with `2%` for inventory.
- If load cell noise causes too many events, increase weight delta to `5g`.

## Door Event Tuning
Use this value:

```cpp
#define DOOR_EVENT_DEBOUNCE_MS   250
```

Meaning:

- Door event should be sent only after the reed switch state is stable for this time.
- This prevents false open/close flicker when the magnet is near the sensor edge.

Suggested start:

- Use `250ms`.
- If door flickers, increase to `500ms`.
- If door response feels slow, reduce to `100ms`.

## Heartbeat
Use this value:

```cpp
#define INNER_HEARTBEAT_MS       60000
```

Meaning:

- The inner unit should still send a packet even when nothing changed.
- This tells the outer unit that the inner unit is alive and refreshes the last known fridge state.

Suggested start:

- Use `60000ms` (`1 minute`) while testing.
- Later, for lower power, increase to `300000ms` (`5 minutes`).

## Future Event Trigger Rule
The next firmware implementation should send an ESP-NOW packet when any condition is true:

```text
door changed and debounce passed
temperature changed by TEMP_EVENT_DELTA_C or crossed safe range
weight changed by WEIGHT_EVENT_DELTA_G
inventory percent changed by INVENTORY_EVENT_DELTA_PERCENT
heartbeat interval reached
```

It should not send every sample unless one of these conditions is true.

