# Firmware Phase 2 Step 13 - Inner Unit Battery ADC Monitor

## Status

Implemented, built, and uploaded for test.

This step adds one-cell Li-ion battery voltage monitoring to the inner unit using ADC1 GPIO34 and a 100k/100k resistor divider. The outer unit now receives the battery percentage through the existing ESP-NOW inner packet and sends it in the existing backend JSON field:

```json
"battery": {
  "innerUnitPercent": <real inner ADC estimate>
}
```

The backend JSON shape is unchanged.

## Wiring

Use this wiring for a 3.7V one-cell Li-ion/LiPo battery:

```text
Battery + -> 100k resistor -> ADC node -> GPIO34 / D34
ADC node  -> 100k resistor -> GND
Battery - -> ESP32 GND
```

Optional noise filter:

```text
GPIO34 / ADC node -> 100nF capacitor -> GND
```

Do not connect battery positive directly to GPIO34. Only the middle point of the resistor divider goes to GPIO34.

## Why GPIO34

GPIO34 is ADC1. The inner unit uses WiFi/ESP-NOW, so ADC2 pins should be avoided because ADC2 reads can fail while WiFi is active.

Current inner-unit pin usage:

| Function | Pin |
| --- | --- |
| Battery ADC | GPIO34 |
| Door reed switch | GPIO4 |
| HX711 DOUT | GPIO5 |
| HX711 CLK | GPIO18 |
| DS18B20 temperature | GPIO21 |

## Voltage Calculation

The divider uses equal resistors:

```text
100k top, 100k bottom
```

So the ADC node receives half the battery voltage:

```text
Battery 4.2V -> ADC about 2.1V
Battery 3.7V -> ADC about 1.85V
```

Firmware reads `analogReadMilliVolts(GPIO34)` and multiplies by `2.0`.

## Percentage Estimate

The first test implementation maps voltage linearly:

```text
3.3V -> 0%
4.2V -> 100%
```

This is good enough for initial testing, but Li-ion discharge is not perfectly linear. After comparing with a multimeter and real battery drain, tune these values in:

```cpp
#define BATTERY_EMPTY_MV 3300
#define BATTERY_FULL_MV  4200
```

## Firmware Flow

Inner unit:

1. Samples GPIO34 using `ADC_11db` attenuation.
2. Averages 16 ADC millivolt readings.
3. Calculates battery voltage using the resistor divider ratio.
4. Calculates battery percentage.
5. Adds `batteryVoltageV` and `batteryPercent` to `InnerPacket`.

Outer unit:

1. Receives the updated `InnerPacket`.
2. Uses `batteryPercent` for `TelemetryEvent.innerBatteryPercent`.
3. Sends the value through the existing backend JSON battery section.
4. Shows `INBAT <percent>%` on the display footer.

## Test Logs

Inner serial monitor should show:

```text
[Sensors] Battery 3.85V (61%)
```

Outer serial monitor should show:

```text
[EventAgg] Inner battery 3.85V (61%)
```

## Commands

Upload inner unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\inner-unit -e esp32dev -t upload --upload-port COM12
```

Upload outer unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe run -d firmware\outer-unit -e esp32-s3-devkitc-1 -t upload --upload-port COM9
```

Monitor inner unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe device monitor -p COM12 -b 115200
```

Monitor outer unit:

```powershell
C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe device monitor -p COM9 -b 115200
```
