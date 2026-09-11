# Dia-Smart: IoT Diabetes Compliance Ecosystem
## Part 1 — Repository Audit & Baseline Verification Report

This document presents a comprehensive technical audit of the current Dia-Smart codebase and baseline verification results, preparing for the integration of a future Gemini-based clinical insight subsystem.

---

## 1. Executive Summary
The Dia-Smart repository contains a prototype IoT diabetes-management ecosystem. The current implementation spans hardware firmware, a Spring Boot backend API, and a React + TypeScript frontend dashboard. The code shows strong support for patient telemetry (blood glucose, insulin doses, refrigerator storage temperatures, and insulin inventory tracking) with relationship-based authorization controls. All backend tests pass successfully, and the frontend compiles without type-check errors. However, there are pre-existing test runner and ESLint issues on the frontend. The database design and access controllers are ready to support a deterministic data aggregation pipeline for Gemini integration with minor extensions.

---

## 2. Git and Workspace State
A Git audit was executed on the workspace before analyzing the code:
* **Repository Root:** `d:\3YP\e21-3yp-dia-smart`
* **Current Branch:** `ai-integration`
* **Git Status:** Working tree clean (nothing to commit).
* **Modified Files:** None.
* **Untracked / Staged Files:** None.
* **Ignored Files:** Local configuration files `.env.local` (both in backend and frontend) and built artifacts are correctly ignored in the repository structure.
* **Git History context:** Recent commits resolve mobile safe-area layouts, styling margins, chart boundaries, and topbar icon alignments.
* **Submodules:** The repository does not utilize any Git submodules.

> [!NOTE]
> All local untracked `.env.local` and `certs` files were present in the workspace before this audit and are correctly listed under Git ignore rules. No files have been modified or staged during this audit.

---

## 3. Repository Structure
The repository is structured as a **Monorepo** containing firmware, backend, database scripts, and multiple frontend prototypes:

```text
e21-3yp-dia-smart/
├── .github/
│   └── workflows/
│       ├── deploy.yml                             # Deploy Spring Boot backend to EC2
│       └── tests.yml                              # Java CI with Maven test run
├── backend/
│   ├── spring-api/                                # Core Spring Boot API (Source of Truth)
│   │   ├── pom.xml                                # Maven build configuration
│   │   ├── src/main/java/com/diasmart/springapi/  # Backend source code
│   │   └── src/test/java/com/diasmart/springapi/  # Backend test suite
│   ├── legacy-node/                               # Deprecated Node.js backend
│   └── sample_backend/                            # Mock express backend (development backup)
├── frontend/
│   ├── web-dashboard/                             # React + TypeScript + Vite Dashboard (Target UI)
│   │   ├── package.json                           # Frontend configurations and scripts
│   │   └── src/                                   # React frontend source code
│   ├── mobile-app/                                # Android/iOS mobile application
│   ├── rn-app/                                    # React Native mobile application
│   └── project-page/                              # Static HTML landing page
├── database/
│   ├── diasmart_rds_final_schema.sql             # SQL script mapping the full RDS schema
│   └── legacy/                                    # Legacy SQL setup scripts
├── firmware/
│   ├── inner-unit/                                # ESP32 cold-storage sensor firmware
│   ├── outer-unit/                                # ESP32 glucometer RS232 bridge firmware
│   └── pen-unit/                                  # ESP32 smart cap dosage tracker firmware
└── docs/                                          # Project documentation and markdown guides
    └── DIA_SMART_AI_PART1_AUDIT.md                # [This Report]
```

---

## 4. Technology Versions
The exact technology stack versions compiled from the active configurations are:

### Backend (`backend/spring-api`)
* **Build Tool:** Maven (pom.xml) with Maven Wrapper
* **Java Version:** JDK 21
* **Spring Boot Version:** `3.5.14` (Parent)
* **Spring Security Version:** Core dependency inherited from Boot Starter Security (`3.5.14`)
* **Database Driver:** PostgreSQL Runtime Driver (managed version)
* **Object-Relational Mapping (ORM):** Spring Data JPA (Hibernate)
* **Validation Framework:** Spring Boot Starter Validation (`jakarta.validation`)
* **JWT Library:** io.jsonwebtoken (`jjwt-api`, `jjwt-impl`, `jjwt-jackson` version `0.12.6`)
* **Testing Frameworks:** JUnit 5 (`spring-boot-starter-test` scope)
* **Mocking Libraries:** Mockito (packaged in Boot Starter Test)
* **In-Memory Test Database:** H2 (`com.h2database:h2` scope)
* **HTTP Client:** Spring RestTemplate / WebClient (WebClient not explicitly declared; RestTemplate available via Web starter)
* **MQTT Client:** Eclipse Paho MQTT Client (`org.eclipse.paho.client.mqttv3` version `1.2.5`)
* **Secrets Management:** me.paulschwarz:spring-dotenv (`4.0.0`)
* **Cryptography:** BouncyCastle Provider (`bcpkix-jdk18on` version `1.78.1`)
* **Monitoring:** Spring Boot Starter Actuator

### Frontend (`frontend/web-dashboard`)
* **Framework:** React `^19.2.6` (React DOM `^19.2.6`)
* **TypeScript Version:** `~6.0.2`
* **Vite Version:** `^8.0.12`
* **Node.js Requirement:** Node `v24.14.1` with npm `11.11.0` (active runner)
* **UI Component Library:** Material UI (MUI) `@mui/material` `^9.0.1`
* **CSS System:** Styled components / CSS-in-JS (via Emotion `@emotion/react` `^11.14.0`)
* **Routing Library:** `react-router-dom` `^7.17.0`
* **HTTP Client:** `axios` `^1.17.0`
* **Chart Library:** `recharts` `^3.8.1`
* **Testing Framework:** Vitest `^4.1.9` with `@testing-library/react` `^16.3.2`
* **Build Bundler:** Vite (with Rolldown under the hood)
* **Linter:** ESLint `^10.3.0`

---

## 5. Backend Architecture
The backend uses **Layered Feature Packages** (where classes are grouped by business feature, then split internally by layer: entity, repository, service, controller).

### Package Breakdown under `com.diasmart.springapi`
* `users`: AppUser and UserSettings management.
* `patients`: Patients profile CRUD and registration.
* `relationships`: access management bridge between users and patients.
* `glucose`: Blood sugar readings storage and retrieval.
* `dose` & `dose_schedules`: Insulin delivery tracking and schedules.
* `storage`: Refrigerator storage sensor monitoring.
* `inventory`: Insulin pen cartridge stock estimation.
* `alerts`: Clinical event generation and acknowledgement.
* `devices` & `deviceevents` & `deviceconfig`: Hardware registry and telemetry processing.
* `mqtt`: Raw MQTTS ingress handling.
* `shared`: Global exceptions, security details, and API envelope.

### Isolated Package Proposal
To add a Gemini-based AI clinical summary subsystem safely:
* Create a new isolated package: `com.diasmart.springapi.ai`
* This package will contain:
  - `controller/AiClinicalSummaryController.java`
  - `dto/AiSummaryRequest.java` and `AiSummaryResponse.java`
  - `service/AiClinicalSummaryService.java` (coordinates data aggregation & FastAPI client calls)
  - `client/FastApiServiceClient.java` (communicates with the Python AI gateway)

---

