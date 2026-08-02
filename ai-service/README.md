# Dia-Smart AI Service

Dia-Smart AI is a microservice designed to provide a safe, testable foundation for the future Dia-Smart AI-assisted clinical-summary subsystem. It handles structured patient telemetry analysis and generates clinical summaries with structured evidence citations.

> [!WARNING]
> **Dia-Smart AI generates informational and decision-support content from supplied records. It does not diagnose medical conditions, prescribe medication, recommend insulin dosage, or replace professional medical judgment.**

---

## 1. Service Purpose & Part 2 Scope
This service functions as an isolated gateway. In Part 2, the implementation uses only a deterministic **MockProvider** for response generation. 

* **No Real LLM Integration:** There is no integration with Google Gemini, OpenAI, or other external APIs yet.
* **No Gemini SDK Installed:** The Google Gemini SDK (`google-genai` / `google-generativeai`) is not installed or used.
* **No External Network Access:** The service operates completely offline.
* **No Database Dependencies:** Telemetry context is supplied entirely inside requests, avoiding database access.

---

## 2. Service Architecture
The service validates incoming patient payloads, runs them through versioned prompt formatting, simulates the model using a deterministic mock provider, runs post-generation medical safety audits, and verifies returned source citations before sending the output back to the caller.

```text
Synthetic telemetry context
            ↓
Request-schema validation (Pydantic requests model)
            ↓
Bearer-token authentication check
            ↓
Prompt formatting (app/prompts/prompt_builder.py)
            ↓
MockProvider execution (app/providers/mock_provider.py)
            ↓
Response-schema validation (Pydantic responses model)
            ↓
Medical-safety validation (app/validators/medical_safety_validator.py)
            ↓
Evidence-reference validation (app/validators/evidence_validator.py)
            ↓
Validated structured clinical summary returned
```

---

## 3. Folder Structure
```text
ai-service/
├── app/
│   ├── api/                  # API endpoints (health, clinical-summary)
│   ├── config/               # Settings loading (settings.py)
│   ├── constants/            # Safety disclaimers and category lists
│   ├── exceptions/           # Controlled custom exceptions & handlers
│   ├── models/               # Pydantic schema validation models
│   ├── prompts/              # System prompts & builders
│   ├── providers/            # AI engines (Protocol and MockProvider)
│   ├── security/             # Bearer authentication mechanics
│   ├── services/             # Coordinate pipeline execution
│   ├── validators/           # Post-run safety and citation validators
│   └── main.py               # Main FastAPI bootstrapper
├── tests/                    # Unit and integration test suites
├── Dockerfile                # Production container spec
├── .dockerignore             # Docker context ignore patterns
├── .gitignore                # Git ignore configurations
├── .env.example              # Development configuration template
├── pyproject.toml            # Project configurations and lints
├── pytest.ini                # Pytest runner variables
└── README.md                 # [This documentation]
```

---

## 4. Prerequisites & Environment Setup

### Prerequisites
* Python `3.11` or `3.12` (compatible up to Python `3.14` active on local environments)
* virtualenv / venv

### Virtual Environment Setup
Run these commands from inside the `ai-service` directory:
```bash
# Create the virtual environment
python -m venv .venv

# Activate the virtual environment (PowerShell Windows)
.venv\Scripts\Activate.ps1

# Upgrade pip
.venv\Scripts\python -m pip install --upgrade pip

# Install project and development dependencies in editable mode
.venv\Scripts\python -m pip install -e .[dev]
```

---

## 5. Configuration & MockProvider
Copy the example environment file to `.env`:
```bash
cp .env.example .env
```
Ensure `AI_PROVIDER=mock` is configured. If any other provider value is configured, the factory will raise a configuration exception. A valid, long `AI_INTERNAL_SERVICE_TOKEN` (at least 32 characters) must be configured in `.env` for local execution.

---

## 6. Running Locally

Start the development server with:
```bash
.venv\Scripts\uvicorn app.main:app --host 0.0.0.0 --port 8000
```
Swagger UI will be available at `http://localhost:8000/docs` for inspection.

---

## 7. Endpoint Usage & Authentication

### Health Endpoint
* **Route:** `GET /health`
* **Auth:** Unauthenticated
* **Query:**
```bash
curl -X GET http://localhost:8000/health
```
* **Success Response (200 OK):**
```json
{
  "status": "ok",
  "service": "Dia-Smart AI Service",
  "version": "0.1.0",
  "provider": "mock",
  "prompt_version": "clinical-summary-v1"
}
```

