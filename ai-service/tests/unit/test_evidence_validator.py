import copy
from uuid import uuid4

import pytest
from pydantic import ValidationError

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse, Correlation, Observation, ProviderMetadata
from app.validators.evidence_validator import EvidenceValidationError, validate_evidence
from tests.fixtures.clinical_contexts import STABLE_GLUCOSE_PAYLOAD


def make_test_response(observations: list, correlations: list = None) -> ClinicalSummaryResponse:
    if correlations is None:
        correlations = []
    return ClinicalSummaryResponse(
        request_id=uuid4(),
        summary="Test summary",
        observations=observations,
        correlations=correlations,
        uncertainties=["Data is limited."],
        discussion_points=["Consult doctor."],
        safety_notice=APPROVED_SAFETY_NOTICE,
        provider_metadata=ProviderMetadata(provider="mock", model="mock", prompt_version="clinical-summary-v1"),
    )


def test_valid_references_pass():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    # Observations use references from stable payload
    res = make_test_response(
        observations=[
            Observation(
                statement="Glucose was tracked.",
                evidence_references=["glucose-summary:selected-period"],
            )
        ],
        correlations=[
            Correlation(
                statement="Co-occurrence observed.",
                confidence="moderate",
                evidence_references=[
                    "glucose-summary:selected-period",
                    "adherence-summary:selected-period",
                ],
            )
        ],
    )
    # Should not raise exception
    validate_evidence(req, res)


def test_invented_reference_rejected():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    res = make_test_response(
        observations=[
            Observation(
                statement="Glucose was tracked.",
                evidence_references=["glucose-summary:invented-ref"],
            )
        ]
    )
    with pytest.raises(EvidenceValidationError, match="Invented evidence reference"):
        validate_evidence(req, res)


def test_empty_observation_references_rejected():
    # Pydantic list min_length=1 validator will reject empty list first during instantiation
    with pytest.raises(ValidationError):
        Observation(
            statement="Glucose was tracked.",
            evidence_references=[],  # type: ignore
        )


def test_insufficient_correlation_references_rejected():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    res = make_test_response(
        observations=[
            Observation(
                statement="Glucose was tracked.",
                evidence_references=["glucose-summary:selected-period"],
            )
        ],
        correlations=[
            Correlation(
                statement="Co-occurrence observed.",
                confidence="moderate",
                evidence_references=["glucose-summary:selected-period"],  # Only 1 ref
            )
        ],
    )
    with pytest.raises(EvidenceValidationError, match="must reference at least two distinct evidence sources"):
        validate_evidence(req, res)


def test_reference_from_omitted_context_rejected():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    # The stable payload has no storage summary, so storage-summary reference should fail
    res = make_test_response(
        observations=[
            Observation(
                statement="Temperature was tracked.",
                evidence_references=["storage-summary:selected-period"],
            )
        ]
    )
    with pytest.raises(EvidenceValidationError, match="Invented evidence reference"):
        validate_evidence(req, res)


def test_alert_type_used_as_reference_rejected():
    # Using 'GLUCOSE_HIGH' instead of valid reference format
    # Caught by Pydantic validation because 'GLUCOSE_HIGH' fails EvidenceReference format check
    with pytest.raises(ValidationError):
        Observation(
            statement="Glucose was high.",
            evidence_references=["GLUCOSE_HIGH"],  # type: ignore
        )
