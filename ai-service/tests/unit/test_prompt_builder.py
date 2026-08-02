import copy

import pytest

from app.models.requests import ClinicalSummaryRequest
from app.prompts.prompt_builder import build_prompt
from tests.fixtures.clinical_contexts import (
    INJECTION_DESCRIPTION_PAYLOAD,
    PROMPT_INJECTION_TEXT,
    STABLE_GLUCOSE_PAYLOAD,
)


def test_successful_prompt_build():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    prompt = build_prompt(req)

    assert prompt.prompt_version == "clinical-summary-v1"
    assert "safety_notice" not in prompt.structured_context
    assert "This AI-generated information is intended for review" in prompt.system_instruction
    assert "patient-ref-stable" in prompt.structured_context


def test_preserves_evidence_references():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    prompt = build_prompt(req)

    assert "glucose-summary:selected-period" in prompt.structured_context
    assert "adherence-summary:selected-period" in prompt.structured_context


def test_untrusted_data_isolation():
    inject = copy.deepcopy(INJECTION_DESCRIPTION_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(inject)
    prompt = build_prompt(req)

    assert PROMPT_INJECTION_TEXT in prompt.structured_context
    assert "[UNTRUSTED_USER_CONTENT_START]" in prompt.structured_context
    assert "[UNTRUSTED_USER_CONTENT_END]" in prompt.structured_context


def test_unsupported_prompt_version():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    # Mutate to an unsupported version
    req.prompt_version = "clinical-summary-v2"  # type: ignore

    with pytest.raises(ValueError, match="Unsupported prompt version"):
        build_prompt(req)


def test_no_credentials_leaked():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    prompt = build_prompt(req)

    assert "AI_INTERNAL_SERVICE_TOKEN" not in prompt.system_instruction
    assert "AI_INTERNAL_SERVICE_TOKEN" not in prompt.structured_context
    assert "replace-with-a-long-random" not in prompt.system_instruction
    assert "replace-with-a-long-random" not in prompt.structured_context


def test_deterministic_output():
    stable1 = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    stable2 = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req1 = ClinicalSummaryRequest.model_validate(stable1)
    req2 = ClinicalSummaryRequest.model_validate(stable2)

    prompt1 = build_prompt(req1)
    prompt2 = build_prompt(req2)

    assert prompt1.system_instruction == prompt2.system_instruction
    assert prompt1.structured_context == prompt2.structured_context
