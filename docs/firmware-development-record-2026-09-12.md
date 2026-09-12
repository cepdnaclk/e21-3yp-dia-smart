# Firmware Development and Hardware Validation Record

- Date: 2026-09-12
- Working branch: `ananthu-dev`
- Firmware units: Outer Unit, Inner Unit, and Pen Unit

## Purpose

This document records the firmware faults investigated during the September
2026 hardware session, the final implementation, the real-device test results,
and the rules that should guide future firmware development.

The two main reported problems were:

1. The glucometer delivered its first BLE reading but later readings did not
   arrive.
2. Wi-Fi credentials changed from the application did not appear to update
   both the Outer and Inner Units reliably.

A related symptom was that the Pen Unit repeatedly sent old dose data and the
Outer Unit keypad sometimes appeared unresponsive.

## Hardware Used During Validation

| Device | Connection or identity |
| --- | --- |
| Outer Unit | ESP32-S3 on `COM3` |
| Inner Unit | ESP32 device, last observed on `COM9` |
| Pen Unit BLE address | `b0:a6:04:07:6c:ea` |
| Accu-Chek Guide Me BLE address | `fc:45:c3:b7:c9:dc` |

The final Outer Unit firmware was built and uploaded to `COM3`.

## Wi-Fi Provisioning Result

Commit `dd8bb9a` (`fix: make wifi provisioning commit reliable`) contains the
Wi-Fi provisioning reliability changes and is present on both `ananthu-dev`
and `develop`.

The verified configuration was:

- Credential version: `14`
- SSID: `ByeByeBye`
- Wi-Fi channel: `6`
- Outer Unit address observed during testing: `192.168.1.163`

The Inner Unit log showed:

```text
[WiFiProvisioning] Pending configuration staged. version=14
[WiFiProvisioning] Candidate connected. version=14 channel=6; waiting for Outer commit
[WiFiProvisioning] Configuration committed. version=14 channel=6
```

The Outer Unit subsequently booted using the saved configuration:

```text
[WiFi] Connecting with saved credentials (version=14)...
SSID: ByeByeBye
[WiFi] Current radio channel: 6
```

### Intended Wi-Fi flow

1. The application publishes a versioned Wi-Fi command to the Outer Unit over
   AWS IoT MQTT while the old network is still available.
2. The Outer Unit stages the candidate credentials and sends them securely to
   the Inner Unit.
3. The Inner Unit tests the candidate network and reports whether it connected.
4. Credentials are committed only after the coordinated result is received.
5. The Outer Unit stores and uses the new credentials for MQTT.
6. The Inner Unit stores the same version and channel. It may disconnect from
   normal Wi-Fi after validation because ESP-NOW is its operational data link,
   but it must remain on the matching radio channel.

### Wi-Fi rules for future development

- Do not replace saved credentials with hardcoded credentials during normal
  operation.
- A fallback network is recovery behavior only; it must not silently become the
  reported successful result of a new provisioning request.
- Never commit one unit to the new configuration while leaving the other unit
  on a different ESP-NOW channel.
- Keep credential versions monotonic. Ignore older commands after a newer
  version has been committed.
- Do not print or publish Wi-Fi passwords in serial logs, MQTT status, or source
  control.
- Do not erase NVS during a normal firmware upload. Erasing NVS removes saved
  Wi-Fi and BLE bonding information.
- The application may retain credentials only long enough to complete a
  provisioning attempt. The devices, not the browser UI, are the source of
  truth for the committed version.
- A frontend timeout does not prove that the devices failed. Confirm with the
  Outer and Inner serial logs and the committed version reported by each unit.

## BLE Root Cause

The main BLE regression was introduced by commit `860346b`, which attempted to
keep independent Pen Unit and glucometer clients connected in parallel.

The installed `ESP32 BLE Arduino` implementation stores only one global
`BLEDevice::m_pClient` pointer. Creating a second `BLEClient` replaces that
pointer. As a result, two application-level client objects are not independent:

- GAP events can be delivered to the wrong client.
- RSSI and connection semaphores can be released through the wrong object.
- A late callback can access a deleted client and cause a `StoreProhibited`
  crash.
- A blocked glucometer connection or service discovery can prevent Pen Unit
  acknowledgements from being sent.

Commit `d95a510` correctly recognized that the Guide Me needs a fresh BLE
session for later records, but repeatedly creating sessions on top of the
parallel-client design exposed the single-client limitation more often.

Observed failure evidence included:

```text
BT_BTM: Device not found
[BLE] Glucometer authentication failed
Deregister Failed unknown client cif
```

The failed path then entered blocking GATT discovery. While that happened, the
Pen Unit did not receive its acknowledgement and repeatedly notified the same
stored dose. This notification storm made the display/keypad workflow appear
broken even though the keypad task itself was running.

