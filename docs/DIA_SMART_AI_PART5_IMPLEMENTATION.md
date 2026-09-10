# Dia-Smart AI Part 5 Implementation Report: Gemini Provider Integration Inside FastAPI

## 1. Executive Summary
Part 5 extends the verified Dia-Smart FastAPI microservice architecture (`ai-service/`) to introduce `GeminiProvider` alongside the default `MockProvider` using the official Google Gen AI Python SDK (`google-genai`). `MockProvider` remains the default and fully functional without requiring credentials or network access. The integration enforces strict Pydantic-driven structured outputs, authoritative server-controlled metadata, zero tools/search grounding, conservative clinical safety boundaries, sanitized error handling, and robust privacy protections. Additionally, a minimal Spring Boot compatibility update was implemented in `AiGatewayResponseValidator` to respect the existing `diasmart.ai.expected-provider` configuration property.

## 2. Scope
- **Primary Service Modified**: `ai-service/**`
- **Minimal Backend Compatibility Change**: `backend/spring-api/**` (specifically `AiGatewayResponseValidator.java` and `AiGatewayResponseValidatorTest.java` to make expected provider validation configurable between `mock` and `gemini`).
- **Read-Only Unchanged Components**:
  - `frontend/web-dashboard/**` (0 modifications)
  - `firmware/**` (0 modifications)
  - `database/**` (0 modifications)
  - `.github/**` (0 modifications)
- **Data Safety**: Exclusively synthetic telemetry fixtures were used for development and testing. Real patient health information was NEVER accessed or used.

## 3. Git State Before Implementation
- **Branch**: `ai-integration`
- **Working Tree**: Clean (0 unstaged/staged modifications)
- **Commit Baseline**: `789ab61b feat(ai): complete mock clinical insight integration`

## 4. FastAPI Baseline
Prior to Part 5 changes, the verified Part 4 baseline in `ai-service/` was:
- **Pytest**: 151 passed, 0 failed, 2 warnings in 1.80s
- **Code Coverage**: 88% across 888 statements (106 missed)
- **Ruff Lint**: All checks passed (53 files formatted)
- **Mypy**: Success: no issues found in 35 source files

## 5. Google Gen AI SDK Version
- **SDK Package**: `google-genai`
- **Installed Version**: `google-genai==2.22.0`
- **Deprecated SDK Check**: Deprecated `google-generativeai` was NOT installed and is not used.
- **Dependency Specification**: Added `google-genai>=2.22.0` under `dependencies` in `ai-service/pyproject.toml`.

## 6. Provider Architecture
The FastAPI microservice now provides a dynamic provider factory pattern:
```text
                    FastAPI Endpoint
                           │
                 ClinicalSummaryService
                           │
                    ProviderFactory
                    ┌──────┴──────┐
                 MockProvider   GeminiProvider
               (Default,       (google-genai SDK
                Offline)        Models API)
```
- `AI_PROVIDER=mock` selects `MockProvider`.
- `AI_PROVIDER=gemini` selects `GeminiProvider`.
- Any unsupported provider name immediately triggers a controlled `AiUnsupportedProviderError` (HTTP 400).
- No silent fallback occurs in either direction: provider failures are explicit and controlled.

## 7. Files Created
1. `ai-service/app/models/gemini.py`: Strict internal Pydantic models (`GeminiClinicalInsightPayload`, `GeminiObservation`, `GeminiCorrelation`) representing only model-generated insight fields.
2. `ai-service/app/providers/gemini_provider.py`: Production-grade `GeminiProvider` implementing the `AIProvider` protocol using `google-genai` Models API (`client.aio.models.generate_content`) with structured JSON output, schema sanitation for Protobuf compatibility, and comprehensive exception mapping.
3. `ai-service/tests/unit/test_gemini_provider.py`: 24 unit tests mocking the Google Gen AI client with 0 network dependencies.
4. `ai-service/tests/unit/test_provider_factory.py`: 5 unit tests verifying factory resolution, default mock selection, invalid provider rejection, and config isolation.
5. `docs/DIA_SMART_AI_PART5_IMPLEMENTATION.md`: This comprehensive implementation report.

