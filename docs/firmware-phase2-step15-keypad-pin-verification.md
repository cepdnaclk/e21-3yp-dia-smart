# Firmware Phase 2 Step 15 - Outer Unit Keypad Pin Verification

## Status

Verified on the outer unit ESP32-S3.

The 4x4 membrane keypad was tested with standalone serial firmware. All pins and key positions worked correctly.

## Verified Wiring

Use this wiring for the keypad:

| Keypad Signal | ESP32-S3 GPIO |
| --- | --- |
| R1 | GPIO1 |
| R2 | GPIO2 |
| R3 | GPIO3 |
| R4 | GPIO4 |
| C1 | GPIO35 |
| C2 | GPIO36 |
| C3 | GPIO37 |
| C4 | GPIO38 |

Rows are driven as outputs one at a time. Columns use `INPUT_PULLUP`.

## Verified Key Map

```text
1 2 3 A
4 5 6 B
7 8 9 C
* 0 # D
```

## Display Pin Conflict Check

The verified keypad pins do not conflict with the current TFT display wiring.

Current display uses:

```text
GPIO6, GPIO7, GPIO8, GPIO9
GPIO12, GPIO13, GPIO14, GPIO15
GPIO16, GPIO17, GPIO18, GPIO21
```

Keypad uses:

```text
GPIO1, GPIO2, GPIO3, GPIO4
GPIO35, GPIO36, GPIO37, GPIO38
```

## Standalone Test Code Used

```cpp
#define R1 1
#define R2 2
#define R3 3
#define R4 4

#define C1 35
#define C2 36
#define C3 37
#define C4 38

const byte rowPins[4] = {R1, R2, R3, R4};
const byte colPins[4] = {C1, C2, C3, C4};

const char keys[4][4] = {
  {'1', '2', '3', 'A'},
  {'4', '5', '6', 'B'},
  {'7', '8', '9', 'C'},
  {'*', '0', '#', 'D'}
};

char lastKey = '\0';

void setup() {
  Serial.begin(115200);

  for (int r = 0; r < 4; r++) {
    pinMode(rowPins[r], OUTPUT);
    digitalWrite(rowPins[r], HIGH);
  }

  for (int c = 0; c < 4; c++) {
    pinMode(colPins[c], INPUT_PULLUP);
  }

  Serial.println("Keypad test started");
}

char scanKeypad() {
  for (int r = 0; r < 4; r++) {
    digitalWrite(rowPins[r], LOW);
    delayMicroseconds(50);

    for (int c = 0; c < 4; c++) {
      if (digitalRead(colPins[c]) == LOW) {
        digitalWrite(rowPins[r], HIGH);
        return keys[r][c];
      }
    }

    digitalWrite(rowPins[r], HIGH);
  }

  return '\0';
}

void loop() {
  char key = scanKeypad();

  if (key != lastKey) {
    lastKey = key;

    if (key != '\0') {
      Serial.print("Pressed: ");
      Serial.println(key);
    } else {
      Serial.println("Released");
    }

    delay(80);
  }

  delay(20);
}
```

## Next Step

When adding keypad to production firmware, do not reuse the standalone test task directly.

Recommended production approach:

- Add a small keypad service/task.
- Push debounced key events into a FreeRTOS queue.
- Let the UI state machine consume key events.
- Keep key scanning independent from BLE, MQTT, and display drawing.
