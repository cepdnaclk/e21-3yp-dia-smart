# Dia-Smart Backend Device Configuration and Care Plan Workflows

Date: 2026-07-17

This document describes the backend implementation for:

1. Outer Unit and Inner Unit Wi-Fi configuration.
2. Prescription schedule windows, versioned Care Plans, reminder events, dose telemetry, and device sync.

The backend uses numeric database `device_id` values in REST APIs and device UID strings in MQTT topics/payloads. Example: REST may receive `outerDeviceId: 1`, while MQTT uses `OUTER-001`.

## Existing Reusable Components

- Device registry: `devices` table and `DeviceRepository`.
- Patient ownership and RBAC: `PatientAccessService`, `AuthorizationService`, current JWT security.
- AES-GCM encryption base: `EncryptionService`.
- MQTT client: single managed `MqttService`.
- Existing telemetry pipeline: `TelemetryProcessingService` for older combined sensor telemetry.
- Existing prescription and dose schedule modules.
- Existing dose event persistence: `dose_events`.

## Main Backend Additions

- Structured AES-256-GCM Wi-Fi password storage with ciphertext, nonce, and tag columns.
- Wi-Fi command lifecycle with `CMD-{id}` public command IDs.
- MQTT publish retry for Wi-Fi and Care Plan publish.
- Device configuration resend endpoint for reconnect or manual retry.
- Inner Unit final Wi-Fi result persistence.
- Versioned Care Plan snapshots and normalized Care Plan schedule rows.
- Care Plan MQTT delivery status and ACK processing.
- Explicit dose schedule window fields: `windowStart`, `targetTime`, `windowEnd`.
- Device telemetry event ledger with application-level ACK.
- Dose telemetry matching by supplied schedule ID or unambiguous fallback matching.
- Reminder, missed-dose, double-dose, and manual-stop event persistence.
- Device sync request handling to resend current Wi-Fi config and Care Plan.

## REST API

### Device Configuration Base

```text
/api/v1/patient/device-configurations
```

All endpoints require a patient JWT. The authenticated patient must have access to the patient assigned to the Outer Unit.

### Create First Wi-Fi Configuration

```http
POST /api/v1/patient/device-configurations
Content-Type: application/json
```

```json
{
  "outerDeviceId": 1,
  "innerDeviceId": 2,
  "penDeviceId": 3,
  "glucometerDeviceId": 4,
  "wifiSsid": "Home-WiFi",
  "wifiPassword": "SecurePassword123"
}
```

Rules:

- `outerDeviceId` must exist, be active, be `OUTER_GATEWAY`, and be assigned to a patient visible to the authenticated user.
- Optional mapped devices must be active and must match expected types:
  - `innerDeviceId`: `INNER_UNIT`
  - `penDeviceId`: `DOSE_CAP`
  - `glucometerDeviceId`: `GLUCOMETER`
- A second configuration for the same Outer Unit returns `CONFIG_ALREADY_EXISTS`.
- API responses never include `wifiPassword`.

### List Current Patient Configurations

```http
GET /api/v1/patient/device-configurations
```

### Get Configuration

```http
GET /api/v1/patient/device-configurations/{outerDeviceId}
```

### Update Configuration

```http
PUT /api/v1/patient/device-configurations/{outerDeviceId}
Content-Type: application/json
```

```json
{
  "wifiSsid": "New Home WiFi",
  "wifiPassword": "NewSecurePassword123",
  "innerDeviceId": 2
}
```

Only supplied fields are changed. When any publish-relevant field changes, the backend increments `configurationVersion` and republishes the Wi-Fi command.

### Re-send Configuration

```http
POST /api/v1/patient/device-configurations/{outerDeviceId}/send
```

This republishes the current encrypted Wi-Fi configuration after decrypting the password only for MQTT payload construction.

### Get Configuration Status

```http
GET /api/v1/patient/device-configurations/{outerDeviceId}/status
```

Response includes:

- `configurationStatus`
- `outerUnitStatus`
- `innerUnitStatus`
- `innerUnitIpAddress`
- `lastInnerUnitStatusAt`
- `configurationVersion`

### Care Plan Endpoints

```http
POST /api/v1/patients/{patientId}/care-plans/generate-send
GET  /api/v1/patients/{patientId}/care-plans/current
POST /api/v1/patients/{patientId}/care-plans/current/send
```

These endpoints use existing patient access permissions. Doctors/caregivers/admins must have edit/read permission through `user_patient_access`.

### Dose Schedule Window JSON

The dose schedule API now supports explicit windows:

```json
{
  "prescriptionId": 5,
  "scheduleLabel": "Morning dose",
  "scheduledTime": "08:00",
  "windowStart": "07:30",
  "targetTime": "08:00",
  "windowEnd": "08:30",
  "doseUnits": 10,
  "daysOfWeek": "1,2,3,4,5,6,7"
}
```

Backward compatibility:

- If `targetTime` is omitted, `scheduledTime` is used.
- If `windowStart` or `windowEnd` is omitted, the backend derives them from `allowedEarlyMinutes` and `allowedLateMinutes`.

## MQTT Topics

### Wi-Fi Command

```text
diasmart/devices/{outerDeviceUid}/commands
```

QoS: 1

Retained: false

Payload:

```json
{
  "commandId": "CMD-25",
  "commandType": "WIFI_CONFIGURATION",
  "createdAt": "2026-07-17T06:30:00Z",
  "outerDeviceId": "OUTER-001",
  "payload": {
    "wifiSsid": "Home-WiFi",
    "wifiPassword": "SecurePassword123",
    "innerDeviceId": "INNER-001",
    "innerDeviceNumericId": 2,
    "configurationVersion": 1
  }
}
```

Security:

- The plaintext Wi-Fi password is never stored.
- It exists only while building the MQTT payload.
- MQTT service logs only topic names, not payload contents.

### Wi-Fi Command ACK

```text
diasmart/devices/{outerDeviceUid}/command-ack
```

Legacy topic also remains subscribed:

```text
diasmart/v1/devices/+/command-ack
```

Payload:

```json
{
  "commandId": "CMD-25",
  "commandType": "WIFI_CONFIGURATION",
  "status": "APPLIED",
  "outerDeviceId": "OUTER-001",
  "message": "Outer Unit connected successfully",
  "configurationVersion": 1,
  "timestamp": "2026-07-17T06:31:00Z"
}
```

Supported statuses:

```text
PENDING
PUBLISHED
RECEIVED
VALIDATED
APPLIED
FAILED
REJECTED
```

### Inner Unit Wi-Fi Final Result

Topic:

```text
diasmart/devices/{outerDeviceUid}/telemetry
```

Payload:

```json
{
  "eventId": "INNER-WIFI-1001",
  "commandId": "CMD-25",
  "eventType": "INNER_WIFI_CONFIGURATION_RESULT",
  "outerDeviceId": "OUTER-001",
  "innerDeviceId": "INNER-001",
  "status": "CONNECTED",
  "ipAddress": "192.168.1.22",
  "timestamp": "2026-07-17T10:32:00+05:30"
}
```

The backend updates the latest `innerUnitStatus`, IP address, message, and status timestamp on the device configuration.

### Care Plan Publish

```text
diasmart/devices/{outerDeviceUid}/care-plan
```

QoS: 1

Retained: true

Payload:

```json
{
  "carePlanId": "CP-10-5",
  "version": 5,
  "patientId": "PATIENT-10",
  "outerDeviceId": "OUTER-001",
  "timezone": "Asia/Colombo",
  "effectiveFrom": "2026-07-17",
  "schedules": [
    {
      "scheduleId": "SCH-100",
      "period": "MORNING",
      "insulinType": "Rapid Acting",
      "doseUnits": 10,
      "windowStart": "07:30",
      "targetTime": "08:00",
      "windowEnd": "08:30"
    }
  ],
  "reminderSettings": {
    "buzzerDurationMinutes": 3,
    "repeatIntervalMinutes": 15,
    "manualStopAllowed": true
  }
}
```

### Care Plan ACK

Topic:

```text
diasmart/devices/{outerDeviceUid}/command-ack
```

Payload:

```json
{
  "carePlanId": "CP-10-5",
  "version": 5,
  "status": "APPLIED",
  "outerDeviceId": "OUTER-001",
  "timestamp": "2026-07-17T10:30:00Z"
}
```

The backend updates `care_plan_snapshots` and `care_plan_delivery_status`.

### Dose Telemetry

Topic:

```text
diasmart/devices/{outerDeviceUid}/telemetry
```

Payload:

```json
{
  "eventId": "DOSE-5501",
  "eventType": "DOSE_RECORDED",
  "outerDeviceId": "OUTER-001",
  "penDeviceId": "PEN-001",
  "scheduleId": "SCH-100",
  "carePlanVersion": 5,
  "doseUnits": 10,
  "takenAt": "2026-07-18T08:15:00+05:30",
  "status": "TAKEN_WITHIN_WINDOW"
}
```

Processing:

- Duplicate `eventId` receives `DUPLICATE` ACK.
- Valid event is stored in `device_telemetry_events`.
- Dose is stored in `dose_events`.
- Supplied `scheduleId` is validated against the Care Plan snapshot.
- If missing or invalid, the backend tries one unambiguous match by patient device ownership, Care Plan version, event timestamp, schedule window, and dose units.
- The backend does not trust a patient ID inside the telemetry payload.

### Reminder Events

Accepted event types:

```text
REMINDER_STARTED
REMINDER_REPEATED
REMINDER_MANUALLY_STOPPED
DOSE_MISSED
POSSIBLE_DOUBLE_DOSE
```

Example:

```json
{
  "eventId": "REM-1002",
  "eventType": "REMINDER_REPEATED",
  "outerDeviceId": "OUTER-001",
  "scheduleId": "SCH-100",
  "carePlanVersion": 5,
  "repeatNumber": 2,
  "timestamp": "2026-07-18T08:18:00+05:30"
}
```

Manual reminder stop is stored as an event. It is not treated as dose confirmation.

### Telemetry ACK

Topic:

```text
diasmart/devices/{outerDeviceUid}/telemetry-ack
```

Payload:

```json
{
  "eventId": "DOSE-5501",
  "status": "ACCEPTED",
  "timestamp": "2026-07-18T02:46:00Z"
}
```

Statuses:

```text
ACCEPTED
DUPLICATE
REJECTED
```

The backend publishes this ACK only after the application-level validation and persistence decision.

### Device Sync Request

Payload:

```json
{
  "eventId": "SYNC-1001",
  "eventType": "DEVICE_SYNC_REQUEST",
  "outerDeviceId": "OUTER-001",
  "timestamp": "2026-07-18T08:00:00+05:30"
}
```

The backend records the sync request and attempts to resend:

- Current Wi-Fi configuration command, if one exists.
- Current Care Plan snapshot, if one exists.

## Database Changes

Updated tables:

- `device_configurations`
  - Added AES-GCM parts: `wifi_password_ciphertext`, `wifi_password_nonce`, `wifi_password_tag`.
  - Added outer and inner unit status fields.
  - Added `PUBLISHED` status.
- `device_commands`
  - Added `command_uid`, `retry_count`, `last_error`.
  - Added `WIFI_CONFIGURATION` command type.
  - Added `PUBLISHED` status.
- `device_command_acknowledgements`
  - Added `command_uid`.
- `dose_schedules`
  - Added `window_start`, `target_time`, `window_end`.
- `dose_events`
  - Added `dose_status`.

New tables:

- `care_plan_snapshots`
- `care_plan_schedules`
- `care_plan_delivery_status`
- `device_telemetry_events`
- `reminder_events`
- `device_sync_requests`

SQL file updated:

```text
database/diasmart_rds_final_schema.sql
```

Also fixed duplicate creation of `ux_user_patient_access_single_self` by using `CREATE UNIQUE INDEX IF NOT EXISTS`.

## Implementation Files

Device configuration:

- `DeviceConfigurationController`
- `DeviceConfigurationService`
- `DeviceConfigurationServiceImpl`
- `DeviceConfiguration`
- `DeviceCommand`
- `DeviceCommandAcknowledgement`
- device configuration DTOs and mapper

Security and MQTT:

- `EncryptionService`
- `MqttService`
- `MqttSubscriber`
- `CommandAckDTO`
- `CommandAckProcessingService`

Care Plan:

- `CarePlanController`
- `CarePlanService`
- `CarePlanPublisherService`
- `CarePlanAckService`
- `DoseScheduleMatchingService`
- Care Plan entities and repositories
- `CarePlanResponse`

Telemetry and sync:

- `DeviceTelemetryProcessingService`
- `DeviceSyncService`
- `DeviceTelemetryEvent`
- `ReminderEvent`
- `DeviceSyncRequest`
- device event repositories

Prescription/schedule integration:

- `PrescriptionService`
- `DoseScheduleService`
- `DoseSchedule`
- dose schedule DTOs
- `DoseEvent`

Tests:

- `DeviceConfigurationServiceImplTest`
- `CarePlanServiceTest`
- `DeviceTelemetryProcessingServiceTest`

## Verification

Command:

```text
./mvnw.cmd test
```

Result:

```text
Tests run: 113, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

## Notes For Firmware Team

- The backend never sends MQTT directly to the Inner Unit.
- Outer Unit receives Wi-Fi credentials and passes them to Inner Unit through ESP-NOW.
- Outer Unit should delete offline queued telemetry only after receiving `telemetry-ack`.
- Care Plan reminder looping remains firmware responsibility.
- Backend stores reminder and dose outcomes and can resend the latest retained Care Plan after reconnect.