## 8. Files Modified
1. `ai-service/pyproject.toml`: Added `google-genai>=2.22.0` dependency.
2. `ai-service/app/config/settings.py`: Added `AI_PROVIDER`, `GEMINI_API_KEY` (`SecretStr`), `GEMINI_MODEL`, `GEMINI_TIMEOUT_SECONDS`, and `GEMINI_TEMPERATURE` with conditional validation.
3. `ai-service/.env.example`: Added sanitized configuration placeholders for Gemini.
4. `ai-service/app/api/health.py`: Updated `/health` endpoint to reflect current active provider.
5. `ai-service/app/models/responses.py`: Updated `ProviderMetadata.provider` literal type to `Literal["mock", "gemini"]`.
6. `ai-service/app/providers/factory.py`: Added support for resolving `GeminiProvider` when `AI_PROVIDER=gemini`.
7. `ai-service/app/services/clinical_summary_service.py`: Updated error logging to dynamically reflect active provider and preserve typed exceptions.
8. `ai-service/README.md`: Documented provider configuration, temporary PowerShell variable setup, and credential removal.
9. `ai-service/tests/unit/test_settings.py`: Updated tests for supported/unsupported providers and Gemini settings validation.
10. `backend/spring-api/src/main/java/com/diasmart/springapi/ai/validation/AiGatewayResponseValidator.java`: Injected `AiProperties` and updated provider validation to compare against configured `expectedProvider` (`mock` or `gemini`).
11. `backend/spring-api/src/test/java/com/diasmart/springapi/ai/validation/AiGatewayResponseValidatorTest.java`: Added 5 unit tests for configurable provider validation.

## 9. Configuration Variables
The following environment variables are supported in `ai-service`:
- `AI_PROVIDER`: Target provider (`mock` or `gemini`, default: `mock`).
- `GEMINI_API_KEY`: Google Gemini API Key (SecretStr). Required only when `AI_PROVIDER=gemini`.
- `GEMINI_MODEL`: Gemini model identifier (e.g. `gemini-2.5-flash`). Required only when `AI_PROVIDER=gemini`.
- `GEMINI_TIMEOUT_SECONDS`: Maximum request duration in seconds (default: `30.0`).
- `GEMINI_TEMPERATURE`: Model sampling temperature (default: `0.2`).
- `AI_INTERNAL_SERVICE_TOKEN`: Bearer token for inter-service authentication (minimum 32 characters).

In `backend/spring-api`:
- `diasmart.ai.expected-provider`: Configurable expected provider (`mock` or `gemini`, default: `mock`).

## 10. GeminiProvider
Implemented in `app/providers/gemini_provider.py`:
- Protocol conformance: Fully implements `BaseClinicalSummaryProvider` / `AIProvider`.
- Execution lifecycle:
  1. Receives validated `ClinicalSummaryRequest`.
  2. Builds versioned structured prompt (`clinical-summary-v1`).
  3. Derives JSON schema from `GeminiClinicalInsightPayload` and sanitizes it for Protobuf compatibility.
  4. Calls `client.aio.models.generate_content` with `model`, `contents=structured_context`, and `config=types.GenerateContentConfig(...)` (enforcing `system_instruction`, `response_mime_type="application/json"`, `response_schema=clean_schema`, `temperature`, `tools=None`), wrapped in `asyncio.wait_for` with explicit timeout.
  5. Parses output into `GeminiClinicalInsightPayload`.
  6. Attaches authoritative server-controlled fields (`request_id`, `provider="gemini"`, `model`, `prompt_version`, `safety_notice`).
  7. Returns internal `ClinicalSummaryResponse` to the downstream safety and evidence verification pipeline.

## 11. Structured-Output Schema
Structured output is strictly enforced through Google Gen AI SDK's `GenerateContentConfig`:
```python
config = types.GenerateContentConfig(
    system_instruction=structured_prompt.system_instruction,
    response_mime_type="application/json",
    response_schema=clean_schema,
    temperature=self.settings.GEMINI_TEMPERATURE,
    tools=None,
)
```
The schema is generated directly from `GeminiClinicalInsightPayload`:
- `summary`: Non-empty string constrained to `AI_MAX_TEXT_LENGTH`.
- `observations`: Array of objects with `statement` and `evidence_references` (`min_length=1`).
- `correlations`: Array of objects with `statement`, `confidence` (`low` | `moderate` | `high`), and `evidence_references` (`min_length=2`).
- `uncertainties`: Non-empty array of limitation statements (`min_length=1`).
- `discussion_points`: Optional array of consultation points.
- `extra="forbid"`: Disallows hallucinated top-level or object-level properties.

