from uuid import uuid4

import pytest

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.responses import ClinicalSummaryResponse, Observation, ProviderMetadata
from app.validators.medical_safety_validator import MedicalSafetyRejection, validate_medical_safety


# Helper to build a clean baseline response
def make_base_response(
    summary: str = "Clean summary",
    observations: list = None,
    correlations: list = None,
    uncertainties: list = None,
    discussion_points: list = None,
    safety_notice: str = APPROVED_SAFETY_NOTICE,
) -> ClinicalSummaryResponse:
    if observations is None:
        observations = [
            Observation(
                statement="Telemetry readings are stable.",
                evidence_references=["glucose-summary:selected-period"],
            )
        ]
    if correlations is None:
        correlations = []
    if uncertainties is None:
        uncertainties = ["Data is limited."]
    if discussion_points is None:
        discussion_points = ["Review data with doctor."]

    return ClinicalSummaryResponse.model_construct(
        request_id=uuid4(),
        summary=summary,
        observations=observations,
        correlations=correlations,
        uncertainties=uncertainties,
        discussion_points=discussion_points,
        safety_notice=safety_notice,
        provider_metadata=ProviderMetadata.model_construct(provider="mock", model="mock-model", prompt_version="clinical-summary-v1"),
    )


def test_safe_response_passes():
    res = make_base_response()
    # Should not raise exception
    validate_medical_safety(res)


def test_rejection_of_diagnosis():
    res = make_base_response(summary="The patient is diagnosed with diabetic ketoacidosis.")
    with pytest.raises(MedicalSafetyRejection, match="Clinical safety violation"):
        validate_medical_safety(res)


def test_rejection_of_dose_recommendation():
    res = make_base_response(summary="Increase the insulin dose by 5 units.")
    with pytest.raises(MedicalSafetyRejection, match="Clinical safety violation"):
        validate_medical_safety(res)


def test_rejection_of_prescription_change():
    res = make_base_response(summary="Change the prescription immediately.")
    with pytest.raises(MedicalSafetyRejection, match="Clinical safety violation"):
        validate_medical_safety(res)


def test_rejection_of_stop_medication():
    res = make_base_response(summary="You should stop taking your insulin.")
    with pytest.raises(MedicalSafetyRejection, match="Clinical safety violation"):
        validate_medical_safety(res)


def test_rejection_of_causation_claim():
    res = make_base_response(summary="The delayed injection caused the glucose spike.")
    with pytest.raises(MedicalSafetyRejection, match="Clinical safety violation"):
        validate_medical_safety(res)


def test_rejection_of_doctor_impersonation():
    res = make_base_response(summary="As your doctor, I advise checking your levels.")
    with pytest.raises(MedicalSafetyRejection, match="Clinical safety violation"):
        validate_medical_safety(res)


def test_rejection_of_missing_safety_notice():
    res = make_base_response(safety_notice="Modified safety notice")
    with pytest.raises(MedicalSafetyRejection, match="Missing or modified safety notice"):
        validate_medical_safety(res)


def test_rejection_of_missing_uncertainties():
    res = make_base_response(uncertainties=[])
    with pytest.raises(MedicalSafetyRejection, match="Response must contain at least one uncertainty"):
        validate_medical_safety(res)