## Final BLE Design

Commit `d51b94d` (`fix(firmware/outer-unit): serialize BLE device sessions`)
implements the validated design. It is present on both `ananthu-dev` and
`develop`.

### One radio owner

There is exactly one active BLE client at a time:

```text
Pen connected and listening
        |
        | short scan discovers meter
        v
ACK pending pen records and disconnect pen
        |
        v
Create fresh glucometer client
        |
        v
Authenticate -> discover GATT -> request latest RACP record
        |
        v
Receive record -> complete RACP -> disconnect meter
        |
        v
Reconnect pen
```

The Pen Unit remains connected while short glucometer scans run. It is paused
only after a meter advertisement has actually been found. This is the same
high-level behavior used by the older working firmware and avoids unnecessary
10-second pen outages.

Current scan timing:

- Glucometer scan window: 3 seconds
- Delay before the next glucometer scan: 4 seconds
- Initial glucometer scan delay after boot: 5 seconds

### Glucometer session

Each glucometer transfer uses a fresh session:

1. Discover the Glucose Service advertisement (`0x1808`).
2. Finish pending Pen Unit ACKs and disconnect the pen.
3. Create the glucometer client.
4. Set `ESP_BLE_SEC_ENCRYPT_MITM` before connecting so encryption begins from
   the real `ESP_GATTC_CONNECT_EVT` peer address.
5. Wait for authentication completion, with a 6-second limit.
6. Abort before GATT discovery if authentication fails or times out.
7. Refresh and discover the GATT services.
8. Subscribe to Glucose Measurement (`0x2A18`) notifications and RACP
   (`0x2A52`) indications.
9. Send `0x01 0x06`, requesting the latest stored record.
10. Finish on the RACP completion indication or after the 12-second timeout.
11. Disconnect the meter and return to the Pen Unit.

This fresh-session behavior fixes the original "first reading works,
subsequent readings do not" problem.

Authentication failure must never be followed by blocking GATT discovery. A
failed attempt returns to scanning, keeping the display, keypad, Inner Unit,
MQTT, and Pen Unit recoverable.

The following messages can appear during a successful session:

```text
retrieveDescriptors(): esp_ble_gattc_get_all_descr: Unknown
```

They were non-fatal on the tested hardware. A session is successful when the
measurement is queued and the RACP completion indication arrives.

### Glucometer pairing behavior

Normal use does not require pairing for every reading.

- Pair once, then take glucose readings normally.
- The Outer Unit scans, reconnects with the stored bond, downloads the latest
  record, and disconnects automatically.
- Re-enter pairing mode only after a bond is cleared, NVS is erased, the meter
  is paired with another device, or authentication repeatedly fails.
- The Guide Me supports one paired device. Pairing a new device replaces the
  previous device.
- To enter pairing mode, turn the meter off and hold both arrow buttons until
  the hourglass and Bluetooth symbols flash.
- The six-digit PIN configured for the test meter is `836337`. If hardware is
  replaced, verify the PIN printed on the new meter rather than copying this
  value blindly.