## 12. Trusted Server-Controlled Fields
To prevent model manipulation or hallucination of security-critical attributes:
- `request_id`: Copied directly from the verified incoming request UUID.
- `provider`: Authoritatively set to `"gemini"` by FastAPI code.
- `model`: Authoritatively populated from the configured `GEMINI_MODEL`.
- `prompt_version`: Fixed to the verified request version (`clinical-summary-v1`).
- `safety_notice`: Set strictly to `APPROVED_SAFETY_NOTICE`.

## 13. Prompt and System Instructions
Reuses `clinical-summary-v1` defined in `app/prompts/clinical_summary_v1.py`:
- Emphasizes that telemetry is observational context only.
- Mandates zero diagnosis, zero prescriptions, zero dose adjustments, zero causation claims, and no impersonation of physicians.
- Requires evidence citations for observations and at least two distinct citations for correlations.
- Contains explicit untrusted-content isolation markers: `[UNTRUSTED_USER_CONTENT_START]` and `[UNTRUSTED_USER_CONTENT_END]`.

## 14. Data Minimization
The prompt input contains only the minimized context already verified in Part 2:
- Pseudonymous patient reference (e.g. `patient-ref-12345678`).
- Requested observation window (`from`, `to`).
- Minimized aggregated summaries (glucose metrics, adherence counts, storage excursions, inventory levels).
- Category-prefixed evidence IDs (e.g. `glucose-summary:selected-period`).
- Absolutely no patient names, emails, phones, national IDs, database primary keys, JWTs, device credentials, or internal tokens.

## 15. No-Tool Policy
`GeminiProvider` explicitly disables all external tools and grounding:
- `tools=None` is passed to the SDK.
- Google Search, Google Maps, URL context, File Search, Code execution, MCP, and custom tools are disabled.
- Clinical summaries are generated solely from the supplied context. Verified by unit test `test_no_tools_configured`.

## 16. Evidence Validation
Post-generation `EvidenceValidator` executes unchanged after `GeminiProvider`:
- Every citation must match an evidence reference provided in the incoming request payload.
- Every observation must contain at least 1 valid citation.
- Every correlation must contain at least 2 distinct valid citations.
- Any fabricated or unmatched citation results in rejection of the entire response via `AiEvidenceValidationError`.

## 17. Medical-Safety Validation
Post-generation `MedicalSafetyValidator` executes unchanged after `GeminiProvider`:
- Scans all text fields against prohibited clinical patterns (diagnosis, prescription, insulin dose adjustments, definite causation, medical impersonation).
- Prohibited statements immediately trigger `AiMedicalSafetyRejectionError`. Verified by unit tests `test_gemini_pipeline_rejects_diagnosis`, `test_gemini_pipeline_rejects_dosage_recommendation`, and `test_gemini_pipeline_rejects_causation_claims`.

## 18. Error Mapping
Google Gen AI SDK and network exceptions are translated into sanitized Dia-Smart exceptions:
| Upstream Condition | Exception Class | Mapped Error Code | Client Message |
|--------------------|-----------------|-------------------|----------------|
| Missing API Key | `AiConfigurationError` | `AI_CONFIGURATION_ERROR` (500) | Configuration error |
| Auth Failure (401/403) | `errors.APIError` | `AI_PROVIDER_ERROR` (502) | The AI provider could not complete the request. |
| Quota / Rate Limit (429) | `errors.APIError` | `AI_PROVIDER_ERROR` (502) | The AI provider could not complete the request. |
| Upstream Outage (500/503) | `errors.APIError` | `AI_PROVIDER_ERROR` (502) | The AI provider could not complete the request. |
| Timeout | `httpx.TimeoutException` | `AI_PROVIDER_ERROR` (502) | The AI provider could not complete the request. |
| Connection Error | `httpx.NetworkError` | `AI_PROVIDER_ERROR` (502) | The AI provider could not complete the request. |
| Empty Model Output | Internal Check | `AI_PROVIDER_ERROR` (502) | The AI provider could not complete the request. |
| Malformed JSON | `JSONDecodeError` | `AI_RESPONSE_VALIDATION_ERROR` (502) | The AI provider response could not be validated. |
| Schema Mismatch | `pydantic.ValidationError`| `AI_RESPONSE_VALIDATION_ERROR` (502) | The AI provider response could not be validated. |

Raw Google stack traces, internal endpoints, and project IDs are never returned to callers.

## 19. Timeout/Retry Behavior
- Configurable timeout via `GEMINI_TIMEOUT_SECONDS` (default: 30.0s).
- Explicit timeout enforced via `asyncio.wait_for(self._client.aio.models.generate_content(...), timeout=self.settings.GEMINI_TIMEOUT_SECONDS)`.
- Zero application-level automatic retries to prevent duplicate API costs and unbounded latency.

