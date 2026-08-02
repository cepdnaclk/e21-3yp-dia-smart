import asyncio
import copy

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.requests import ClinicalSummaryRequest
from app.providers.mock_provider import MockProvider
from tests.fixtures.clinical_contexts import (
    ELEVATED_GLUCOSE_DELAYED_DOSE_PAYLOAD,
    INJECTION_DESCRIPTION_PAYLOAD,
    INSUFFICIENT_CONTEXT_PAYLOAD,
    STABLE_GLUCOSE_PAYLOAD,
)


def test_deterministic_summary_stable():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert res.request_id == req.request_id
    assert res.safety_notice == APPROVED_SAFETY_NOTICE
    assert len(res.observations) >= 2
    assert "30 glucose readings" in res.observations[0].statement
    assert "20" in res.observations[1].statement
    assert res.provider_metadata.provider == "mock"
    assert res.provider_metadata.model == "mock-clinical-summary-v1"


def test_insufficient_information():
    insufficient = copy.deepcopy(INSUFFICIENT_CONTEXT_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(insufficient)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert "limited and do not support" in res.summary
    assert len(res.observations) == 1
    assert "Only a limited number of records" in res.observations[0].statement


def test_elevated_glucose_delayed_dose_correlation():
    elevated = copy.deepcopy(ELEVATED_GLUCOSE_DELAYED_DOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(elevated)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    # Verify correlation statement is present
    assert len(res.correlations) >= 1
    correlation = res.correlations[0]
    assert "co-occurrence does not establish causation" in correlation.statement
    assert "caused" not in correlation.statement

    # Must have both glucose summary and adherence summary evidence references
    assert "glucose-summary:selected-period" in correlation.evidence_references
    assert "adherence-summary:selected-period" in correlation.evidence_references


def test_ignores_prompt_injection():
    inject = copy.deepcopy(INJECTION_DESCRIPTION_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(inject)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    # Injecting instructions inside event descriptions should not alter mock output features
    assert "Ignore all previous instructions" not in res.summary
    assert "20 units" not in res.summary
    assert not any("prescribe" in obs.statement.lower() for obs in res.observations)
