# Inner and Outer Unit Wi-Fi Provisioning Implementation Plan

## Purpose

This document is an implementation plan for connecting an assigned Dia-Smart
Outer Unit and Inner Unit to patient Wi-Fi without breaking existing firmware
behavior.

The selected design preserves the current communication model:

- Outer Unit is the internet and MQTT gateway.
- Inner Unit connects to the assigned 2.4 GHz access point briefly to learn the
  real Wi-Fi channel.
- Inner Unit then disconnects from normal Wi-Fi, keeps the radio on that
  channel, and sends sensor data to Outer through ESP-NOW.
- Outer receives backend Wi-Fi commands and securely coordinates the same
  configuration with Inner.

No implementation was performed when this plan was written.

## Source Documents

Backend workflow:

- `docs/backend-device-config-care-plan-workflows.md`
- User-provided backend implementation document dated 2026-07-17.

Official Espressif references:

- Wi-Fi provisioning:
  <https://docs.espressif.com/projects/esp-idf/en/v5.1.5/esp32/api-reference/provisioning/wifi_provisioning.html>
- Unified provisioning security:
  <https://docs.espressif.com/projects/esp-idf/en/v5.2/esp32/api-reference/provisioning/provisioning.html>
- Wi-Fi and ESP-NOW channel requirement:
  <https://docs.espressif.com/projects/esp-faq/en/latest/application-solution/esp-now.html>

## Current Working Behavior

Outer Unit currently:

1. Reads hardcoded `WIFI_SSID` and `WIFI_PASSWORD`.
2. Connects in station mode.
3. Uses the access point's channel for Wi-Fi and ESP-NOW.
4. Receives Inner Unit broadcasts.
5. Connects to AWS IoT and publishes telemetry.
6. Subscribes to the Care Plan topic.

Inner Unit currently:

1. Reads the same hardcoded Wi-Fi credentials.
2. Connects to the same access point.
3. Reads the access point channel.
4. Disconnects while retaining station mode.
5. Locks ESP-NOW to that channel.
6. Sends sensor packets using an ESP-NOW broadcast peer whose channel is `0`,
   meaning it follows the current radio channel.

This works because both units discover the same 2.4 GHz access point channel.
Hardcoding itself is not required; synchronized credentials and channel
selection are what matter.

## Current Gaps

The backend control plane exists, but the firmware path is incomplete:

- Credentials are compile-time constants.
- Outer does not subscribe to
  `diasmart/devices/{outerDeviceUid}/commands`.
- Outer does not parse `WIFI_CONFIGURATION`.
- Outer does not persist current, pending, and previous configurations.
- Outer does not publish the Wi-Fi command ACK lifecycle.
- Outer does not transfer Wi-Fi configuration to Inner.
- Inner does not accept a runtime Wi-Fi configuration.
- Current ESP-NOW sensor traffic is broadcast and unencrypted.
- There is no safe first-time setup path.
- There is no coordinated channel-change or rollback state machine.
- There is no recovery when the old Wi-Fi is already unavailable.

## Non-Negotiable Existing Behaviors

The implementation must preserve:

- Inner sensor sampling and ESP-NOW telemetry packet format.
- Outer ESP-NOW receive and duplicate filtering.
- Parallel BLE connections for insulin pen and glucometer.
- Live glucometer synchronization and source measurement timestamps.
- Pen dose buffering, acknowledgement, confirmation, edit, and cancel.
- Care Plan MQTT receive, persistence, reminders, and prescription UI.
- Existing telemetry JSON and backend compatibility.
- LittleFS offline queue and retry ordering.
- TFT pages, keypad navigation, buzzer, and alerts.
- NTP root event timestamps.
- Storage, inventory, battery, dose, and glucose processing.
- AWS IoT TLS authentication.

Wi-Fi provisioning work must remain isolated from BLE callbacks, sensor
callbacks, and display rendering.

## Product Architecture

### Authority

- Backend is the source of device assignment, configuration version, command
  history, and delivery status.
- Outer is the runtime authority for currently active Wi-Fi credentials.
- Inner accepts Wi-Fi configuration only from its paired Outer.
- A configuration is identified by `commandId` and
  `configurationVersion`.