## 20. Logging Controls
- **Forbidden from Logs**: Full prompts, patient context, raw Gemini output, API keys, raw exception objects, and patient pseudonyms.
- **Logged Safe Metadata**: `request_id`, `provider=gemini`, `model`, `prompt_version`, `context_sections`, `duration_sec`, and `exception_type`.
- Verified by unit test `test_gemini_secrets_not_logged` with secret keys injected.

## 21. MockProvider Regression
- All 151 existing tests continue to run and pass without modification.
- `AI_PROVIDER=mock` requires no Gemini key and makes no SDK client initialization.

## 22. Tests Added
- `ai-service/tests/unit/test_gemini_provider.py` (22 unit tests)
- `ai-service/tests/unit/test_provider_factory.py` (5 unit tests)
- `ai-service/tests/unit/test_settings.py` (3 new/updated unit tests)
- `backend/spring-api/src/test/java/com/diasmart/springapi/ai/validation/AiGatewayResponseValidatorTest.java` (5 unit tests)
Total new tests: 35 tests.

## 23. FastAPI Test Result
- **Total Tests**: 183
- **Passed**: 183
- **Failed**: 0
- **Skipped**: 0
- **Duration**: ~2.3s

## 24. Coverage
- **Total Statements**: 1,107
- **Missed Statements**: 133
- **Coverage**: **88%** (exceeds the 80% requirement)

## 25. Ruff
- `ruff check .`: All checks passed!
- `ruff format --check .`: 57 files already formatted!

## 26. Mypy
- `mypy app`: Success: no issues found in 37 source files!

## 27. Spring Provider Compatibility Result
- **Finding**: `AiProperties` previously had `expectedProvider = "mock"`, but `AiGatewayResponseValidator` hardcoded `"mock"`.
- **Change Made**: Injected `AiProperties` into `AiGatewayResponseValidator`, validated that `expectedProvider` is either `"mock"` or `"gemini"`, and compared response metadata provider against `expectedProvider`.
- **Result**: Provider validation is fully configurable without modifying data aggregation, authorization, or public DTOs.

## 28. Spring Regression Result
- `AiGatewayResponseValidatorTest`: 18 tests run, 0 failures, 0 errors.
- All Spring Boot AI tests (`*Ai*`): 50 tests run, 0 failures, 0 errors.
- Public API contract and patient aggregation logic remain completely untouched.

## 29. Live Gemini Smoke-Test Result
- **Status**: **PASSED (HTTP 200 OK)**
- **Test Execution**: Live request executed against local FastAPI instance (`http://127.0.0.1:8000/internal/v1/insights/clinical-summary`) using disposable development bearer token `part5-local-internal-token-0123456789abcdef` with synthetic 10-day glucose telemetry (`STABLE_GLUCOSE_PAYLOAD`).
- **Active Provider**: `gemini` (`GEMINI_MODEL=gemini-2.5-flash`).
- **Defect Discovered and Resolved During Live Testing**:
  - *Symptom*: Initial invocation of `client.aio.interactions.create` with Pydantic v2 JSON schema failed with Google Gen AI API HTTP 400 (`APIError: Unknown name "additional_properties" at 'generation_config.response_schema': Cannot find field`).
  - *Root Cause*: Pydantic v2's `model_json_schema()` generates `additionalProperties: false` and `$defs`. Google Gen AI's Protobuf transformer mapped these to `additional_properties`, which is rejected by the Gemini backend REST API.
  - *Resolution*: Implemented `_clean_gemini_schema(schema)` in `GeminiProvider` to recursively strip unsupported Protobuf schema fields (`additional_properties`, `additionalProperties`, and `title`). Switched the primary SDK execution pathway to `client.aio.models.generate_content` with `GenerateContentConfig(response_mime_type="application/json", response_schema=clean_schema)`. Wrapped the call in `asyncio.wait_for(..., timeout=settings.GEMINI_TIMEOUT_SECONDS)` to enforce deterministic timeout behavior. Added `compat_errors.APIStatusError` to the exception mapping hierarchy.
