# Dia-Smart: Intelligent Diabetes Care System

**Department of Computer Engineering, University of Peradeniya**

Dia-Smart is an academic IoT prototype that brings insulin dose records, blood glucose readings, insulin storage conditions, and care schedules into a shared patient record. Three embedded units, a Spring Boot API, and a React dashboard help patients, doctors, and caregivers review day-to-day diabetes care.

This README describes the current non-legacy implementation on `develop`. Monitoring and record-keeping features should not be interpreted as autonomous insulin delivery or clinical validation.

---

## What the System Supports

- **Patient monitoring:** Glucose and dose history, storage temperature, door state, estimated insulin inventory, and device health information.
- **Connected dose recording:** Pen dial rotation and button input produce dose records that are buffered on the pen and transferred to the Outer Unit over BLE.
- **Care coordination:** Patient access relationships, prescriptions, dose schedules, and versioned care plans delivered to the Outer Unit.
- **Alerts and reminders:** Backend evaluation of storage-temperature and inventory thresholds, alert acknowledgement and resolution, and device-side schedule reminders and dose confirmation.
- **Analytics:** Glucose and dose charts, with schedule-based adherence summaries for on-time, late, missed, and unscheduled dose events.
- **Device onboarding:** Administrator kit registration, patient kit activation, local Wi-Fi provisioning, remote Wi-Fi updates, and provisioning status tracking.

| Role          | Application focus                                                                              |
| ------------- | ---------------------------------------------------------------------------------------------- |
| Patient       | Personal readings, prescriptions, schedules, alerts, care-team relationships, and device setup |
| Doctor        | Assigned patients, patient workspaces, prescriptions, schedules, and adherence review          |
| Caregiver     | Assigned patients, readings, alerts, and care monitoring                                       |
| Administrator | Users, device kits, assignments, system information, and audit records                         |

The API uses JWT authentication and patient-access permissions to control access to patient data and supported actions.

---

## System Architecture

```text
Inner Unit ---------------- ESP-NOW ---+
                                      |
Pen Unit ------------------ BLE -------+--> Outer Unit
                                      |    Display, keypad, care plans,
BLE glucometer ------------ BLE -------+    telemetry and offline queue
                                           |
                                           Wi-Fi / MQTT / TLS
                                           |
                                           AWS IoT Core
                                           |
                                           Spring Boot API
                                            /          \
                                     PostgreSQL      REST API
                                                       |
                                                 React dashboard
                                                 Browser / Android
```

The cloud-to-device path carries care plans and Wi-Fi configuration commands through MQTT. The Outer Unit reports command acknowledgements, provisioning outcomes, and reminder or dose events back to the backend.

### Embedded Units

| Component      | Current responsibilities                                                                                                                                                                                                                | Hardware and software                                                                        |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------------------------------------------------------------------------------------------- |
| **Inner Unit** | Samples storage temperature, load-cell weight, door state, and battery voltage; estimates inventory percentage; sends sensor packets to the Outer Unit; participates in coordinated Wi-Fi provisioning                                  | ESP32 DevKit target, DS18B20, HX711/load cell, door input, battery ADC, Arduino and FreeRTOS |
| **Outer Unit** | Receives Inner Unit data, pen doses, and glucose readings; provides a display and keypad; stores care plans; handles reminders and dose confirmation; publishes telemetry and retries queued messages                                   | ESP32-S3 DevKit target, BLE clients, ESP-NOW, Wi-Fi, MQTT/TLS, NVS and LittleFS              |
| **Pen Unit**   | Measures cumulative dial rotation with an AS5600 magnetic angle sensor; uses a debounced button press/release to capture a dose; stores pending records and transfers them over BLE with time synchronisation and slot acknowledgements | ESP32-C3 PlatformIO targets, AS5600 over I2C, button input, BLE GATT and NVS                 |

Inventory is a calibrated weight-based estimate. Pen dose values are derived from the configured rotation-to-unit conversion and button interaction.

### Blood Glucose Data

The Outer Unit integrates with an **Accu-Chek Guide Me** through the BLE Glucose Service. It requests the latest stored measurement using the Record Access Control Point, parses the reading and measurement time, and forwards it through the telemetry pipeline. The pen and glucometer have separate BLE client state: connection setup is serialised, while each glucometer record-request session closes after completion or timeout.

The backend checks glucose sequence numbers per device to suppress duplicate readings. It preserves valid glucometer measurement timestamps and falls back to the enclosing event time when necessary. Manual glucose and dose entry are also available through the API.

### Care Plans and Provisioning

