# Firmware Wi-Fi Provisioning Implementation

## Scope

This implementation adds runtime Wi-Fi provisioning to the current Outer and
Inner firmware without changing the existing Inner sensor packet, BLE pen,
glucometer, Care Plan, display, telemetry, or offline queue contracts.

Both updated firmware images must be flashed together before hardware testing.

## First-Time Setup

When the Outer has no saved Wi-Fi configuration, or its saved Wi-Fi cannot
connect, it starts:

```text
SSID: DiaSmart-DS-OUTER-0001
Address: 192.168.4.1
```

The development setup password is:

```text
DiaSmartSetup0001
```

This password is for prototype testing only. Production builds must inject a
unique printed per-device setup password and define:

```text
DIASMART_SETUP_PASSWORD_PROVISIONED
```

The setup app uses:

```http
POST /api/provision
Content-Type: application/json

{
  "ssid": "Patient-WiFi",
  "password": "patient-password"
}
```

and polls:

```http
GET /api/provision/status
```

The setup AP stays available for 30 seconds after success so the app can read
the final result.

## Remote Update

Outer subscribes to:

```text
diasmart/devices/{outerDeviceUid}/commands
```

`WIFI_CONFIGURATION` commands are copied into a bounded FreeRTOS queue and
parsed outside the MQTT callback. Outer publishes sanitized command status to:

```text
diasmart/devices/{outerDeviceUid}/command-ack
```

The password is never printed or included in status messages.

## Outer-to-Inner Transfer

The existing `InnerPacket` payload remains unchanged. Inner broadcasts it only
until first pairing discovery. After pairing, normal sensor packets use the
same encrypted unicast peer as provisioning traffic.

Outer learns the Inner MAC from a valid sensor packet. The units exchange
non-secret pairing frames, persist the peer MAC, and then replace the temporary
peer with encrypted unicast ESP-NOW using a PMK and LMK.

Only the encrypted `WIFI_CONFIG_STAGE` frame contains credentials. Every
configuration frame includes:

- Protocol magic and version
- Packet type
- Transaction nonce
- Configuration version
- Command hash
- Payload length
- Payload checksum

The maximum frame size is checked at compile time against the ESP-NOW payload
limit.

## Coordinated Apply

1. Outer stores the new credentials as pending.
2. Outer sends encrypted staging data to Inner.
3. Inner validates and stores the same pending data.
4. Inner returns the staging acknowledgement.
5. Outer sends a relative-delay apply command.
6. Both try the new 2.4 GHz access point.
7. Inner records the router channel, disconnects from normal Wi-Fi, and
   reinitializes ESP-NOW on that channel.
8. Outer waits for the Inner Unit's `CONNECTED` result.
9. Only after both units connect, Outer promotes the pending credentials and
   reports `APPLIED`.
10. If the Inner Unit is unavailable, Outer keeps the pending credentials and
    retries discovery and encrypted staging without requiring the app to resend
    them.

Current credentials are promoted only after both units confirm a successful
connection. The prior working configuration remains available for rollback.

At boot, both units try a real saved app configuration first. If it cannot
connect, they try the compile-time development fallback without copying that
fallback into NVS. A legacy `DEV-FALLBACK` version-zero NVS record is treated
as the development fallback rather than a patient configuration. If neither
network works, both use the configured ESP-NOW recovery channel. The Outer
keeps its setup access point available whenever it is running on the fallback.

The Inner sends its final Wi-Fi result three times over encrypted ESP-NOW so a
single lost packet cannot leave the Outer waiting after a successful switch.
The Inner provisioning service is the only owner of Wi-Fi connection attempts,
fallback selection, and ESP-NOW channel changes; `main.cpp` only asks it to
prepare the radio before starting the sensor task.

## Development and Production Keys

The repository contains clearly marked development-only ESP-NOW and setup AP
defaults so the existing prototype remains buildable.

Production builds must inject unique per-kit values for:

```text
DIASMART_ESPNOW_PMK_BYTES
DIASMART_ESPNOW_LMK_BYTES
DIASMART_SETUP_AP_PASSWORD
```

and define:

```text
DIASMART_ESPNOW_KEYS_PROVISIONED
DIASMART_SETUP_PASSWORD_PROVISIONED
DIASMART_PRODUCTION_BUILD
```

The build intentionally fails if production mode is enabled without provisioned
keys.

ESP32 flash encryption and NVS encryption must also be enabled during production
manufacturing. This is an eFuse/partition provisioning operation and must not be
enabled automatically by application firmware.

## Unchanged Firmware

No Wi-Fi changes are needed in:

- Pen Unit firmware: it continues communicating with Outer through BLE.
- Glucometer: it continues communicating with Outer through BLE and has no
  Dia-Smart firmware in this repository.

## Required External Verification

Before merging to `develop`:

1. Flash the updated Outer and Inner images together.
2. Verify first-time provisioning using the web/mobile wizard.
3. Test wrong credentials and rollback.
4. Test access points on channels 1, 6, and 11.
5. Verify Inner sensor telemetry resumes after switching.
6. Verify BLE pen, glucometer, Care Plan, display, and offline queue behavior.
7. Confirm the AWS IoT certificate policy permits subscribe/receive on the
   device command topic.
8. Replace development keys/passwords with unique per-kit manufacturing data.