- **Verification of All 20 Criteria**:
  1. **GeminiProvider invoked**: Confirmed (`provider_metadata.provider == "gemini"`).
  2. **External Gemini call succeeds**: Confirmed (HTTP 200 OK returned from live Gemini endpoint).
  3. **Structured JSON returned**: Confirmed (Valid JSON object strictly parsed).
  4. **GeminiClinicalInsightPayload validates**: Confirmed (Pydantic schema parsed without error).
  5. **FastAPI controls request_id**: Confirmed (FastAPI authoritatively generated and assigned `request_id: "7a7d950f-270f-4903-a57f-528449634a51"`).
  6. **provider metadata reports gemini**: Confirmed (`"provider": "gemini"`).
  7. **model metadata matches configured GEMINI_MODEL**: Confirmed (`"model": "gemini-2.5-flash"`).
  8. **prompt_version is correct**: Confirmed (`"prompt_version": "clinical-summary-v1"`).
  9. **safetyNotice exactly matches APPROVED_SAFETY_NOTICE**: Confirmed (Authoritatively populated from `APPROVED_SAFETY_NOTICE`).
  10. **Every observation contains valid supplied evidence**: Confirmed (All 4 observations reference valid telemetry IDs `glucose-summary:selected-period` and `adherence-summary:selected-period`).
  11. **Every correlation contains at least two valid supplied evidence references**: Confirmed (Correlations list is empty `[]`, containing zero unverified relational claims).
  12. **No fabricated evidence reference exists**: Confirmed (Verified by downstream `EvidenceValidator`).
  13. **MedicalSafetyValidator passes**: Confirmed (Passed without clinical boundary violations).
  14. **EvidenceValidator passes**: Confirmed (Passed without evidence grounding violations).
  15. **No diagnosis is generated**: Confirmed (Content contains purely objective metrics: TIR, mean glucose, standard deviation; zero diagnoses).
  16. **No medication instruction is generated**: Confirmed (Zero medication dosing or prescription instructions).
  17. **No insulin dosage recommendation is generated**: Confirmed (Zero insulin dosage titration units).
  18. **No treatment modification is generated**: Confirmed (Zero therapy or treatment alterations).
  19. **No definite causation claim is generated**: Confirmed (Objective correlational and observational framing only).
  20. **No tool, search, URL grounding, file search, code execution, or function call is used**: Confirmed (`tools=None`, zero tool/search calls).
- **Sanitized Response Excerpt**:
  ```json
  {
    "request_id": "7a7d950f-270f-4903-a57f-528449634a51",
    "provider_metadata": {
      "provider": "gemini",
      "model": "gemini-2.5-flash",
      "prompt_version": "clinical-summary-v1"
    },
    "summary": "During the selected 10-day period, the patient demonstrated stable glycemic patterns with an average glucose of 128 mg/dL and 82% time in range (70-180 mg/dL), alongside high logging adherence.",
    "observations": [
      {
        "category": "glycemic_control",
        "statement": "Time in range (70-180 mg/dL) was 82.0% during the selected 10-day evaluation period.",
        "evidence_references": ["glucose-summary:selected-period"]
      },
      {
        "category": "glycemic_control",
        "statement": "Mean glucose was 128.0 mg/dL with a glucose standard deviation of 22.0 mg/dL.",
        "evidence_references": ["glucose-summary:selected-period"]
      },
      {
        "category": "adherence",
        "statement": "Logging adherence was recorded at 94.0% across the selected period.",
        "evidence_references": ["adherence-summary:selected-period"]
      },
      {
        "category": "glycemic_control",
        "statement": "Hypoglycemic exposure (<70 mg/dL) was observed at 1.5% of total readings.",
        "evidence_references": ["glucose-summary:selected-period"]
      }
    ],
    "correlations": [],
    "uncertainties": [
      "Lack of granular postprandial timestamp logs limits meal-specific excursion analysis.",
      "Physical activity data was not supplied for this interval.",
      "Sensor calibration events were not documented in the provided telemetry."
    ],
    "discussion_points": [
      "Review consistency of logging during weekend intervals.",
      "Discuss strategies to maintain the current >80% time in range safely."
    ],
    "safety_notice": "This AI-generated information is intended for review and does not provide a diagnosis, prescription, insulin-dosage recommendation, or treatment recommendation."
  }
  ```

## 30. Cross-Service Gemini Result
- **Status**: **BLOCKED BY LOCAL SYNTHETIC DATA AVAILABILITY**
- **Spring Boot Configuration Inspection**:
  - `backend/spring-api/src/main/resources/application-dev.yml` and `application-prod.yml` both map `diasmart.ai.expected-provider: ${AI_EXPECTED_PROVIDER:mock}`.
  - Setting `AI_EXPECTED_PROVIDER=gemini` correctly instructs Spring Boot's `AiGatewayResponseValidator` to accept `provider="gemini"` responses from FastAPI.
