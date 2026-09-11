import asyncio
import copy
import json
import logging
from unittest.mock import AsyncMock, MagicMock

import httpx
import pytest
from google.genai import errors

from app.config.settings import Settings
from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.exceptions.types import (
    AiConfigurationError,
    AiEvidenceValidationError,
    AiMedicalSafetyRejectionError,
    AiProviderError,
    AiResponseValidationError,
)
from app.models.requests import ClinicalSummaryRequest
from app.providers.gemini_provider import GeminiProvider
from app.services.clinical_summary_service import ClinicalSummaryService
from tests.fixtures.clinical_contexts import (
    INJECTION_DESCRIPTION_PAYLOAD,
    STABLE_GLUCOSE_PAYLOAD,
)

VALID_GEMINI_INSIGHT_PAYLOAD = {
    "summary": "Patient glucose records show stable readings within target range during the requested period.",
    "observations": [
        {
            "statement": "Recorded 30 glucose readings with mean value 110 mg/dL.",
            "evidence_references": ["glucose-summary:selected-period"],
        }
    ],
    "correlations": [
        {
            "statement": "Consistent insulin administration co-occurs with glycemic stability across the monitoring interval.",
            "confidence": "moderate",
            "evidence_references": [
                "glucose-summary:selected-period",
                "adherence-summary:selected-period",
            ],
        }
    ],
    "uncertainties": ["Continuous intraday glucose variability telemetry is not available."],
    "discussion_points": ["Review nutrition timing consistency during regular consultation."],
}


def _make_gemini_settings() -> Settings:
    return Settings(
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
        GEMINI_API_KEY="test-secret-key-1234567890",
        GEMINI_MODEL="gemini-2.5-flash",
        GEMINI_TIMEOUT_SECONDS=15.0,
        GEMINI_TEMPERATURE=0.2,
    )


def _make_mock_client(output_text: str | None = None) -> MagicMock:
    client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = output_text if output_text is not None else json.dumps(VALID_GEMINI_INSIGHT_PAYLOAD)
    client.aio.models.generate_content = AsyncMock(return_value=mock_response)
    return client


def test_successful_gemini_summary():
    settings = _make_gemini_settings()
    client = _make_mock_client()
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    res = asyncio.run(provider.generate_clinical_summary(req))

    # Verify client call parameters
    client.aio.models.generate_content.assert_awaited_once()
    _, kwargs = client.aio.models.generate_content.call_args
    assert kwargs["model"] == "gemini-2.5-flash"
    assert kwargs["contents"] is not None
    config = kwargs["config"]
    assert config.response_mime_type == "application/json"
    assert "properties" in config.response_schema
    assert config.temperature == 0.2
    assert config.tools is None

    # Verify server-controlled fields
    assert res.request_id == req.request_id
    assert res.provider_metadata.provider == "gemini"
    assert res.provider_metadata.model == "gemini-2.5-flash"
    assert res.provider_metadata.prompt_version == req.prompt_version
    assert res.safety_notice == APPROVED_SAFETY_NOTICE

    # Verify payload content
    assert res.summary == VALID_GEMINI_INSIGHT_PAYLOAD["summary"]
    assert len(res.observations) == 1
    assert res.observations[0].statement == VALID_GEMINI_INSIGHT_PAYLOAD["observations"][0]["statement"]
    assert len(res.correlations) == 1
    assert res.correlations[0].confidence == "moderate"
    assert len(res.uncertainties) == 1
    assert len(res.discussion_points) == 1


def test_no_tools_configured():
    settings = _make_gemini_settings()
    client = _make_mock_client()
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    asyncio.run(provider.generate_clinical_summary(req))

    _, kwargs = client.aio.models.generate_content.call_args
    assert kwargs["config"].tools is None


def test_missing_api_key_configuration():
    settings = Settings.model_construct(
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
        GEMINI_API_KEY=None,
        GEMINI_MODEL="gemini-2.5-flash",
    )
    with pytest.raises(AiConfigurationError, match="GEMINI_API_KEY is not configured"):
        GeminiProvider(settings=settings)


def test_missing_model_configuration():
    settings = Settings.model_construct(
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
        GEMINI_API_KEY="test-key",
        GEMINI_MODEL=None,
    )
    with pytest.raises(AiConfigurationError, match="GEMINI_MODEL is not configured"):
        GeminiProvider(settings=settings)