The backend builds versioned care-plan snapshots from active dose schedules and publishes them to the patient's Outer Unit. The firmware stores the plan in NVS for prescription display, reminder windows, and dose-confirmation workflows. Delivery acknowledgements and device sync requests support status tracking and resending plans.

Device kits group the Inner Unit, Outer Unit, pen, and glucometer. Onboarding registers and activates the kit, saves Wi-Fi configuration, provisions the Outer Unit through its local setup access point, and tracks the resulting status. Subsequent updates can arrive through MQTT.

The Outer and Inner units coordinate staging, applying, committing, or rolling back Wi-Fi changes over ESP-NOW. Backend Wi-Fi passwords are stored encrypted.

---

## Technology Stack

| Layer                | Current implementation                                                                                      |
| -------------------- | ----------------------------------------------------------------------------------------------------------- |
| Backend              | Java 21, Spring Boot 3.5, Spring Web, Spring Security, Spring Data JPA, Maven                               |
| Authentication       | JWT access tokens, BCrypt password hashing, patient-access checks                                           |
| Database             | PostgreSQL; schema and provisioning scripts target Amazon RDS PostgreSQL                                    |
| Web application      | React 19, TypeScript, Vite, Material UI, React Router, Axios, Recharts                                      |
| Android packaging    | Capacitor 8 wrapping the web dashboard, with an Android project in the repository                           |
| Firmware             | C++ with Arduino/FreeRTOS, PlatformIO, ESP32/ESP32-S3/ESP32-C3 targets                                      |
| Device communication | ESP-NOW, BLE, Wi-Fi, MQTT with TLS certificates; AWS IoT Core integration                                   |
| Automated checks     | JUnit/Mockito and Spring test support; Vitest and React Testing Library; a firmware glucose-time simulation |

PostgreSQL stores patients and users, access relationships, devices and kits, telemetry, glucose and dose readings, storage and inventory history, prescriptions, schedules, alerts, care-plan delivery records, and provisioning commands and acknowledgements. Backend services perform alert evaluation.

The [backend deployment workflow](.github/workflows/deploy.yml) targets EC2 from `develop`, and the [CI workflow](.github/workflows/tests.yml) runs Maven tests with Java 21. These describe deployment and validation mechanisms, not a guarantee that a public service is currently running. The `cloud/` directories contain placeholders rather than a complete infrastructure provisioning implementation.

---

## Repository Guide

| Path                                                                     | Purpose                                                                                    |
| ------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------ |
| [`backend/spring-api/`](backend/spring-api/)                             | Current REST API, MQTT ingestion, security, care plans, provisioning, analytics, and tests |
| [`frontend/web-dashboard/`](frontend/web-dashboard/)                     | Current React dashboard and Capacitor Android application                                  |
| [`firmware/inner-unit/`](firmware/inner-unit/)                           | Storage and inventory sensing firmware                                                     |
| [`firmware/outer-unit/`](firmware/outer-unit/)                           | Gateway, BLE integration, display, keypad, care plans, and MQTT firmware                   |
| [`firmware/pen-unit/`](firmware/pen-unit/)                               | Pen dose detection, persistent records, and BLE transfer                                   |
| [`firmware/common/`](firmware/common/)                                   | Shared protocols, models, utilities, and provisioning support                              |
| [`database/`](database/)                                                 | PostgreSQL schema and manual provisioning updates                                          |
| [`docs/`](docs/)                                                         | Implementation notes, protocol details, and hardware verification guidance                 |
| [`frontend/mobile-app/rn-app/`](frontend/mobile-app/rn-app/)             | Separate Expo/React Native demo using mock users and sample readings                       |
| [`experiments/dosage_detection/poc/`](experiments/dosage_detection/poc/) | Separate video-based dose-detection experiment                                             |
| [`frontend/project-page/`](frontend/project-page/)                       | Project presentation website, separate from the dashboard                                  |

Directories named `legacy` or `legacy-*` contain historical implementations and are outside the architecture described here.

---

## Development

### Requirements and Configuration

Use Java 21 with Maven or the included Maven wrapper, Node.js 24 with npm for the dashboard, and PlatformIO for firmware. Android packaging additionally requires an Android development environment.

Running the integrated system requires your own PostgreSQL database, backend configuration, and AWS IoT credentials and certificates. Review the selected Spring profile and override environment-specific settings before starting the API. The existing MQTT configuration loads certificate files during startup; a fresh checkout is not a self-contained offline backend setup.

