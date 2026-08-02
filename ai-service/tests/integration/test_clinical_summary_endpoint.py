import copy
from unittest.mock import AsyncMock, patch
from uuid import UUID

from fastapi.testclient import TestClient

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.main import app
from app.models.responses import ClinicalSummaryResponse, Observation, ProviderMetadata
from tests.fixtures.clinical_contexts import (
    STABLE_GLUCOSE_PAYLOAD,
)

client = TestClient(app)

VALID_TOKEN = "test-handshake-token-of-at-least-32-characters-long"
HEADERS = {"Authorization": f"Bearer {VALID_TOKEN}"}
URL = "/internal/v1/insights/clinical-summary"


def test_successful_clinical_summary():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=HEADERS)
    assert res.status_code == 200

    data = res.json()
    assert "summary" in data
    assert len(data["observations"]) > 0
    assert data["safety_notice"] == APPROVED_SAFETY_NOTICE
    assert data["provider_metadata"]["provider"] == "mock"


def test_invalid_request_body_422():
    invalid_payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    invalid_payload["prompt_version"] = "unsupported-version-v2"

    res = client.post(URL, json=invalid_payload, headers=HEADERS)
    assert res.status_code == 422

    data = res.json()
    assert data["error_code"] == "AI_REQUEST_VALIDATION_ERROR"
    assert "prompt_version" in data["message"]


@patch("app.services.clinical_summary_service.get_provider")
def test_provider_execution_error_502(mock_get_provider):
    mock_provider = AsyncMock()
    mock_provider.generate_clinical_summary.side_effect = Exception("Internal provider fail")
    mock_get_provider.return_value = mock_provider

    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=HEADERS)
    assert res.status_code == 502

    data = res.json()
    assert data["error_code"] == "AI_PROVIDER_ERROR"
    assert "The AI provider could not complete the request." in data["message"]


@patch("app.services.clinical_summary_service.get_provider")
def test_medical_safety_rejection_502(mock_get_provider):
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    # Mock the provider to return unsafe text that violates safety filters
    unsafe_response = ClinicalSummaryResponse(
        request_id=UUID(stable["request_id"]),
        summary="The patient has diabetic ketoacidosis. Increase the insulin dose to 12 units.",
        observations=[
            Observation(
                statement="High values were recorded.",
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
    mock_provider.generate_clinical_summary.return_value = unsafe_response
    mock_get_provider.return_value = mock_provider

    res = client.post(URL, json=stable, headers=HEADERS)
    assert res.status_code == 502

    data = res.json()
    assert data["error_code"] == "AI_MEDICAL_SAFETY_REJECTION"
    assert "did not pass medical safety checks" in data["message"]


@patch("app.services.clinical_summary_service.get_provider")
def test_evidence_validation_rejection_502(mock_get_provider):
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    # Mock response containing invented evidence references not in stable payload request
    invalid_evidence_response = ClinicalSummaryResponse(
        request_id=UUID(stable["request_id"]),
        summary="Safe summary description",
        observations=[
            Observation(
                statement="Glucose was normal.",
                evidence_references=["glucose-summary:invented-reference-not-present-in-req"],
            )
        ],
        correlations=[],
        uncertainties=["Telemetry is limited."],
        discussion_points=["Contact your doctor."],
        safety_notice=APPROVED_SAFETY_NOTICE,
        provider_metadata=ProviderMetadata(provider="mock", model="mock-clinical-summary-v1", prompt_version="clinical-summary-v1"),
    )

    mock_provider = AsyncMock()
    mock_provider.generate_clinical_summary.return_value = invalid_evidence_response
    mock_get_provider.return_value = mock_provider

    res = client.post(URL, json=stable, headers=HEADERS)
    assert res.status_code == 502

    data = res.json()
    assert data["error_code"] == "AI_EVIDENCE_VALIDATION_ERROR"
    assert "contains invalid citations" in data["message"]