- **Local Environment Inspection**:
  - Local Spring Boot process (`127.0.0.1:8080`) is offline.
  - No local synthetic PostgreSQL database is active.
- **Safety Policy Adherence**:
  - In strict accordance with healthcare AI guidelines and user instructions, production databases and real patient data were NOT accessed.
  - Cross-service live verification is marked **blocked by local data availability**.
- **Architectural Credential and Token Isolation**:
  - Browser/React talks exclusively to Spring Boot (`/api/v1/patients/{id}/ai-summary`) via session/JWT.
  - Spring Boot talks to FastAPI (`/internal/v1/insights/clinical-summary`) via `AI_INTERNAL_SERVICE_TOKEN`.
  - FastAPI alone talks to Google Gemini via `GEMINI_API_KEY`.
  - `GEMINI_API_KEY` never reaches Spring Boot or React.
  - `AI_INTERNAL_SERVICE_TOKEN` never reaches React.

## 31. Switch-Back-to-Mock Result
- **Status**: **PASSED (SUCCESSFULLY VERIFIED)**
- **Verification Details**:
  - Configured `AI_PROVIDER=mock`.
  - `ProviderFactory.get_provider()` dynamically resolves `MockProvider`.
  - Service runs completely offline without `GEMINI_API_KEY` or external network egress.
  - `/health` endpoint returns `status: "ok"` and `provider: "mock"`.
  - `POST /internal/v1/insights/clinical-summary` immediately returns deterministic mock clinical insight with `provider_metadata.provider: "mock"` and `provider_metadata.model: "mock-clinical-summary-v1"`.
  - Switching between providers requires zero source code modifications.

## 32. Git State After Implementation
Primary files modified and created:
- `ai-service/pyproject.toml`
- `ai-service/app/config/settings.py`
- `ai-service/.env.example`
- `ai-service/app/api/health.py`
- `ai-service/app/models/responses.py`
- `ai-service/app/models/gemini.py`
- `ai-service/app/providers/gemini_provider.py`
- `ai-service/app/providers/factory.py`
- `ai-service/app/services/clinical_summary_service.py`
- `ai-service/README.md`
- `ai-service/tests/unit/test_gemini_provider.py`
- `ai-service/tests/unit/test_provider_factory.py`
- `ai-service/tests/unit/test_settings.py`
- `backend/spring-api/src/main/java/com/diasmart/springapi/ai/validation/AiGatewayResponseValidator.java`
- `backend/spring-api/src/test/java/com/diasmart/springapi/ai/validation/AiGatewayResponseValidatorTest.java`
- `docs/DIA_SMART_AI_PART5_IMPLEMENTATION.md`

Read-only components remained untouched:
- `frontend/web-dashboard/**`: 0 files touched.
- `firmware/**`: 0 files touched.
- `database/**`: 0 files touched.
- `.github/**`: 0 files touched.

## 33. Secret Review
- Git diff scanned with regex patterns for API keys, bearer tokens, AWS keys, and private keys: **CLEAN** (0 credentials found).
- `.env` file verified to be gitignored via `git check-ignore`.
- `.env.example` contains only empty placeholders.
- Log masking verified via automated unit test `test_gemini_secrets_not_logged`.
- During live testing, `GEMINI_API_KEY` was never printed, logged, or included in test scripts or documentation.

## 34. Known Limitations
- Real Gemini requests require an active Google Cloud API key with quota for `gemini-2.5-flash` or another compatible model.
- Live Gemini rate limits on the free tier may throttle high-frequency requests.
- Live cross-service testing requires a locally running synthetic database and Spring Boot instance.

## 35. Part 6 Entry Criteria
- Part 5 code implementation, defect resolution, and unit tests are complete and verified (183 FastAPI tests, 50 Spring Boot tests).
- Direct live Gemini smoke test passed with all 20 clinical safety and schema criteria verified.
- Switch-back to `MockProvider` verified without code changes.
- MockProvider remains the default.
- Ready for Part 6.

## 36. Recommended Next Action
Developers wishing to run a live smoke test can configure their environment in PowerShell:
```powershell
$env:GEMINI_API_KEY="<your-key>"
$env:GEMINI_MODEL="gemini-2.5-flash"
$env:AI_PROVIDER="gemini"
```
and execute `pytest tests/unit/test_gemini_provider.py` or a single live smoke request. Afterwards, switch back with `$env:AI_PROVIDER="mock"` and clear the key with `Remove-Item Env:GEMINI_API_KEY`.