| Configuration area           | What to supply                                                                                                                                      |
| ---------------------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Backend profile and database | `SPRING_PROFILES_ACTIVE` and datasource configuration for your environment; the development profile references `DB_PASSWORD`                        |
| Backend security             | Private JWT and encryption settings; the development profile references `JWT_SECRET`, `JWT_EXPIRATION_MS`, and `ENCRYPTION_KEY`                     |
| MQTT                         | Broker/topic configuration and certificates expected by the backend and firmware; the development profile references `MQTT_BROKER` and `MQTT_TOPIC` |
| Dashboard                    | `VITE_API_URL`, set to the API base URL including `/api/v1`                                                                                         |
| Hardware                     | Board selection, wiring, sensor calibration, device identities, and private provisioning material                                                   |

Keep credentials, private endpoints, certificates, and keys out of public documentation and commits. Frontend `VITE_` variables are build-time client configuration and must not contain secrets.

Review [`database/diasmart_rds_final_schema.sql`](database/diasmart_rds_final_schema.sql) and the applicable scripts in [`database/manual_rds/`](database/manual_rds/) against the target database state. These are SQL schema and manual update files, not a single automated migration pipeline.

### Backend

After supplying configuration and certificates, run from the repository root:

```sh
cd backend/spring-api
mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

The included wrapper can replace `mvn`: use `./mvnw` on Unix-like systems or `.\mvnw.cmd` in PowerShell.

### Dashboard and Android

After setting `VITE_API_URL`, run from the repository root:

```sh
cd frontend/web-dashboard
npm ci
npm run dev
```

To build and open the Android wrapper, run from the dashboard directory:

```sh
npm run build
npm run cap:sync
npm run cap:open:android
```

### Firmware

Each unit is a separate PlatformIO project. After reviewing its hardware and private configuration, build from the repository root:

```sh
pio run -d firmware/inner-unit -e esp32dev
pio run -d firmware/outer-unit -e esp32-s3-devkitc-1
pio run -d firmware/pen-unit -e seeed_xiao_esp32c3
```

The pen project also defines an `esp32-c3-supermini` environment. Choose the target that matches the hardware. Compilation alone does not verify sensor calibration, radio interoperability, or the complete device-to-database workflow.

### Existing Checks

Run these commands in their respective component directories:

| Directory                | Command                 | Purpose                                                                                                           |
| ------------------------ | ----------------------- | ----------------------------------------------------------------------------------------------------------------- |
| `backend/spring-api`     | `mvn test`              | Backend tests for areas including authentication, access control, telemetry, alerts, care plans, and provisioning |
| `frontend/web-dashboard` | `npm run test -- --run` | Component and service tests                                                                                       |
| `frontend/web-dashboard` | `npm run lint`          | ESLint checks                                                                                                     |
| `frontend/web-dashboard` | `npm run build`         | TypeScript and production bundle build                                                                            |

---

## Current Boundaries

- **Offline delivery:** The pen retains dose records until the Outer Unit acknowledges their slots. The Outer Unit stores failed MQTT payloads in LittleFS and retries them, but removes a queued record when the MQTT publish call succeeds. It does not wait for backend persistence acknowledgement before deletion.
- **Device scheduling:** The current care-plan MQTT payload omits per-schedule days of the week and prescription end dates. The firmware therefore evaluates published schedules daily from the plan's effective date.
- **Hardware validation:** The BLE integration notes include a physical two-device verification checklist. Source code and recorded build results do not establish that every hardware scenario has passed.
- **Application completeness:** Some device diagnostics fields still contain placeholders. The separate React Native application uses demo data; the current Android packaging path is the Capacitor dashboard.
- **AI:** AI integration appears in Git history but was reverted on `develop`. The current application provides charts and schedule-based analytics, without an active AI insight service.

---

## Implementation Documentation

- [Device registration and Wi-Fi provisioning](docs/device-registration-wifi-provisioning-implementation.md)
- [Backend device configuration and care-plan workflows](docs/backend-device-config-care-plan-workflows.md)
- [BLE pen and glucometer integration](docs/firmware-parallel-ble-glucometer-summary.md)
- [Outer Unit prescription UI and dose confirmation](docs/firmware-prescription-ui-summary.md)
- [Inner Unit calibration and event-trigger guidance](docs/inner-unit-calibration-and-event-triggers.md)

These notes record different implementation stages. For conflicts, use the active source and manifests; for example, the later BLE integration supersedes the earlier single-device session description in the prescription UI notes.

---

## Team

| Student           | Registration Number |
| ----------------- | ------------------- |
| Ananthasagaran N. | E/21/031            |
| Arnikan U.        | E/21/036            |
| Sanjeevan U.      | E/21/356            |
| Sivasuthan J.     | E/21/386            |

**Supervisors**

- Ms. Yasodha Vimukthi
- Dr. Isuru Nawinne

---

**Department of Computer Engineering**  
**University of Peradeniya**
