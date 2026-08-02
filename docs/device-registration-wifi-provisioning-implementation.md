# Dia-Smart Device Registration and Automatic Wi-Fi Provisioning

Implementation Summary

Branch: `sanjeevan-dev`

Implementation date: 2026-08-02

## Purpose

This backend work completes secure four-device kit registration and activation, automatic Wi-Fi provisioning, MQTT command publishing, Outer/Inner Unit coordination, secure ACK/result processing, and a frontend polling status model.

The implemented flow lets an admin register a real purchased kit, lets an authorized patient activate the four printed UIDs, stores Wi-Fi credentials encrypted, publishes Wi-Fi commands over MQTT without persisting plaintext command payloads, correlates firmware ACK/result events, and exposes a safe status response for the frontend wizard.

## Architecture

```text
Frontend
    -> REST
Spring Boot Backend
    -> MQTT
AWS IoT Core
    ->
Outer Unit
    -> ESP-NOW
Inner Unit
```

`http://192.168.4.1/api/provision` is called by the frontend or mobile device while connected to the Outer Unit SoftAP. The backend does not call that local IP address.

## Part 1 Implementation

Part 1 secured device access and added the real kit model.

- Device registry APIs are admin-protected where they expose inventory-level data.
- Patient device listing is patient-scoped through backend relationships.
- `device_kits` stores the purchased kit.
- `device_kit_devices` maps exactly one device per kit role.
- Important files include `DeviceKit`, `DeviceKitDevice`, `DeviceKitRepository`, `DeviceKitDeviceRepository`, admin registration DTOs/services, and `database/manual_rds/part1_device_kit_model.sql`.

## Part 2 Implementation

Part 2 completed secure activation.

- The patient activates a kit using all four printed device UIDs.
- Activation validates authorization, expected device types, active status, same-kit membership, and idempotent assignment.
- Activation runs transactionally and writes safe audit/rate-limit metadata to `device_activation_attempts`.
- Brute-force and repeated invalid activation attempts are rate-limited by user and IP.
- Important SQL: `database/manual_rds/part2_device_activation_security.sql`.

## Part 3 Implementation

Part 3 secured Wi-Fi command storage and publishing.

- Wi-Fi password storage uses AES-GCM structured fields: ciphertext, nonce, and tag.
- `device_commands.payload` stores safe metadata only, not plaintext passwords or encrypted password parts.
- Plaintext Wi-Fi passwords are decrypted only at publish time and cleared after use.
- MQTT publishing is scheduled after transaction commit.
- Retry/recovery handles failed or stale publish attempts without conflicting with command history.
- Manual resend republishes the current configuration version.
- Important files include `DeviceConfigurationServiceImpl`, `WifiConfigurationCommandPublisher`, `WifiCommandStateService`, `WifiCommandRecoveryScheduler`, `EncryptionService`, and `database/manual_rds/part3_secure_wifi_command_publishing.sql`.

## Part 4 Implementation

Part 4 completed ACK/result correlation and status polling.

- `CommandAckProcessingService` correlates ACKs by public command ID, command type, topic Outer UID, payload Outer UID, configuration reference, configuration version, and transition order.
- `CommandAckStatus` centralizes firmware status mapping and prevents backwards terminal transitions.
- ACK deduplication uses firmware ACK ID when supplied or a stable command/status/version/topic/timestamp key.
- Rejected ACKs are stored with safe processing results and do not update command or configuration state.
- `DeviceTelemetryProcessingService` correlates `INNER_WIFI_CONFIGURATION_RESULT` by telemetry event ID, topic Outer UID, stored command, configuration version, expected Inner Unit UID, patient, and same-kit membership.
- Inner result statuses are controlled through `InnerWifiResultStatus`.
- Failed, rolled-back, and recovery-channel outcomes preserve previous successful configuration version fields.
- Published commands get a provisioning timeout deadline separate from MQTT publish retry state.
- `DeviceProvisioningLifecycleService` centralizes overall status calculation for the existing status endpoint.

## REST API Contract

```http
POST /api/v1/admin/devices/register-kit
GET  /api/v1/patients/{patientId}/devices
POST /api/v1/patients/{patientId}/devices/activate-kit
POST /api/v1/patient/device-configurations
PUT  /api/v1/patient/device-configurations/{outerDeviceId}
POST /api/v1/patient/device-configurations/{outerDeviceId}/send
GET  /api/v1/patient/device-configurations/{outerDeviceId}/status
```

The configuration endpoints use numeric REST `device_id` values. MQTT uses device UID strings.

## MQTT Contract

Backend publishes Wi-Fi commands:

```text
diasmart/devices/{outerUid}/commands
```

Sanitized example shape:

```json
{
  "commandId": "CMD-25",
  "commandType": "WIFI_CONFIGURATION",
  "outerDeviceId": "OUTER-001",
  "createdAt": "2026-08-02T12:30:00Z",
  "payload": {
    "wifiSsid": "Home-WiFi",
    "wifiPassword": "<redacted>",
    "innerDeviceId": "INNER-001",
    "innerDeviceNumericId": 2,
    "configurationVersion": 3
  }
}
```

