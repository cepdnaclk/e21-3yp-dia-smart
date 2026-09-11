# Dia-Smart: AI-Assisted Clinical Insight Subsystem - Part 3 Implementation Report

This report documents the design, architecture, security hardening, and verification of the secure Spring Boot backend integration slice connecting to the standalone FastAPI microservice (configured in mock mode).

---

## 1. Overview of the Architecture

The Dia-Smart Spring Boot backend acts as the secure, authenticated orchestrator that sits between clients (e.g., clinicians, patients) and the FastAPI AI service.

```mermaid
sequenceDiagram
    autonumber
    actor User as Authenticated Caller (JWT)
    participant SB as Spring Boot Backend
    participant DB as PostgreSQL Database
    participant FA as FastAPI Microservice

    User->>SB: GET /api/v1/patients/{patientId}/ai-summary?from=...&to=...
    Note over SB: 1. Authorize caller via AuthorizationService (READ_PATIENT_READINGS)<br/>2. Validate feature flag & configuration<br/>3. Validate timezone-aware ISO-8601 period & max date range
    SB->>DB: 4. Query telemetry aggregates, alerts, inventory, adherence (UTC range)
    DB-->>SB: Return stats projections & filtered readings
    Note over SB: 5. Transform telemetry to opaque sequential references (category:ref-XXX)<br/>6. Anonymize patient info (patient-ref-[UUID])<br/>7. Strip all database, patient, and device IDs
    SB->>FA: 8. POST /internal/v1/insights/clinical-summary (Bearer token)
    FA-->>SB: Return clinical summary response JSON (MockProvider)
    Note over SB: 9. Validate response ID & safety notice<br/>10. Verify provider is mock & confidence levels<br/>11. Verify citations count & categories<br/>12. Apply clinical safety regex filter
    SB->>DB: 13. Write sanitized audit log (metadata only; no PII/pseudonyms)
    SB-->>User: 14. Return camelCase JSON (200 OK)
```

### Execution Order Guarantee
The controller enforces a strict sequence:
1. **Caller Authorization**: `authorizationService.authorizePatientAccess(patientId, Permission.READ_PATIENT_READINGS)` executes before any data retrieval, feature flag checking, or gateway communication.
2. **Feature & Config Checks**: Validates `diasmart.ai.enabled` and token presence.
3. **Period Validation**: Validates timezone-aware ISO-8601 date range (max range enforced, chronological order).
4. **Data Retrieval & Transformation**: Assembles telemetry context using trusted patient targets and opaque references.
5. **Gateway Invocation**: Transmits payload to FastAPI microservice using dedicated `RestClient`.
6. **Response Validation**: Enforces structural, citation, and safety boundaries.
7. **Sanitized Audit Recording**: Logs request outcome without exposing sensitive data.

If authorization or validation fails at any point, the downstream AI gateway is **never invoked**.

---

## 2. Configuration & Isolated Client Policy

AI features are configured via environment variables mapped under the `diasmart.ai` property prefix:

### Application Properties
- **`diasmart.ai.enabled`**: Set to `${AI_ENABLED:false}` (disabled by default).
- **`diasmart.ai.gateway-url`**: Set to `${AI_GATEWAY_URL:http://127.0.0.1:8000}`.
- **`diasmart.ai.internal-service-token`**: Set to `${AI_INTERNAL_SERVICE_TOKEN:}`.
- **`diasmart.ai.connect-timeout`**: Set to `${AI_CONNECT_TIMEOUT:3s}`.
- **`diasmart.ai.read-timeout`**: Set to `${AI_READ_TIMEOUT:30s}`.
- **`diasmart.ai.max-date-range-days`**: Set to `${AI_MAX_DATE_RANGE_DAYS:31}` (limits request period).
- **`diasmart.ai.max-alerts`**: Set to `${AI_MAX_ALERTS:100}` (caps alert items passed to AI).
- **`diasmart.ai.max-selected-events`**: Set to `${AI_MAX_SELECTED_EVENTS:100}` (caps timeline context events).

### Dedicated `RestClient` Bean Isolation
- Created as an isolated Spring bean (`aiRestClient`) using an independent `RestClient.Builder`.
- Configured with dedicated `SimpleClientHttpRequestFactory` with strict connection timeout (3s) and read timeout (30s).
- **No Shared Headers**: The client does NOT share base headers, default interceptors, or authentication contexts with external client beans. The internal service token is injected per-request or via isolated client configuration.
- **Token Sanitization**: The internal service token and full authorization header are never written to log files or serialized in error messages.

---

## 3. Data Minimization & Privacy Protection

The system aggregates telemetry records while strictly enforcing privacy and data minimization:

1. **Complete ID Stripping**:
   - Patient names, contact info, email addresses, and database primary keys (e.g., patient ID, user ID, device ID, reading ID) are completely stripped.
   - The patient is represented via a request-scoped pseudonymous identifier: `"patient-ref-" + UUID.randomUUID()`.

2. **Sequential Opaque Evidence References**:
   - Telemetry citations do NOT contain database IDs or device serials.
   - All references are formatted as `category:ref-XXX` using a request-scoped sequential counter (e.g., `glucose_reading:ref-001`, `alert:ref-002`, `storage_excursion:ref-003`).
   - Categories are strictly restricted to the approved set: `glucose_reading`, `dose_event`, `storage_excursion`, `alert`, `inventory_event`, and `adherence_record`.

3. **Domain-Specific Aggregation & Trusted Sources**:
   - **Glucose**: Target bounds are read strictly from the patient profile (`targetGlucoseMinMgDl` / `targetGlucoseMaxMgDl`). If patient targets are not configured, hardcoded clinical defaults (70/180) are **never used**; the glucose statistics block is safely omitted from the payload.
   - **Storage**: Does NOT rely on hardcoded temperature ranges (e.g., 2°C–8°C). Instead, storage excursion counts and readings rely solely on trusted device/system-classified `temperatureStatus` values (`EXCURSION` and `WARNING`) via `StorageReadingRepository.countExcursionsByStatus`. If no storage readings exist, the storage section is omitted.
   - **Adherence**: Evaluated from scheduled and recorded administrations. All artificial clamping (`Math.min`) has been eliminated. If adherence records contain mathematical inconsistencies, the adherence section is omitted rather than falsifying clinical data.
   - **Inventory**: Aggregates verified device inventory records, tracking remaining units and status without exposing hardware identifiers.

4. **Event Timeline Minimization**:
   - Timeline events are deterministic, sorted chronologically, and capped at `maxSelectedEvents`.
   - Free-text clinician notes, user comments, and raw text entries are excluded.

---

## 4. Error Mapping & Exception Handling

The backend maps all AI gateway responses and transport exceptions to standardized, secure Spring Boot HTTP error responses:

| Microservice Condition / Status | Spring Boot Exception | Client Status Code | Error Code |
|---|---|---|---|
| 400 Bad Request from AI | `AiGatewayBadRequestException` | 502 Bad Gateway | `AI_GATEWAY_BAD_REQUEST` |
| 401 / 403 Unauthorized | `AiGatewayAuthenticationException` | 502 Bad Gateway | `AI_GATEWAY_AUTHENTICATION_ERROR` |
| 404 Not Found from AI | `AiGatewayNotFoundException` | 502 Bad Gateway | `AI_GATEWAY_NOT_FOUND` |
| 413 Payload Too Large | `AiGatewayRequestTooLargeException` | 502 Bad Gateway | `AI_GATEWAY_REQUEST_TOO_LARGE` |
| 422 Unprocessable Entity | `AiGatewayUnprocessableEntityException` | 502 Bad Gateway | `AI_GATEWAY_UNPROCESSABLE_ENTITY` |
| 429 Rate Limited | `AiGatewayRateLimitException` | 502 Bad Gateway | `AI_GATEWAY_RATE_LIMITED` |
| 500+ Internal Error from AI | `AiGatewayUpstreamErrorException` | 502 Bad Gateway | `AI_GATEWAY_UPSTREAM_ERROR` |
| Read / Connect Timeout | `AiGatewayTimeoutException` | 504 Gateway Timeout | `AI_GATEWAY_TIMEOUT` |
| Connection Refused / Network Down | `AiGatewayUnavailableException` | 503 Service Unavailable | `AI_GATEWAY_UNAVAILABLE` |
| Feature Disabled (`diasmart.ai.enabled=false`) | `AiDisabledException` | 503 Service Unavailable | `AI_DISABLED` |
| Token Missing or Empty | `AiConfigurationException` | 503 Service Unavailable | `SERVICE_UNAVAILABLE` |
| Response Validation Failed | `AiInvalidResponseException` | 502 Bad Gateway | `AI_INVALID_RESPONSE` |

*Note: Exception messages returned to API callers and logged in application logs are sanitized. They contain only the status code and high-level description; upstream request URLs, bearer tokens, and internal response bodies are excluded.*

---

## 5. Defense-in-Depth Response Validation

Before accepting and returning the AI summary response, the backend applies strict verification layers:

1. **Request Matching**: Validates that `response.requestId == request.requestId`.
2. **Provider Assertion**: Verifies that `response.provider == "mock"`.
3. **Safety Notice Disclaimer**: Ensures the disclaimer matches `APPROVED_SAFETY_NOTICE` verbatim.
4. **Clinical Safety Pattern Scanning**: Evaluates all generated summaries, observations, and recommendations against strict clinical safety regexes. Prohibits direct diagnostic claims, medication changes, insulin dosage calculations, and emergency instructions.
5. **Evidence Citation Verification**:
   - Checks that all citations match known opaque evidence IDs from the request context.
   - Verifies that correlation items have at least 2 distinct citations.
   - Asserts that citations use valid categories (`glucose_reading`, `dose_event`, etc.).
   - Asserts valid confidence levels (`HIGH`, `MEDIUM`, `LOW`).
   - Asserts non-empty uncertainties list.

---

## 6. Sanitized Audit Logging

Audit logs are written via the central `AuditService.record()` method and SLF4J structured logging:
- **Logged Attributes**:
  - Request ID (UUID)
  - Caller principal / username (from Spring Security context)
  - Action name: `"AI_CLINICAL_SUMMARY_GENERATED"` / `"AI_CLINICAL_SUMMARY_FAILED"`
  - Execution duration (milliseconds)
  - Gateway status code / outcome
- **Excluded Attributes (Never Logged)**:
  - Patient database ID or national ID
  - Request-scoped patient pseudonyms (`patient-ref-UUID`)
  - AI internal service token or Authorization headers
  - Full request context payloads or sensitive telemetry readings

---

## 7. Secrets Management & Production Hardening

### Externalization of Production Secrets
- **Database Password**: Externalized in `application-prod.yml` as `${DB_PASSWORD}` without fallback.
- **Encryption Key**: Externalized in `application-prod.yml` as `${ENCRYPTION_KEY}` without fallback.
- **JWT Secret**: Externalized in `application-prod.yml` as `${JWT_SECRET}` without fallback.
- **Development Profile**: Synthetic local values are provided in `application-dev.yml` (e.g., `${JWT_SECRET:dev-super-secure-local-jwt-secret-key-32-chars-minimum}`) for seamless local engineering.
- **Rotation Checklist**: Created `docs/DIA_SMART_SECRET_ROTATION_CHECKLIST.md` detailing the operational steps to rotate production secrets in AWS Systems Manager Parameter Store / Secrets Manager, update ECS task definitions, and revoke previously exposed credentials without committing literal secrets to git.

---

## 8. Verification Results

### Automated Test Suite Execution

#### 1. Spring Boot Backend Tests (Maven)
- Ran complete test suite:
  ```powershell
  .\mvnw.cmd test
  ```
- **Results**: **164 tests run, 0 failures, 0 errors, 0 skipped** (BUILD SUCCESS).
- **New Test Classes Added**:
  - `AiSerializationPrivacyTest`: Asserts distinctive synthetic numeric IDs (456789, 918273645, 782364) never leak into serialized JSON payloads.
  - `AiClientSecurityTest`: Asserts Bearer token injection, header isolation, and timeout configurations.
  - `AiErrorMappingTest`: Asserts correct status codes and error code translations for all 10 gateway error states.
  - `AiGatewayResponseValidatorTest`: Asserts provider checks, citation counts, confidence ratings, and clinical safety regexes.
  - `PatientAiContextServiceTest`: Asserts unclamped adherence, omission of unconfigured metrics, and sequential opaque references.
  - `AiClinicalSummaryControllerTest`: Asserts execution order, authorization enforcement, and audit recording.

#### 2. Backend Package Build
- Verified full package compilation:
  ```powershell
  .\mvnw.cmd package -DskipTests
  ```
- **Results**: **BUILD SUCCESS** (JAR artifact built cleanly).

#### 3. FastAPI AI Microservice Tests (Pytest)
- Ran complete Python test suite:
  ```powershell
  .\.venv\Scripts\python.exe -m pytest -v
  ```
- **Results**: **151 passed, 2 warnings** in 0.98s.
- **Code Quality**:
  - `ruff check app tests`: 0 issues found.
  - `mypy app`: 0 issues found in 17 source files.

#### 4. End-to-End Live Local Mock Verification
- Started local FastAPI mock microservice with `AI_PROVIDER=mock`.
- Verified endpoints:
  - `GET http://127.0.0.1:8000/health` -> `200 OK` (Healthy, MockProvider)
  - `POST http://127.0.0.1:8000/internal/v1/insights/clinical-summary` (unauthorized) -> `401 Unauthorized` (`AI_UNAUTHORIZED`)
  - `POST http://127.0.0.1:8000/internal/v1/insights/clinical-summary` (authorized mock payload) -> `200 OK` (Valid summary with disclaimer, citations, and correlations)
- Service terminated cleanly after verification.