### First-Time Setup

MQTT cannot perform first-time provisioning because Outer needs Wi-Fi before it
can receive MQTT.

Use local secure provisioning:

1. Patient claims the kit by scanning a QR code containing Outer UID and a
   unique proof-of-possession value.
2. On first boot, Outer enters provisioning mode.
3. Outer starts a temporary SoftAP such as `DiaSmart-3178`.
4. The setup app connects and transfers the selected 2.4 GHz SSID/password
   using Espressif secure provisioning with proof-of-possession.
5. Outer validates and stages the configuration.
6. Outer stages the same configuration on its paired Inner over encrypted
   unicast ESP-NOW.
7. Both devices apply the configuration using the coordinated state machine
   described below.
8. The app creates or updates the backend device configuration using HTTPS.
9. After Outer reaches MQTT, it subscribes to commands and publishes status.

Use SoftAP only during setup and recovery. Stop it before normal BLE and
ESP-NOW operation.

### Remote Update

Remote update is possible only while Outer can still reach MQTT using the
current Wi-Fi:

1. Backend publishes `WIFI_CONFIGURATION` at QoS 1.
2. Outer acknowledges `RECEIVED`.
3. Outer validates and stores the new configuration as pending.
4. Outer acknowledges `VALIDATED`.
5. Outer stages the pending configuration on Inner.
6. Outer and Inner switch in a coordinated manner.
7. Outer reconnects to Wi-Fi and MQTT.
8. Outer acknowledges `APPLIED`.
9. Inner reports its final result to Outer.
10. Outer publishes `INNER_WIFI_CONFIGURATION_RESULT`.

QoS 1 can deliver duplicates. Repeated `commandId` or an already applied
`configurationVersion` must be handled idempotently.

### Recovery Setup

If the old router or password no longer works, backend MQTT cannot deliver a
replacement.

Outer must enter local provisioning mode when:

- The patient holds a documented setup key combination.
- No saved Wi-Fi can connect after a bounded retry period.
- A provisioning-only reset is requested.

Provisioning reset must remove Wi-Fi configuration without deleting device
identity, AWS certificates, Care Plan, or telemetry queue.

## Selected Inner Unit Strategy

To match the backend workflow and preserve current behavior, Inner receives the
same assigned Wi-Fi credentials, connects briefly, records the actual channel,
then disconnects and continues with ESP-NOW.

Do not send credentials through the existing broadcast packet path.

Required security:

- Pair Outer and Inner during manufacturing or kit activation.
- Store the paired MAC address.
- Use encrypted unicast ESP-NOW for configuration packets.
- Configure ESP-NOW PMK and per-peer LMK.
- Never include SSID or password in serial logs, display text, telemetry, or
  error messages.
- Enable flash/NVS encryption for production credentials.

If encrypted pairing is not ready, do not implement credential transfer as a
temporary plaintext feature.

## Configuration Storage

Use separate NVS records:

- `current`: known working configuration.
- `pending`: received but not yet proven.
- `previous`: rollback configuration.
- `appliedVersion`: highest successfully applied backend version.
- `commandId`: last processed command ID.
- `state`: interrupted transaction recovery state.

Credential structure:

```text
ssid
password
configurationVersion
commandId
checksum
valid flag
```

Rules:

- SSID maximum length: 32 bytes.
- WPA/WPA2 password maximum length: 63 characters.
- Open networks require an explicit empty-password flag.
- Reject missing SSID, oversized fields, unsupported versions, and malformed
  command IDs.
- Write pending data completely before changing transaction state.
- Promote pending to current only after successful connection.
- Keep previous until Outer and Inner results are finalized.
- Never replace a working configuration immediately after parsing MQTT.

During migration, compile-time credentials may be used only when no NVS
configuration exists. Remove that fallback from production builds after field
provisioning is proven.

## Outer Firmware Components

Implement small ownership-focused modules:

### Wi-Fi Credential Store

Responsibilities:

- Load current, pending, and previous configuration.
- Validate field lengths.
- Atomically stage, promote, and roll back.
- Track command ID and version.
- Sanitize logs.

### Wi-Fi Provisioning Service

Responsibilities:

- Start first-time or recovery SoftAP provisioning.
- Receive credentials through secure Espressif provisioning.
- Stop provisioning before normal operation.
- Pass validated configuration to the coordinator.

### MQTT Command Router

Extend MQTT handling without changing Care Plan behavior:

- Subscribe to `diasmart/devices/{outerDeviceUid}/commands`.
- Route Care Plan messages to the existing Care Plan service.
- Route `WIFI_CONFIGURATION` to a FreeRTOS command queue.
- Reject unknown command types safely.
- Keep MQTT callbacks short and non-blocking.
- Never connect Wi-Fi, write NVS, or wait for Inner inside the MQTT callback.

### Wi-Fi Configuration Coordinator

Own the update state machine and all retries:

```text
IDLE
COMMAND_RECEIVED
VALIDATED
INNER_STAGING
INNER_STAGED
APPLY_SCHEDULED
OUTER_CONNECTING
OUTER_CONNECTED
MQTT_CONNECTED
WAITING_INNER_RESULT
APPLIED
FAILED
ROLLING_BACK
ROLLED_BACK
```

### Backend ACK Publisher

Publish:

- `RECEIVED` after bounded JSON parsing and command identity validation.
- `VALIDATED` after field, version, assignment, and NVS staging checks.
- `APPLIED` only after Outer reconnects to MQTT using the new configuration.
- `FAILED` with a non-secret reason code.
- `REJECTED` for malformed, stale, unauthorized, or incompatible commands.

ACKs must include the original command ID and configuration version.

## Inner Configuration Protocol

Create a separate versioned ESP-NOW protocol. Do not modify the existing
`InnerPacket` sensor telemetry layout.

Packet types:

```text
WIFI_CONFIG_STAGE
WIFI_CONFIG_STAGE_ACK
WIFI_CONFIG_APPLY
WIFI_CONFIG_RESULT
WIFI_CONFIG_ROLLBACK
WIFI_CONFIG_STATUS_REQUEST
WIFI_CONFIG_STATUS_RESPONSE
```

Every packet should contain:

```text
protocol magic
protocol version
packet type
transaction nonce
configuration version
command identifier or compact hash
payload length
payload checksum
```

Only `WIFI_CONFIG_STAGE` contains credentials. It must be encrypted unicast.

The complete packet must stay below the supported ESP-NOW payload limit.
Use fixed-size structures, zero-initialize them, validate the exact received
length, and reject unknown versions.

## Coordinated Apply Sequence

The main channel-change risk is that changing Wi-Fi can move Outer and Inner to
a different radio channel.

Use this sequence:

1. Outer receives and validates the new configuration while still connected to
   old Wi-Fi.
2. Outer sends `WIFI_CONFIG_STAGE` to Inner on the current ESP-NOW channel.
3. Inner validates and persists pending credentials.
4. Inner returns `WIFI_CONFIG_STAGE_ACK`.
5. Outer sends `WIFI_CONFIG_APPLY` with a short relative delay and nonce.
6. Both stop normal ESP-NOW traffic for the bounded switch window.
7. Inner and Outer attempt the new access point.
8. Each learns the new channel from the access point.
9. Each reinitializes ESP-NOW and its peer on that channel.
10. Inner sends `WIFI_CONFIG_RESULT`.
11. Outer reconnects MQTT and publishes final backend status.
12. Normal sensor telemetry resumes.

Do not use wall-clock synchronization for the apply moment. Use receipt of the
apply packet plus a bounded relative delay.

Sensor samples created during the switch should remain in existing queues or be
sampled after the bounded pause. Do not delete telemetry because Wi-Fi is
changing.

## Failure and Rollback Rules

### Inner unavailable before apply

- Outer must not assume Inner received credentials.
- Keep Outer on current Wi-Fi.
- Report Inner staging timeout.
- Permit backend resend or patient retry.

### New credentials invalid

- Outer and Inner retain previous credentials.
- Attempt new configuration for a bounded period.
- Roll back independently to previous configuration.
- Publish `FAILED` after MQTT is restored.

### Outer succeeds but Inner fails