def test_gemini_api_auth_error_401():
    settings = _make_gemini_settings()
    client = MagicMock()
    api_err = errors.APIError(401, {"error": {"message": "Unauthenticated"}})
    client.aio.models.generate_content = AsyncMock(side_effect=api_err)
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="authentication or permission failed"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_api_permission_error_403():
    settings = _make_gemini_settings()
    client = MagicMock()
    api_err = errors.APIError(403, {"error": {"message": "Permission Denied"}})
    client.aio.models.generate_content = AsyncMock(side_effect=api_err)
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="authentication or permission failed"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_api_rate_limit_429():
    settings = _make_gemini_settings()
    client = MagicMock()
    api_err = errors.APIError(429, {"error": {"message": "Quota exceeded"}})
    client.aio.models.generate_content = AsyncMock(side_effect=api_err)
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="rate limit was exceeded"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_api_service_unavailable_503():
    settings = _make_gemini_settings()
    client = MagicMock()
    api_err = errors.APIError(503, {"error": {"message": "Service Unavailable"}})
    client.aio.models.generate_content = AsyncMock(side_effect=api_err)
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="service is currently unavailable"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_timeout_handling():
    settings = _make_gemini_settings()
    client = MagicMock()
    client.aio.models.generate_content = AsyncMock(side_effect=httpx.TimeoutException("Read timed out"))
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="request timed out"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_network_connection_error():
    settings = _make_gemini_settings()
    client = MagicMock()
    client.aio.models.generate_content = AsyncMock(side_effect=httpx.NetworkError("Connection refused"))
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="Failed to connect"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_empty_output():
    settings = _make_gemini_settings()
    client = _make_mock_client(output_text="")
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="returned empty output"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_blocked_by_safety_filter():
    settings = _make_gemini_settings()
    client = MagicMock()
    mock_response = MagicMock()
    mock_response.text = None
    mock_candidate = MagicMock()
    mock_candidate.finish_reason = "SAFETY"
    mock_response.candidates = [mock_candidate]
    client.aio.models.generate_content = AsyncMock(return_value=mock_response)
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="blocked by safety filters"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_malformed_json_rejected():
    settings = _make_gemini_settings()
    client = _make_mock_client(output_text="Here is the clinical insight: {bad-json")
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiResponseValidationError, match="not valid JSON"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_missing_required_fields_rejected():
    settings = _make_gemini_settings()
    # Missing 'uncertainties'
    bad_payload = {
        "summary": "Summary text",
        "observations": [],
        "correlations": [],
        "discussion_points": [],
    }
    client = _make_mock_client(output_text=json.dumps(bad_payload))
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiResponseValidationError, match="did not match the expected schema"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_unknown_forbidden_fields_rejected():
    settings = _make_gemini_settings()
    bad_payload = copy.deepcopy(VALID_GEMINI_INSIGHT_PAYLOAD)
    bad_payload["unauthorized_field"] = "malicious_injection"  # extra='forbid'
    client = _make_mock_client(output_text=json.dumps(bad_payload))
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiResponseValidationError, match="did not match the expected schema"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_invalid_confidence_rejected():
    settings = _make_gemini_settings()
    bad_payload = copy.deepcopy(VALID_GEMINI_INSIGHT_PAYLOAD)
    bad_payload["correlations"][0]["confidence"] = "ultra-high"  # invalid Literal
    client = _make_mock_client(output_text=json.dumps(bad_payload))
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiResponseValidationError, match="did not match the expected schema"):
        asyncio.run(provider.generate_clinical_summary(req))


def test_gemini_pipeline_rejects_invented_evidence():
    settings = _make_gemini_settings()
    bad_payload = copy.deepcopy(VALID_GEMINI_INSIGHT_PAYLOAD)
    bad_payload["observations"][0]["evidence_references"] = ["glucose-summary:fabricated-ref"]
    client = _make_mock_client(output_text=json.dumps(bad_payload))
    provider = GeminiProvider(settings=settings, client=client)

    service = ClinicalSummaryService()
    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)

    with pytest.MonkeyPatch.context() as mp:
        mp.setattr("app.services.clinical_summary_service.get_provider", lambda: provider)
        with pytest.raises(AiEvidenceValidationError):
            asyncio.run(service.generate_summary(req))


def test_gemini_pipeline_rejects_diagnosis():
    settings = _make_gemini_settings()
    bad_payload = copy.deepcopy(VALID_GEMINI_INSIGHT_PAYLOAD)
    bad_payload["summary"] = "The patient is diagnosed with uncontrolled type 1 diabetes."
    client = _make_mock_client(output_text=json.dumps(bad_payload))
    provider = GeminiProvider(settings=settings, client=client)

    service = ClinicalSummaryService()
    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)

    with pytest.MonkeyPatch.context() as mp:
        mp.setattr("app.services.clinical_summary_service.get_provider", lambda: provider)
        with pytest.raises(AiMedicalSafetyRejectionError):
            asyncio.run(service.generate_summary(req))