Reference: [Accu-Chek Guide Me User's Manual](https://www.accu-chek.com/document/accu-chek-guide-me-users-manual-english/download).

## Pen Unit Replay Protection

The Pen Unit stores dose records until it receives an acknowledgement. When
the old shared BLE loop blocked, the Pen Unit correctly retried, but the Outer
Unit treated some retries as new user doses.

The final implementation:

- Identifies a compact dose by both its record slot and injection epoch.
- Remembers the last accepted epoch independently for all 16 record slots.
- Queues only the first copy of a slot/epoch pair.
- Still sends an ACK when a duplicate copy is received.
- Rejects invalid slot numbers outside `0..15`.

Remembering only the immediately previous notification is insufficient because
records may arrive out of order, for example slot 1, slot 2, then slot 1 again.

The in-memory table resets after an Outer Unit reboot. This is acceptable
because acknowledged Pen Unit records should be removed. Backend uniqueness is
still the final protection against duplicate persisted events.

## Keypad Investigation

The keypad task is pinned separately from BLE and continued to run. During live
testing the monitor recorded:

```text
[Keypad] Key pressed: 1
[Keypad] Key pressed: B
[EventAgg] Dose edit mode started
```

The Pen Unit delivered a fresh dose, the `B` key entered Edit mode, and the
edited workflow completed. No alternate keypad mapping was retained because
the verified hardware mapping is correct:

```text
1 2 3 A
4 5 6 B
7 8 9 C
* 0 # D
```

If a key appears unresponsive in the future, check for a `[Keypad] Key pressed`
line first. No line indicates a scan/wiring issue; a line without a UI action
indicates a state-machine issue. Test another key in the same row and another
in the same column before changing the key map.

## Real-Hardware Verification

The final firmware compiled successfully for `esp32-s3-devkitc-1`:

- RAM: approximately 70.5 KB of 327.7 KB, 21.5 percent
- Flash: approximately 1.626 MB of 3.342 MB, 48.6 percent

The firmware was flashed to the Outer Unit on `COM3` and tested with the real
Inner Unit, Pen Unit, and Accu-Chek Guide Me.

### Glucometer session 1

```text
[BLE] Glucometer authentication succeeded
[BLE] RACP latest-record request sent
[BLE] Glucose queued immediately: 91 mg/dL seq=364
[BLE] RACP latest-record request completed
[BLE] Glucometer session finished: RACP complete
[MQTT] SUCCESS: Payload delivered to AWS.
```

### Glucometer session 2 without rebooting the Outer Unit

```text
[BLE] Glucometer authentication succeeded
[BLE] RACP latest-record request sent
[BLE] Glucose queued immediately: 102 mg/dL seq=365
[BLE] RACP latest-record request completed
[BLE] Glucometer session finished: RACP complete
[MQTT] SUCCESS: Payload delivered to AWS.
```

The different sequence numbers prove that a later reading was received through
a new BLE session, resolving the original defect.

During these tests:

- Fresh Inner Unit packet sequence numbers continued arriving over ESP-NOW.
- Outer Unit MQTT publishing continued successfully.
- Pen Unit doses were received and acknowledged.
- Repeated Pen Unit records no longer generated repeated dose prompts.
- Keypad navigation and dose Edit mode worked.

## Development Rules to Preserve

1. Do not create simultaneous `BLEClient` objects with the current ESP32 BLE
   Arduino library.
2. Separate device-specific parsing and state, but serialize access to the BLE
   client and radio connection.
3. Do not disconnect the Pen Unit merely to look for the meter. Scan first;
   disconnect only after the meter is found.
4. Authenticate before GATT discovery. On authentication failure, abort the
   session and return to scanning.
5. Use a fresh glucometer connection for every RACP latest-record request.
6. Keep glucometer sequence-number deduplication. Do not publish the same meter
   sequence as a new glucose event.
7. ACK duplicate Pen Unit records even when they are not queued again.
8. Never perform long work inside BLE notification callbacks. Copy the parsed
   event into a FreeRTOS queue and return.
9. Treat serial messages as evidence. Do not declare a peripheral working from
   discovery alone; require authentication, data receipt, protocol completion,
   and MQTT delivery where applicable.
10. Test at least two glucometer sessions without rebooting the Outer Unit.
11. Test a Pen Unit dose and keypad action while glucometer scanning is active.
12. Keep Inner and Outer Unit Wi-Fi versions and ESP-NOW channels coordinated.
13. Preserve saved Wi-Fi and BLE bond NVS during ordinary firmware uploads.
14. Do not reintroduce a complex fallback state machine without a hardware test
   for power loss, wrong passwords, one-unit failure, and rollback behavior.
15. Keep changes small and observable. Add clear logs at discovery,
   authentication, protocol completion, commit, and recovery boundaries.

If true concurrent BLE connections become a requirement, first replace the BLE
stack with one that explicitly supports multiple clients, then prove the design
with pen notifications and repeated Guide Me RACP sessions on real hardware.
Do not attempt concurrency again by adding more tasks around the current global
client implementation.

## Useful Commands

Build the Outer Unit:

```powershell
& 'C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe' `
  run -d firmware\outer-unit -e esp32-s3-devkitc-1
```

Upload the Outer Unit on `COM3`:

```powershell
& 'C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe' `
  run -d firmware\outer-unit -e esp32-s3-devkitc-1 `
  -t upload --upload-port COM3
```

Monitor the Outer Unit:

```powershell
& 'C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe' `
  device monitor --port COM3 --baud 115200
```

Monitor the Inner Unit, replacing the port if Windows assigns another one:

```powershell
& 'C:\Users\nsaga\.platformio\penv\Scripts\platformio.exe' `
  device monitor --port COM9 --baud 115200
```

Only one process can own a COM port. Stop an existing monitor with `Ctrl+C`
before uploading firmware.

## Commit and Branch Record

- `dd8bb9a` - reliable coordinated Wi-Fi provisioning commit
- `d51b94d` - serialized BLE sessions and pen replay protection
- Both commits were pushed to `origin/ananthu-dev` and `origin/develop`.
- The working branch was returned to `ananthu-dev` after synchronization.

The older `firmware-parallel-ble-glucometer-summary.md` is historical and must
not be used as the current BLE architecture reference.