### Clinical Summary Endpoint
* **Route:** `POST /internal/v1/insights/clinical-summary`
* **Auth:** Bearer Token handshake via header: `Authorization: Bearer <AI_INTERNAL_SERVICE_TOKEN>`
* **Example Payload:**
```json
{
  "request_id": "7a7d950f-270f-4903-a57f-528449634a51",
  "request_type": "CLINICAL_SUMMARY",
  "prompt_version": "clinical-summary-v1",
  "patient_reference": "patient-ref-7e05bb",
  "requested_period": {
    "from": "2026-07-01T00:00:00Z",
    "to": "2026-07-31T23:59:59Z"
  },
  "glucose_summary": {
    "evidence_reference": "glucose-summary:selected-period",
    "unit": "mg/dL",
    "reading_count": 84,
    "average": 142.7,
    "minimum": 82.0,
    "maximum": 231.0,
    "high_reading_count": 7,
    "low_reading_count": 1
  },
  "adherence_summary": {
    "evidence_reference": "adherence-summary:selected-period",
    "scheduled_administrations": 62,
    "recorded_administrations": 59,
    "delayed_administrations": 4,
    "missed_administrations": 3
  }
}
```
* **Example Response (200 OK):**
```json
{
  "request_id": "7a7d950f-270f-4903-a57f-528449634a51",
  "summary": "The selected period contains glucose, administration, storage, inventory, and alert information suitable for review.",
  "observations": [
    {
      "statement": "A total of 84 glucose readings were processed with an average value of 142.7 mg/dL.",
      "evidence_references": ["glucose-summary:selected-period"]
    },
    {
      "statement": "7 glucose readings were recorded above the configured high threshold.",
      "evidence_references": ["glucose-summary:selected-period"]
    }
  ],
  "correlations": [
    {
      "statement": "The selected period contains both elevated glucose readings and delayed recorded administrations. This co-occurrence does not establish causation.",
      "confidence": "moderate",
      "evidence_references": ["glucose-summary:selected-period", "adherence-summary:selected-period"]
    }
  ],
  "uncertainties": [
    "The supplied records are not sufficient to determine the medical cause of the observed readings.",
    "Telemetry does not capture external context such as patient diet, stress, physical exercise, or device calibration issues."
  ],
  "discussion_points": [
    "A healthcare professional may review the recorded timing of elevated readings and delayed administrations.",
    "Discuss regular tracking habits and check if device sync schedules are operating correctly."
  ],
  "safety_notice": "This AI-generated information is intended for review and does not provide a diagnosis, prescription, insulin-dosage recommendation, or treatment recommendation.",
  "provider_metadata": {
    "provider": "mock",
    "model": "mock-clinical-summary-v1",
    "prompt_version": "clinical-summary-v1"
  }
}
```
* **Example Unauthorized Response (401 Unauthorized):**
```json
{
  "error_code": "AI_UNAUTHORIZED",
  "message": "Authorization header is missing or empty",
  "request_id": null
}
```

---

## 8. Verification & Quality Commands

Ensure your virtual environment is active before running these quality checks:

### Run Tests
```bash
.venv\Scripts\pytest
```

### Run Coverage Report
```bash
.venv\Scripts\pytest --cov=app --cov-report=term-missing
```

### Static Analysis (Ruff & mypy)
```bash
# Run Ruff lint check
.venv\Scripts\ruff check .

# Run Ruff formatting check
.venv\Scripts\ruff format --check .

# Run mypy type checking
.venv\Scripts\mypy app tests
```

---

## 9. Docker Deployment

### Build the Image
```bash
docker build -t diasmart-ai-service:latest .
```

### Run the Container
```bash
docker run -d -p 8000:8000 \
  -e AI_INTERNAL_SERVICE_TOKEN=your-random-production-token-of-at-least-32-chars \
  --name diasmart-ai \
  diasmart-ai-service:latest
```

---

## 10. Safety Controls & Validation Filters (Part 2 Correction Pass)
* **Maximum Request Body Size Limit:** Enforces `AI_MAX_REQUEST_BODY_BYTES` (default 1 MiB) early in the ASGI request stream, rejecting oversized payloads with `413 Payload Too Large` and a controlled `AI_REQUEST_TOO_LARGE` JSON body before memory exhaustion.
* **String Length & Whitespace Constraints:** Limits string properties (e.g., `patient_reference`: 8-128 chars; `unit`: 1-32; `evidence_reference`: <=128; others: 1-64) and rejects empty/whitespace-only strings.
* **Pseudonymous Patient Reference Check:** Pattern `^[A-Za-z][A-Za-z0-9._:-]{7,127}$` filters out raw database IDs (e.g. `patient-1`, `user_234`) and email formats.
* **Telemetry Data Consistency Rules:** Model-level validation ensures mathematical rules (e.g., `minimum <= average <= maximum` for glucose and storage, and logical reading/administration counts bounds).
* **Time Period Boundaries Alignment:** Rejects requests if any associated alert or selected event falls outside `requested_period.from` to `requested_period.to`.
* **Request-ID Correlation Validator:** Asserts `response.request_id == request.request_id` in the pipeline and returns a controlled `AI_RESPONSE_VALIDATION_ERROR` (502 status) on mismatch.
* **Medical Safety Regex Checks:** Checks all response blocks against expanded phrase lists (diagnoses, dose recommendations, prescription changes, stop-medication instructions, causation claims, doctor impersonation) after case, space, and punctuation normalization.
* **Secure Error Sanitization:** Exception handlers mask raw exception details, filesystems, URLs, and stack traces from API callers, returning safe, controlled error messages.
* **Context-Aware Mock rendering:** MockProvider dynamically generates observations based only on present context blocks, handles sparse records (e.g., 1 reading), and verifies correlation dependencies.

---

## 11. Troubleshooting
* **NameError: name 're' is not defined:** Ensure you are using the updated `app/models/requests.py` which contains `import re` at the top of the module.
* **401 errors on correct tokens:** Ensure your `.env` is loaded by uvicorn or specify environment variables directly on execution.