- Outer keeps the valid new Wi-Fi.
- Inner enters ESP-NOW channel discovery mode.
- Inner scans 2.4 GHz channels for its paired Outer.
- After discovery, Outer can resend the pending configuration.
- Backend shows Outer `APPLIED` and Inner `FAILED` or `PENDING`.

### Router channel changes later

Outer follows the access point after reconnect. Inner may remain on an old
channel, so Inner must start channel discovery after repeated ESP-NOW send
failures or missing acknowledgements.

### Power loss

On boot, both units inspect persisted transaction state:

- Pending but not apply-scheduled: remain on current configuration.
- Apply in progress: try pending, then previous.
- Rollback in progress: try previous.
- Applied version: use current.

No power-loss point may erase the last known working configuration.

## Channel Discovery

Static `ESPNOW_CHANNEL=1` remains only a startup/recovery channel.

Add a bounded discovery mode:

1. Inner cycles through valid 2.4 GHz channels.
2. Inner sends a non-secret pairing/status probe.
3. Paired Outer responds on its current channel.
4. Inner validates the paired identity and locks the channel.
5. Inner restores encrypted ESP-NOW communication.

Discovery packets must not contain credentials.

## Backend Integration

Expected topics:

```text
diasmart/devices/{outerDeviceUid}/commands
diasmart/devices/{outerDeviceUid}/command-ack
diasmart/devices/{outerDeviceUid}/telemetry
```

Outer should publish Inner result:

```json
{
  "eventId": "INNER-WIFI-1001",
  "commandId": "CMD-25",
  "eventType": "INNER_WIFI_CONFIGURATION_RESULT",
  "outerDeviceId": "DS-OUTER-0001",
  "innerDeviceId": "DS-INNER-0001",
  "status": "CONNECTED",
  "ipAddress": "192.168.1.22",
  "timestamp": "2026-07-17T10:32:00+05:30"
}
```

If Inner disconnects from the access point after learning the channel, the IP
address is historical and should not be presented as a continuously reachable
Inner address.

The backend should separately show:

- Outer Wi-Fi/MQTT status.
- Inner configuration result.
- Inner ESP-NOW communication status.
- Configuration version.
- Last status timestamp.

## UI Requirements

Do not expose the SSID password.

Outer display should eventually show:

- `SETUP MODE` during first-time/recovery provisioning.
- `CONNECTING WIFI`.
- `OUTER CONNECTED`.
- `CONFIGURING INNER`.
- `INNER CONNECTED`.
- `WIFI UPDATE FAILED - REOPEN SETUP`.

Keep technical command IDs and stack errors on sanitized serial diagnostics,
not the patient display.

Wi-Fi configuration screens must not interrupt an active dose confirmation.
Defer non-emergency UI transitions until the dose prompt is resolved.

## Implementation Phases and Commits

Each phase must build and pass tests before its commit.

### Phase 0: Baseline

- Record Outer and Inner build sizes.
- Verify current hardcoded same-AP ESP-NOW communication.
- Verify pen, glucometer, display, Care Plan, MQTT, and offline queue.
- Commit documentation only if required.

### Phase 1: Credential abstraction

- Add configuration model and validators.
- Add Outer and Inner NVS stores.
- Load NVS first with development hardcoded fallback.
- Do not add MQTT commands yet.
- Test current behavior is unchanged.
- Suggested commit: `Add persistent Wi-Fi configuration storage`.

### Phase 2: Outer MQTT command ingestion

- Add command topic.
- Add bounded parser and command queue.
- Add version/idempotency rules.
- Publish `RECEIVED`, `VALIDATED`, and rejected/failed ACKs.
- Do not switch Wi-Fi yet.
- Suggested commit: `Receive versioned Wi-Fi configuration commands`.

### Phase 3: Secure Outer-to-Inner staging

- Pair MAC addresses.
- Enable encrypted unicast ESP-NOW.
- Add separate configuration packets and acknowledgements.
- Keep existing sensor packets unchanged.
- Suggested commit: `Stage Wi-Fi configuration on paired inner unit`.

### Phase 4: Coordinated apply and rollback

- Implement both state machines.
- Reinitialize ESP-NOW after channel changes.
- Add rollback and boot recovery.
- Publish Inner result telemetry.
- Suggested commit: `Apply Wi-Fi updates across outer and inner units`.