## 6. Authentication and Authorization
The API is secured using a stateless JSON Web Token (JWT) architecture:
* **Registration & Login Endpoints:** `/api/v1/auth/register` and `/api/v1/auth/login` are marked public in `SecurityConfig.java`.
* **JWT Filter:** [JwtAuthenticationFilter](file:///d:/3YP/e21-3yp-dia-smart/backend/spring-api/src/main/java/com/diasmart/springapi/auth/security/JwtAuthenticationFilter.java) intercepts every request, extracts the Bearer token, validates it via [JwtService](file:///d:/3YP/e21-3yp-dia-smart/backend/spring-api/src/main/java/com/diasmart/springapi/auth/security/JwtService.java), and sets user details in Spring's `SecurityContextHolder`.
* **Roles:** Roles are defined in `UserRole` enum (`PATIENT`, `CAREGIVER`, `DOCTOR`, `ADMIN`).
* **Method-level Security:** Enabled via `@EnableMethodSecurity`.
* **Central Authorization:** [AuthorizationService](file:///d:/3YP/e21-3yp-dia-smart/backend/spring-api/src/main/java/com/diasmart/springapi/shared/security/AuthorizationService.java) handles business-level access control. Instead of comparing user ID directly to patient ID, it checks active relations in the bridge table using [PatientAccessService](file:///d:/3YP/e21-3yp-dia-smart/backend/spring-api/src/main/java/com/diasmart/springapi/relationships/service/PatientAccessService.java).
* **Access Rules:**
  - `ADMIN` is granted universal access bypasses.
  - Other roles (`DOCTOR`, `CAREGIVER`, `PATIENT`) must have an `ACTIVE` row in the `user_patient_access` table with `can_view = true` to view patient information.
  - Clinical edits require `can_edit_prescriptions = true`, and alert clearing requires `can_acknowledge_alerts = true`.

> [!TIP]
> The future AI endpoint should directly reuse `AuthorizationService.authorize(Permission.READ_PATIENT_READINGS, patientId)` to enforce relationship-based authorization, avoiding any custom or secondary security configurations.

---

## 7. User and Relationship Model
Access controls are mapped in PostgreSQL tables:
* **Entities:** `AppUser` (`app_users`), `Patient` (`patients`), and `UserPatientAccess` (`user_patient_access`).
* **Keys:** Primary keys are auto-incrementing `BIGINT` (`userId`, `patientId`, `accessId`). `AppUser` and `Patient` also store random `UUID` values for external APIs.
* **Relations:** `user_patient_access` functions as a bridge table between `user_id` and `patient_id`. It includes flags `can_view`, `can_acknowledge_alerts`, and `can_edit_prescriptions`, an `access_role` enum (`SELF`, `CAREGIVER`, `DOCTOR`), a `status` field (`ACTIVE`, `PENDING`, `REVOKED`), and tracking timestamps.
* **Ownership Verification:** This model is robust. The backend always resolves the current authenticated user's ID and queries `user_patient_access` to confirm if they can view the patient, preventing unauthorized access.

---

## 8. Available Patient Data
We audited the patient data structures to support the first version of the clinical summary:

| Data Category | Table Name | Status in Codebase | Suitable for First AI Summary? |
| :--- | :--- | :--- | :--- |
| **Glucose Readings** | `glucose_readings` | Fully implemented (measured_at, glucose_value_mg_dl, source, meal_context). | Yes. Suitable. |
| **Insulin Doses** | `dose_events` | Fully implemented (injected_at, dose_units, detection_method, dose_status). | Yes. Suitable. |
| **Prescriptions** | `prescriptions` | Fully implemented (prescription_name, start_date, end_date, active, notes). | Yes. Suitable. |
| **Cold Storage Temp** | `storage_readings` | Fully implemented (measured_at, temperature_c, door_state, status). | Yes. Suitable. |
| **Insulin Inventory** | `inventory_readings` | Fully implemented (measured_at, weight_g, estimated_units, inventory_status). | Yes. Suitable. |
| **Clinical Alerts** | `alerts` | Fully implemented (alert_type, severity, message, status, createdAt). | Yes. Suitable. |
| **Device Registry** | `devices` | Fully implemented (device_uid, device_type, last_seen_at, status). | Yes. Suitable. |

---

## 9. Database Design
* **PostgreSQL:** Driven via standard JPA repositories.
* **ddl-auto:** Configuration is set to `update` in `application-dev.yml` and `application-prod.yml`. Hibernate automatically handles schema synchronizations on startup. No Flyway or Liquibase migrations exist.
* **Test Profiles:** The test suite uses an in-memory H2 database in PostgreSQL mode, with `ddl-auto` set to `none`. Schemas are automatically created by JPA scan properties.
* **Indexes:** Indexes exist for searching patient contacts, devices, and querying telemetry (e.g., `idx_glucose_patient_time`, `idx_dose_patient_time`, `idx_storage_patient_time`, `idx_inventory_patient_time`). Date-range queries on these tables are highly efficient.

---

## 10. Frontend Architecture
The React dashboard is organized into components, contexts, and pages:
* **Router:** [AppRouter.tsx](file:///d:/3YP/e21-3yp-dia-smart/frontend/web-dashboard/src/routes/AppRouter.tsx) manages route definitions. Protected pages are wrapped under [ProtectedRoute.tsx](file:///d:/3YP/e21-3yp-dia-smart/frontend/web-dashboard/src/routes/ProtectedRoute.tsx).
* **Context:** [AuthContext.tsx](file:///d:/3YP/e21-3yp-dia-smart/frontend/web-dashboard/src/context/AuthContext.tsx) handles tokens, user roles, and numeric user IDs in `localStorage`.
* **Workspace Page:** [PatientWorkspacePage.tsx](file:///d:/3YP/e21-3yp-dia-smart/frontend/web-dashboard/src/pages/workspace/PatientWorkspacePage.tsx) renders components dynamically based on active user roles defined in `workspaceSections.ts` configuration.
* **Safe Integration Path:** The AI summary component should be added as a card component in `src/components/workspace/AiClinicalSummaryCard.tsx` and registered under `COMPONENT_REGISTRY` in `PatientWorkspacePage.tsx`. It can be added to the grid layouts of `UserRole.DOCTOR` or `UserRole.CAREGIVER` inside `workspaceSections.ts`.

---

## 11. Relevant API Endpoints
A list of active backend API endpoints relevant to clinical summaries:

| Method | Route | Required Role | Description |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/auth/login` | Public | Authenticates user, returns token, role, and userId. |
| **GET** | `/api/v1/patients/{patientId}` | Checked (canView) | Fetches the full patient profile. |
| **GET** | `/api/v1/patients/{patientId}/glucose-readings` | Checked (canView) | Returns paginated glucose history. |
| **GET** | `/api/v1/patients/{patientId}/dose-events` | Checked (canView) | Returns paginated insulin injections history. |
| **GET** | `/api/v1/patients/{patientId}/prescriptions` | Checked (canView) | Returns active/inactive prescriptions list. |
| **GET** | `/api/v1/patients/{patientId}/storage-readings` | Checked (canView) | Returns paginated refrigerator temperature logs. |
| **GET** | `/api/v1/patients/{patientId}/inventory-readings` | Checked (canView) | Returns paginated insulin inventory logs. |
| **GET** | `/api/v1/alerts` | Checked (relationship) | Returns paginated alerts for the user's patient(s). |

---

## 12. Error-Handling Conventions
* **Structure:** [GlobalExceptionHandler](file:///d:/3YP/e21-3yp-dia-smart/backend/spring-api/src/main/java/com/diasmart/springapi/shared/exceptions/GlobalExceptionHandler.java) intercepts failures and returns a standard `ErrorResponse` DTO:
  ```json
  {
    "message": "Error details",
    "errorCode": "ERROR_CODE"
  }
  ```
* **Status Mappings:** Invalid inputs return `422 Unprocessable Entity` (`VALIDATION_ERROR`). Access denied returns `403 Forbidden` (`FORBIDDEN`). Missing resources return `404 Not Found` (`NOT_FOUND`). Unhandled exceptions map to `500 Internal Server Error` (`INTERNAL_ERROR`).
* **Integration Recommendation:** Future AI exceptions should extend `com.diasmart.springapi.common.exceptions.ApiException` or a custom exception to map errors such as `AI_SERVICE_UNAVAILABLE` or `AI_TIMEOUT` directly to matching status codes.

---

## 13. Logging and Monitoring
* **Logging:** standard SLF4J console logging. Levels configured in properties.
* **Actuator:** `spring-boot-starter-actuator` is present in dependencies.
* **Audit Service:** [AuditService](file:///d:/3YP/e21-3yp-dia-smart/backend/spring-api/src/main/java/com/diasmart/springapi/audit/service/AuditService.java) writes log rows into `audit_logs` table (user_id, patient_id, action_type, entity_type, ip_address, JSONB details).

> [!IMPORTANT]
> The future AI subsystem must NOT write complete prompts or responses to logs or the `audit_logs` database table. Instead, log only metadata: `userId`, `patientId`, `promptVersion`, `geminiModel`, `executionTimeMs`, and `tokenCount` inside the `details` JSONB column.

---

## 14. Environment and Secret Management
* **Local environment:** `.env.local` contains DB configurations, JWT secret, and MQTT credentials.
* **Production Properties:** [application-prod.yml](file:///d:/3YP/e21-3yp-dia-smart/backend/spring-api/src/main/resources/application-prod.yml) contains committed credentials (database password, JWT secret, encryption keys). This represents a security vulnerability.
* **Ignore Rules:** Both backend and frontend ignore `.env.local` configurations.

### Recommended Future AI Configuration Variables
For Spring Boot:
* `AI_ENABLED` (boolean, fallback: false)
* `AI_PROVIDER` (string, fallback: "gemini")
* `AI_GATEWAY_URL` (string, target python service URL)
* `AI_INTERNAL_SERVICE_TOKEN` (shared secret header for gateway auth)
* `AI_REQUEST_TIMEOUT_SECONDS` (integer, fallback: 30)

For the FastAPI AI Gateway:
* `GEMINI_API_KEY` (secret, read from environment or secret manager)
* `GEMINI_MODEL` (string, fallback: "gemini-1.5-flash")

---

## 15. AWS and Deployment Architecture
* **Spring Boot Service:** Deployed to an AWS EC2 instance. It runs as a systemd service (`diasmart`) managed via target `/opt/diasmart-backend/springapi-0.0.1-SNAPSHOT.jar`.
* **Database:** AWS RDS PostgreSQL instance (`diasmart-db....rds.amazonaws.com`).
* **Frontend:** Built via Vite package scripts and can be deployed via AWS Amplify Console.
* **Gateway Hosting:** The FastAPI Python service should run on an ECS container or a small EC2 instance. It should communicate privately with the Spring Boot backend inside the VPC, not exposed to the public internet.

---

## 16. Testing Setup
* **Backend:** JUnit 5 and Mockito. Tests run in an isolated test profile with H2.
* **Frontend:** Vitest unit/component tests in Node environment.
* **Future AI Testing Strategy:**
  1. Unit tests: mock `FastApiServiceClient` network requests in `AiClinicalSummaryService` tests.
  2. Integration tests: Mock the Python Gateway API using `WireMock` to test Spring Boot's error handling and parsing conventions.
  3. Validate responses using synthetic, deterministic patient data templates.

---

## 17. Baseline Commands
The repository scripts reveal the standard execution commands:

* **Backend Clean:** `.\mvnw.cmd clean`
* **Backend Test:** `.\mvnw.cmd test`
* **Backend Production Build:** `.\mvnw.cmd package -DskipTests`
* **Backend Run:** `.\mvnw.cmd spring-boot:run`
* **Frontend Install:** `npm install` (in `frontend/web-dashboard`)
* **Frontend Test:** `npm run test -- --run` (Vitest single run)
* **Frontend Lint:** `npm run lint` (runs `eslint .`)
* **Frontend Type Check:** `npx tsc -b` (TypeScript build command)
* **Frontend Production Build:** `npm run build` (runs `tsc -b && vite build`)

---

## 18. Baseline Results

We successfully executed the baseline checks:

### 1. Backend Tests
* **Command:** `.\mvnw.cmd test`
* **Result:** **SUCCESS**
* **Test Metrics:** Tests run: **119**, Failures: **0**, Errors: **0**, Skipped: **0**
* **Duration:** 10.775 seconds

### 2. Backend Production Build
* **Command:** `.\mvnw.cmd package -DskipTests`
* **Result:** **SUCCESS**
* **Output:** Repackaged fat JAR `target/springapi-0.0.1-SNAPSHOT.jar` generated successfully.

### 3. Frontend Type Check
* **Command:** `npx tsc -b`
* **Result:** **SUCCESS** (0 compile or type errors)

### 4. Frontend Tests
* **Command:** `npm run test -- --run`
* **Result:** **FAILED (Exit Code: 1)**
* **Details:** 9 test files passed (34 unit tests passed), but 7 test files failed.
* **Failure Analysis:** Pre-existing ES modules resolution error when importing MUI Material internal transition components in Vitest (`Directory import react-transition-group/TransitionGroupContext is not supported resolving ES modules`). The application code itself compiles fine, but the Vitest runner requires configuration adjustments to transpile these modules properly.

### 5. Frontend Production Build
* **Command:** `npm run build`
* **Result:** **SUCCESS**
* **Details:** Rolldown compiler successfully bundled the app into the `dist/` directory.

### 6. Frontend Lint
* **Command:** `npm run lint`
* **Result:** **FAILED (Exit Code: 1)**
* **Details:** Found **123 problems (116 errors, 7 warnings)**.
* **Vulnerabilities:** Unused variables (`@typescript-eslint/no-unused-vars`), explicit any type usage (`@typescript-eslint/no-explicit-any`), empty interfaces, and calling `setState` inside the `useEffect` body in `PatientWorkspacePage.tsx`.

---

## 19. AI Integration Feasibility Assessment
* **Backend Suitability: Ready with minor changes.** Needs a new controller, client, and minor date-range query additions in telemetry repositories.
* **Frontend Suitability: Ready with minor changes.** Layout allows easy registration of a new dashboard card component.
* **Data Availability: Ready.** Database tables for glucose, dose, prescriptions, temperature, inventory, and alerts are fully populated and structured.
* **Authorization Readiness: Ready.** Centralized `AuthorizationService` is highly reusable.
* **Database-query Readiness: Requires minor work.** Need to write date-range query methods for `GlucoseReadingRepository`, `StorageReadingRepository`, and `InventoryReadingRepository`.
* **Testing Readiness: Ready.** Existing Mockito/Vitest frameworks are well integrated.
* **Security Readiness: Requires moderate work.** Production properties must externalize hardcoded credentials.

---

## 20. Recommended First Feature Scope
The **AI-Assisted Clinical Summary** is highly feasible for Part 2. The data suitability for the first version:

* **Glucose Summary:** **Include in first version.** Analyzes average, minimum, maximum, and threshold excursions.
* **Insulin Dose Summary:** **Include in first version.** Correlates dosage compliance with schedules.
* **Prescription Adherence:** **Include in first version.** Compares recorded doses to active prescription schedules.
* **Storage Temperature Safety:** **Include in first version.** Reports storage temperature ranges and open door durations.
* **Inventory Stock Status:** **Include after minor backend work.** Reports remaining quantity and warns of critical stock.
* **Clinical Alerts Summary:** **Include in first version.** Mentions open or unresolved alerts.

---

## 21. Proposed Spring-to-FastAPI Request Contract
A minimal request payload containing only fields supported by the current schema:

```json
{
  "request_id": "4020a112-a720-4100-b3e1-3298c11aa090",
  "request_type": "CLINICAL_SUMMARY",
  "prompt_version": "clinical-summary-v1",
  "patient_id": 1,
  "glucose_readings": [
    {
      "value_mg_dl": 142.5,
      "measured_at": "2026-08-01T20:30:00Z",
      "meal_context": "AFTER_MEAL"
    }
  ],
  "doses": [
    {
      "dose_units": 10.0,
      "injected_at": "2026-08-01T08:15:00Z",
      "dose_status": "TAKEN_WITHIN_WINDOW"
    }
  ],
  "prescriptions": [
    {
      "prescription_name": "Actrapid morning schedule",
      "start_date": "2026-07-01",
      "end_date": "2026-10-01"
    }
  ],
  "storage_excursions": {
    "avg_temperature_c": 5.4,
    "excursion_count": 0
  },
  "alerts": [
    {
      "alert_type": "GLUCOSE_HIGH",
      "message": "Blood sugar reading at 210 mg/dL",
      "status": "OPEN"
    }
  ]
}
```

---

## 22. Proposed response FastAPI-to-Spring Contract
A structured JSON response to ensure compliance and validation:

```json
{
  "summary": "Patient demonstrates stable glycaemic control, though high readings are observed post-breakfast.",
  "observations": [
    {
      "statement": "Glucose excursion of 210 mg/dL occurred after breakfast.",
      "evidence_references": ["GLUCOSE_HIGH"]
    }
  ],
  "correlations": [
    {
      "statement": "Glucose peaks correlate with a 30-minute delay in morning insulin injection.",
      "confidence": "moderate",
      "evidence_references": ["GLUCOSE_HIGH", "TAKEN_LATE"]
    }
  ],
  "uncertainties": [
    "Post-dinner glucose readings are missing for two days, limiting complete nocturnal assessment."
  ],
  "discussion_points": [
    "Assess if current morning insulin timing needs adjusting relative to breakfast."
  ],
  "safety_notice": "Disclaimer: AI-generated summary. Verify all insulin dosage adjustments with a clinical physician.",
  "provider_metadata": {
    "provider": "gemini",
    "model": "gemini-1.5-flash",
    "prompt_version": "clinical-summary-v1"
  }
}
```

---

## 23. Recommended Integration Points
* **FastAPI Service Location:** Create `/ai-service` at the repository root.
* **Spring Boot Package:** Create `com.diasmart.springapi.ai` inside `backend/spring-api/src/main/java`.
* **Authorization Reuse:** Reuse `AuthorizationService.authorize(...)` inside the AI service class.
* **Telemetry Context Sources:** Inject `GlucoseReadingRepository`, `DoseEventRepository`, `PrescriptionRepository`, `StorageReadingRepository`, `InventoryReadingRepository`, and `AlertRepository`.
* **HTTP Client:** Define RestTemplate bean or configure a RestClient.
* **Public API Endpoint:** `GET /api/v1/patients/{patientId}/ai-summary`
* **Frontend component:** `frontend/web-dashboard/src/components/workspace/AiClinicalSummaryCard.tsx`
* **Vite API client:** Extend `frontend/web-dashboard/src/services/api.ts`

---

## 24. Risk Register

| Risk Category | Severity | Evidence | Impact | Recommended Mitigation | Stage |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **Committed Secrets** | Critical | Hardcoded credentials in `application-prod.yml`. | Database breach, credential exposure. | Move passwords to environment variables. | Part 3 |
| **Patient Privacy** | High | Sending raw identifiers to Gemini. | PII exposure to external AI models. | Pseudonymize patient data in request contract. | Part 3 |
| **Vitest Run Errors** | Medium | MUI transition modules resolution fails in Vitest. | CI pipeline failure. | Add resolution rules or alias mappings to `vitest.config.ts`. | Part 4 |
| **ESLint Warnings** | Low | 123 ESLint problems on web dashboard. | Static check failures, code smell. | Perform controlled cleanup of unused variables. | Part 4 |
| **Missing Repositories** | Low | Telemetry repositories lack date-range queries. | Retrieval inefficiency, memory bloat. | Implement `findByPatientIdAndMeasuredAtBetween` queries. | Part 3 |

---

## 25. Revised Part-by-Part Plan
1. **Part 2 — FastAPI AI Foundation:** Setup Python structure under `/ai-service`, integrate Gemini SDK, configure prompt template, add basic test suite with mocked Gemini responses.
2. **Part 3 — Spring Boot Integration:** Clean up hardcoded production secrets, implement date-range queries, write `AiClinicalSummaryService` with client call, apply `AuthorizationService`, test with mock gateway.
3. **Part 4 — React Integration:** Add `AiClinicalSummaryCard.tsx` React component, register card in workspace grid configuration, handle loading and error states.
4. **Part 5 — Gemini Integration:** Configure real Gemini connection on FastAPI, write prompt validation checks, execute end-to-end integration tests.

---

## 26. Blockers and Open Technical Questions
1. **Gemini Key Store:** Where will the production `GEMINI_API_KEY` be hosted? (AWS Secrets Manager or ECS Task Environment)?
2. **MUI Vitest Resolution:** Is it acceptable to modify the `vitest.config.ts` file to bypass the MUI ES modules resolution errors during the next phase?

---

## 27. Recommended Next Action
Obtain approval on this Part 1 audit report, then proceed to **Part 2: FastAPI AI Foundation** setup.