Firmware publishes command ACKs:

```text
diasmart/devices/{outerUid}/command-ack
```

Legacy ACK topic is still supported:

```text
diasmart/v1/devices/{outerUid}/command-ack
```

ACK example:

```json
{
  "commandId": "CMD-25",
  "acknowledgementId": "ACK-1001",
  "commandType": "WIFI_CONFIGURATION",
  "outerDeviceId": "OUTER-001",
  "configurationVersion": 3,
  "status": "VALIDATED",
  "message": "Configuration validated",
  "timestamp": "2026-08-02T12:30:04Z"
}
```

Firmware publishes telemetry and Inner Wi-Fi results:

```text
diasmart/devices/{outerUid}/telemetry
```

Inner result example:

```json
{
  "eventId": "INNER-WIFI-1001",
  "commandId": "CMD-25",
  "eventType": "INNER_WIFI_CONFIGURATION_RESULT",
  "outerDeviceId": "OUTER-001",
  "innerDeviceId": "INNER-001",
  "configurationVersion": 3,
  "status": "CONNECTED",
  "ipAddress": "192.168.1.22",
  "message": "Inner Unit connected",
  "timestamp": "2026-08-02T12:32:00Z"
}
```

## Provisioning Lifecycle

Command statuses:

`PENDING`, `SENT`, `PUBLISHED`, `RECEIVED`, `VALIDATED`, `STAGED`, `APPLYING`, `APPLIED`, `FAILED`, `ROLLED_BACK`, `TIMED_OUT`, `EXPIRED`

Outer Unit statuses:

`PENDING`, `PUBLISHED`, `RECEIVED`, `VALIDATED`, `STAGED`, `APPLYING`, `APPLIED`, `FAILED`, `REJECTED`, `ROLLED_BACK`

Inner Unit statuses:

`NOT_CONFIGURED`, `WAITING_FOR_CONFIGURATION`, `STAGED`, `CONNECTING`, `CONNECTED`, `FAILED`, `ROLLED_BACK`, `RECOVERY_CHANNEL`

MQTT statuses:

`PENDING`, `PUBLISHED`, `RECONNECTING`, `CONNECTED`, `FAILED`, `PUBLISH_FAILED`, `TIMED_OUT`

Rollback statuses:

`NOT_REQUIRED`, `ROLLBACK_STARTED`, `ROLLED_BACK`, `RECOVERY_CHANNEL_ACTIVE`

Overall statuses:

`SAVED`, `PENDING_PUBLICATION`, `PUBLISHED`, `OUTER_RECEIVED`, `VALIDATING`, `STAGING_INNER`, `APPLYING`, `RECONNECTING`, `SUCCEEDED`, `FAILED`, `ROLLED_BACK`, `TIMED_OUT`, `SUPERSEDED`, `STALE`

Terminal statuses:

`SUCCEEDED`, `FAILED`, `ROLLED_BACK`, `TIMED_OUT`, `SUPERSEDED`, `STALE`

## Status API Fields

The existing status endpoint returns the previous fields plus lifecycle fields:

- `configurationStatus`, `outerUnitStatus`, `innerUnitStatus`, `innerUnitIpAddress`, `configurationVersion`
- `overallStatus`, `commandStatus`, `mqttStatus`, `rollbackStatus`, `terminal`
- `commandId`, `commandNumericId`
- `lastSuccessfulConfigurationVersion`, `previousConfigurationVersion`
- `lastErrorCode`, `lastErrorMessage`
- `publishedAt`, `lastAcknowledgedAt`, `lastInnerUnitStatusAt`, `timeoutAt`, `completedAt`
- `lastAckProcessingResult`, `lastResultProcessingResult`, `staleResultIgnored`

The response does not include passwords, encrypted password fields, command payloads, certificates, JWTs, or buyer personal information.

## Database Changes

Part 1 added:

- `device_kits`
- `device_kit_devices`

Part 2 added:

- `device_kits.patient_id`
- `device_kits.activated_at`
- `device_activation_attempts`

Part 3 added or expanded:

- `device_configurations.wifi_password_ciphertext`
- `device_configurations.wifi_password_nonce`
- `device_configurations.wifi_password_tag`
- `device_commands.device_configuration_id`
- `device_commands.configuration_version`
- `device_commands.last_attempt_at`
- `device_commands.next_retry_at`

Part 4 added or expanded:

- `device_command_acknowledgements.configuration_version`
- `device_command_acknowledgements.reporting_outer_device_uid`
- `device_command_acknowledgements.payload_outer_device_uid`
- `device_command_acknowledgements.ack_uid`
- `device_command_acknowledgements.ack_deduplication_key`
- `device_command_acknowledgements.processing_result`
- `device_command_acknowledgements.device_timestamp`
- `device_configurations.last_successful_configuration_id`
- `device_configurations.last_successful_configuration_version`
- `device_configurations.last_successful_at`
- `device_configurations.previous_configuration_id`
- `device_configurations.previous_configuration_version`
- `device_configurations.provisioning_started_at`
- `device_configurations.provisioning_completed_at`
- `device_configurations.provisioning_timeout_at`
- `device_configurations.provisioning_failure_code`
- `device_configurations.provisioning_failure_message`
- `device_configurations.rollback_status`
- `device_configurations.mqtt_status`
- `device_configurations.last_provisioning_command_id`
- `device_configurations.last_provisioning_command_uid`
- `device_commands.timeout_at`
- `device_commands.completed_at`
- `device_telemetry_events.command_id`
- `device_telemetry_events.command_uid`
- `device_telemetry_events.device_configuration_id`
- `device_telemetry_events.configuration_version`
- `device_telemetry_events.inner_device_id`
- `device_telemetry_events.inner_device_uid`
- `device_telemetry_events.processing_result`

Manual SQL execution order:

```text
database/manual_rds/part1_device_kit_model.sql
database/manual_rds/part2_device_activation_security.sql
database/manual_rds/part3_secure_wifi_command_publishing.sql
database/manual_rds/part4_provisioning_status_and_ack.sql
```

AWS RDS must be modified manually. Hibernate auto-update should not be relied on for production.

Legacy rows are preserved. Old ACK rows receive `LEGACY|acknowledgement_id` deduplication keys. Missing legacy correlation fields remain null and are treated as historical, not trusted to update current provisioning status.

## Security Decisions

- Wi-Fi passwords use AES-GCM structured storage.
- Plaintext Wi-Fi passwords are not stored in command persistence.
- Command ACK processing trusts the MQTT topic Outer UID and validates payload UID only as a consistency check.
- Configuration versions prevent stale ACKs or old Inner results from updating current configuration state.
- QoS 1 duplicate ACKs are suppressed by a database-backed deduplication key.
- Inner result events validate command, Outer Unit, Inner Unit, patient, and same-kit membership.
- Logs and stored ACK/result messages are controlled and truncated.
- Backend authorization remains enforced through patient access checks.

## Firmware Responsibilities

Firmware remains responsible for:

- SoftAP provisioning
- encrypted ESP-NOW staging
- coordinated apply
- router connection
- MQTT reconnect
- channel switching
- rollback
- recovery mode
- publishing ACK and result events with command ID, Outer UID, Inner UID, configuration version, status, and timestamp

## Frontend Responsibilities

Frontend remains responsible for:

- collect four printed UIDs
- activate the kit
- collect Wi-Fi credentials
- save backend configuration
- connect to `DiaSmart-XXXX`
- call `http://192.168.4.1/api/provision`
- return to cloud connectivity
- poll backend status
- clear the password from memory after use

## Manual Verification

1. Apply the four SQL scripts in order.
2. Restart the backend after migration if it was running during schema changes.
3. Register a kit as admin.
4. Activate the kit for a patient using all four UIDs.
5. Save Wi-Fi credentials through `POST /api/v1/patient/device-configurations`.
6. Confirm `device_commands.payload` has no `wifiPassword`.
7. Publish a valid ACK on `diasmart/devices/{outerUid}/command-ack`.
8. Publish the same ACK again and confirm no duplicate side effects.
9. Publish a wrong-Outer ACK and confirm command/config state does not change.
10. Publish a stale version ACK and confirm it is rejected.
11. Publish a valid `INNER_WIFI_CONFIGURATION_RESULT`.
12. Publish a wrong-Inner result and confirm it is rejected.
13. Poll `GET /api/v1/patient/device-configurations/{outerDeviceId}/status`.
14. Let a published command pass timeout and confirm `TIMED_OUT`.
15. Run `mvn test` in `backend/spring-api`.

## Known Limitations

- The backend records firmware rollback and recovery outcomes; it does not perform firmware rollback itself.
- Configuration history is represented by current-row version markers and command/ACK/result history. A separate historical Wi-Fi configuration table was not introduced.
- Legacy rows without correlation data are preserved for audit but are not trusted for current status mutation.
- Frontend/mobile and firmware changes are still required to send the full command/result contract consistently.

## Git Commits

Part 1:

- `56797c7` Secure device APIs and add device kit model

Part 2:

- `eca9654` Complete secure device kit activation and rate limiting

Part 3:

- `9a6de8f` Prevent plaintext WiFi command persistence
- `f02732c` Build WiFi MQTT payload securely at publish time
- `138bd8a` Make WiFi MQTT publishing transaction safe
- `a803b52` Add WiFi publish retry tests and RDS migration

Part 4:

- `4a5654a` Secure WiFi command acknowledgement processing
- `17f1c26` Correlate Inner WiFi results with provisioning commands
- `d78dd5a` Complete WiFi provisioning status lifecycle