### Phase 5: First-time and recovery provisioning

- Add secure SoftAP provisioning with proof-of-possession.
- Add setup entry and provisioning-only reset.
- Integrate with the same coordinator used by MQTT.
- Suggested commit: `Add secure local Wi-Fi provisioning`.

### Phase 6: Status UI and diagnostics

- Add patient-safe setup and result states.
- Add sanitized diagnostics and counters.
- Preserve dose prompt priority.
- Suggested commit: `Show Wi-Fi provisioning status on outer display`.

### Phase 7: Final verification and documentation

- Run the complete test matrix.
- Update workflow and product setup documentation.
- Remove production credential constants.
- Suggested commit: `Document product Wi-Fi provisioning workflow`.

## Automated Tests

Add host/unit coverage for:

- Maximum and empty credential fields.
- Invalid SSID/password lengths.
- Open-network explicit handling.
- Command JSON bounds.
- Duplicate command ID.
- Stale and future configuration versions.
- NVS stage/promote/rollback.
- Power loss state recovery.
- ESP-NOW packet exact size, checksum, version, and type.
- Wrong sender MAC.
- Stage ACK timeout.
- Apply timeout.
- Outer success plus Inner failure.
- New configuration failure plus previous configuration recovery.
- Secret redaction in generated status messages.

Backend tests should verify:

- Command publish and resend.
- ACK lifecycle.
- Duplicate ACK handling.
- Inner final result persistence.
- Status response for mixed Outer/Inner outcomes.

## Hardware Test Matrix

Test with real Outer, Inner, pen, and glucometer:

1. Existing hardcoded network baseline.
2. First-time setup to a new 2.4 GHz network.
3. Remote update while old Wi-Fi works.
4. New access point on channel 1.
5. New access point on channel 6.
6. New access point on channel 11.
7. Wrong new password.
8. Missing new SSID.
9. Inner powered off during staging.
10. Inner powered off during apply.
11. Outer reboot after staging.
12. Inner reboot after staging.
13. Power loss while both switch.
14. Router unavailable and later restored.
15. Old router removed before update.
16. Local recovery provisioning.
17. Router automatic channel change.
18. Duplicate MQTT command delivery.
19. Backend resend endpoint.
20. Telemetry generated during Wi-Fi switching.
21. Offline queue delivery after reconnect.
22. Care Plan retained and still active.
23. Pen and glucometer reconnect after switching.
24. Live glucose reaches backend.
25. Dose confirmation/edit/cancel still works.
26. Inner storage, inventory, and battery telemetry still arrives.

## Acceptance Criteria

Implementation is complete only when:

- No patient Wi-Fi credentials remain in production source constants.
- First-time setup works without pre-existing internet.
- Remote update works while old Wi-Fi is available.
- Local recovery works when old Wi-Fi is unavailable.
- Credentials are encrypted in transit to Inner and protected at rest.
- Outer and Inner converge on the same access point channel.
- ESP-NOW sensor communication resumes automatically.
- Failed updates retain or recover the last working configuration.
- Backend status accurately distinguishes Outer and Inner outcomes.
- No password appears in logs, display, MQTT diagnostics, or telemetry.
- Existing BLE, glucose, pen, Care Plan, display, sensor, timestamp, MQTT, and
  offline-queue behavior passes regression testing.
- All software tests pass before commits.
- Real hardware tests pass before merging into `develop`.

## Instructions For The Implementing AI Agent

1. Read this document and the backend workflow before editing.
2. Inspect current branch status and preserve unrelated user changes.
3. Do not implement all phases in one commit.
4. Do not change the existing `InnerPacket` layout for Wi-Fi configuration.
5. Do not perform network switching inside MQTT or ESP-NOW callbacks.
6. Do not send credentials over broadcast or unencrypted ESP-NOW.
7. Keep a working rollback configuration at every phase.
8. Build both firmware targets after shared-model changes.
9. Simulate state-machine failures before hardware flashing.
10. Commit only after the current phase tests pass.
11. Flash only after explicit user approval.
12. Merge into `develop` only after explicit user approval and hardware
    validation.
