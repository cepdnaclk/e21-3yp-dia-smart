import copy
from unittest.mock import AsyncMock, patch
from uuid import uuid4

from fastapi.testclient import TestClient

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.main import app
from app.models.responses import ClinicalSummaryResponse, Observation, ProviderMetadata
from tests.fixtures.clinical_contexts import STABLE_GLUCOSE_PAYLOAD

client = TestClient(app)

VALID_TOKEN = "test-handshake-token-of-at-least-32-characters-long"
HEADERS = {"Authorization": f"Bearer {VALID_TOKEN}"}
URL = "/internal/v1/insights/clinical-summary"


# 1. Request Size Limit Enforcement Tests
def test_request_body_size_limit_exceeded():
    # Construct a payload that exceeds 1 MiB (default limit)
    copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    # Fill description in a selected event with a very long string (> 1MB)
    # Note: request validation checks maximum text length (AI_MAX_TEXT_LENGTH = 1000)
    # So if we want to bypass pydantic validation and hit ASGI size limit, we can just pad the JSON payload with unused spaces/keys,
    # or send a huge raw string that ASGI reads before Pydantic parsing!
    # Let's send a raw payload with large whitespace formatting
    payload_str = " " * (1024 * 1024 + 100)  # > 1 MiB of whitespaces

    # POST the oversized request to the endpoint
    res = client.post(
        URL,
        content=payload_str,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(payload_str)),
        },
    )
    assert res.status_code == 413

    data = res.json()
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"
    assert "exceeds the maximum allowed limit" in data["message"]


# 2. Request-ID Correlation Mismatch Rejection Tests
@patch("app.services.clinical_summary_service.get_provider")
def test_request_id_correlation_mismatch(mock_get_provider):
    stable_req = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)

    # Mock the provider to return a response with a mismatched request_id
    mismatched_response = ClinicalSummaryResponse(
        request_id=uuid4(),  # Different UUID
        summary="A safe summary description",
        observations=[
            Observation(
                statement="Glucose average was normal.",
                evidence_references=["glucose-summary:selected-period"],
            )
        ],
        correlations=[],
        uncertainties=["Telemetry is limited."],
        discussion_points=["Contact your doctor."],
        safety_notice=APPROVED_SAFETY_NOTICE,
        provider_metadata=ProviderMetadata(provider="mock", model="mock-clinical-summary-v1", prompt_version="clinical-summary-v1"),
    )

    mock_provider = AsyncMock()
    mock_provider.generate_clinical_summary.return_value = mismatched_response
    mock_get_provider.return_value = mock_provider

    res = client.post(URL, json=stable_req, headers=HEADERS)
    assert res.status_code == 502

    data = res.json()
    assert data["error_code"] == "AI_RESPONSE_VALIDATION_ERROR"
    assert "response could not be validated" in data["message"]


# 3. Error Masking Behavior Tests
@patch("app.services.clinical_summary_service.get_provider")
def test_raw_provider_exception_masked(mock_get_provider):
    mock_provider = AsyncMock()
    # Raise a runtime exception containing sensitive URL, local path, and stack traces
    mock_provider.generate_clinical_summary.side_effect = RuntimeError(
        "Exception: connection failed to http://gemini.googleapis.com/v1/models/gemini-pro. File: C:\\Users\\secret\\key.txt"
    )
    mock_get_provider.return_value = mock_provider

    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=HEADERS)
    assert res.status_code == 502

    data = res.json()
    assert data["error_code"] == "AI_PROVIDER_ERROR"
    # Ensure raw exception text, paths, and URLs are masked and not leaked
    assert "gemini.googleapis.com" not in data["message"]
    assert "C:\\Users\\secret" not in data["message"]
    assert "The AI provider could not complete the request." in data["message"]