def test_gemini_pipeline_rejects_dosage_recommendation():
    settings = _make_gemini_settings()
    bad_payload = copy.deepcopy(VALID_GEMINI_INSIGHT_PAYLOAD)
    bad_payload["observations"][0]["statement"] = "Patient instructed to take 4 units."
    client = _make_mock_client(output_text=json.dumps(bad_payload))
    provider = GeminiProvider(settings=settings, client=client)

    service = ClinicalSummaryService()
    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)

    with pytest.MonkeyPatch.context() as mp:
        mp.setattr("app.services.clinical_summary_service.get_provider", lambda: provider)
        with pytest.raises(AiMedicalSafetyRejectionError):
            asyncio.run(service.generate_summary(req))


def test_gemini_pipeline_rejects_causation_claims():
    settings = _make_gemini_settings()
    bad_payload = copy.deepcopy(VALID_GEMINI_INSIGHT_PAYLOAD)
    bad_payload["correlations"][0]["statement"] = "Storage conditions caused the glucose spike."
    client = _make_mock_client(output_text=json.dumps(bad_payload))
    provider = GeminiProvider(settings=settings, client=client)

    service = ClinicalSummaryService()
    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)

    with pytest.MonkeyPatch.context() as mp:
        mp.setattr("app.services.clinical_summary_service.get_provider", lambda: provider)
        with pytest.raises(AiMedicalSafetyRejectionError):
            asyncio.run(service.generate_summary(req))


def test_gemini_secrets_not_logged(caplog):
    secret_key = "super-secret-gemini-key-xyz-999"
    settings = Settings(
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
        GEMINI_API_KEY=secret_key,
        GEMINI_MODEL="gemini-2.5-flash",
    )
    client = MagicMock()
    client.aio.models.generate_content = AsyncMock(side_effect=errors.APIError(500, {"error": {"message": f"Failed with key {secret_key}"}}))
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with caplog.at_level(logging.DEBUG):
        with pytest.raises(AiProviderError):
            asyncio.run(provider.generate_clinical_summary(req))

    for record in caplog.records:
        assert secret_key not in record.message
        assert "super-secret" not in record.message


def test_gemini_prompt_injection_boundary_isolation():
    settings = _make_gemini_settings()
    client = _make_mock_client()
    provider = GeminiProvider(settings=settings, client=client)

    req = ClinicalSummaryRequest.model_validate(INJECTION_DESCRIPTION_PAYLOAD)
    res = asyncio.run(provider.generate_clinical_summary(req))

    # Confirm boundary markers were added in prompt input sent to SDK
    _, kwargs = client.aio.models.generate_content.call_args
    assert "[UNTRUSTED_USER_CONTENT_START]" in kwargs["contents"]
    assert "[UNTRUSTED_USER_CONTENT_END]" in kwargs["contents"]
    assert res.provider_metadata.provider == "gemini"


def test_successful_gemini_models_generate_content():
    from google import genai

    settings = _make_gemini_settings()
    client = MagicMock(spec=genai.Client)
    client.aio = MagicMock()
    mock_response = MagicMock()
    mock_response.text = json.dumps(VALID_GEMINI_INSIGHT_PAYLOAD)
    client.aio.models.generate_content = AsyncMock(return_value=mock_response)

    provider = GeminiProvider(settings=settings, client=client)
    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    res = asyncio.run(provider.generate_clinical_summary(req))

    client.aio.models.generate_content.assert_awaited_once()
    assert res.provider_metadata.provider == "gemini"
    assert res.provider_metadata.model == "gemini-2.5-flash"
    assert res.summary == VALID_GEMINI_INSIGHT_PAYLOAD["summary"]
    assert res.safety_notice == APPROVED_SAFETY_NOTICE


def test_gemini_compat_api_status_error_mapping():
    try:
        from google.genai._gaos.lib.compat_errors import APIStatusError
    except ImportError:
        pytest.skip("compat_errors not available")

    settings = _make_gemini_settings()
    client = MagicMock()
    dummy_resp = MagicMock()
    err = APIStatusError("Quota exceeded", response=dummy_resp, body={"error": "details"})
    err.status_code = 429
    client.aio.models.generate_content = AsyncMock(side_effect=err)

    provider = GeminiProvider(settings=settings, client=client)
    req = ClinicalSummaryRequest.model_validate(STABLE_GLUCOSE_PAYLOAD)
    with pytest.raises(AiProviderError, match="rate limit was exceeded"):
        asyncio.run(provider.generate_clinical_summary(req))
