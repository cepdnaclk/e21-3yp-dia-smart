import asyncio
import copy
import os
from datetime import UTC, datetime, timedelta
from uuid import uuid4

import pytest
from pydantic import ValidationError

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse, Observation, ProviderMetadata
from app.providers.mock_provider import MockProvider
from app.validators.medical_safety_validator import MedicalSafetyRejection, validate_medical_safety
from tests.fixtures.clinical_contexts import STABLE_GLUCOSE_PAYLOAD, make_iso


def get_base_payload():
    return copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)


# 1. Patient Reference Validation Tests
@pytest.mark.parametrize(
    "ref",
    [
        "patient-1",
        "user_234",
        "db-9999",
        "patient-ref@domain.com",
        "patient ref space",
        "pat",  # too short
        "123abcde",  # starts with number
        "a" * 129,  # too long
        "   ",  # whitespace
        "",  # empty
    ],
)
def test_invalid_patient_references(ref):
    payload = get_base_payload()
    payload["patient_reference"] = ref
    with pytest.raises(ValidationError):
        ClinicalSummaryRequest.model_validate(payload)


@pytest.mark.parametrize(
    "ref",
    [
        "patient_ref_123",
        "ptref:a81d2e44",
        "clinical-ref_12ab90cd",
        "patient-ref-7e05bb",
    ],
)
def test_valid_patient_references(ref):
    payload = get_base_payload()
    payload["patient_reference"] = ref
    req = ClinicalSummaryRequest.model_validate(payload)
    assert req.patient_reference == ref


# 2. Internal Data Consistency Tests
def test_glucose_consistency_average_bounds():
    payload = get_base_payload()
    payload["glucose_summary"]["average"] = 150.0
    with pytest.raises(ValidationError, match="Glucose statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


def test_glucose_consistency_reading_counts():
    payload = get_base_payload()
    payload["glucose_summary"]["reading_count"] = 15
    payload["glucose_summary"]["high_reading_count"] = 10
    payload["glucose_summary"]["low_reading_count"] = 10
    with pytest.raises(ValidationError, match="Glucose statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


def test_adherence_consistency_recorded_exceeds_scheduled():
    payload = get_base_payload()
    payload["adherence_summary"]["recorded_administrations"] = 25
    with pytest.raises(ValidationError, match="Adherence statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


def test_adherence_consistency_delayed_exceeds_recorded():
    payload = get_base_payload()
    payload["adherence_summary"]["recorded_administrations"] = 10
    payload["adherence_summary"]["delayed_administrations"] = 15
    with pytest.raises(ValidationError, match="Adherence statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


# 3. Exact Date-Range Validation Tests
def test_exact_date_range_limits():
    payload = get_base_payload()
    start_dt = datetime(2026, 7, 1, 0, 0, 0, tzinfo=UTC)

    # 1. Exactly 31 days accepted
    payload["requested_period"] = {"from": start_dt.isoformat(), "to": (start_dt + timedelta(days=31)).isoformat()}
    req = ClinicalSummaryRequest.model_validate(payload)
    assert req.requested_period.to - req.requested_period.from_ == timedelta(days=31)

    # 2. 31 days plus one microsecond rejected
    payload["requested_period"] = {"from": start_dt.isoformat(), "to": (start_dt + timedelta(days=31, microseconds=1)).isoformat()}
    with pytest.raises(ValidationError, match="Requested period duration exceeds the maximum limit"):
        ClinicalSummaryRequest.model_validate(payload)

    # 3. 31 days plus one second rejected
    payload["requested_period"] = {"from": start_dt.isoformat(), "to": (start_dt + timedelta(days=31, seconds=1)).isoformat()}
    with pytest.raises(ValidationError, match="Requested period duration exceeds the maximum limit"):
        ClinicalSummaryRequest.model_validate(payload)

    # 4. 31 days plus 23 hours rejected
    payload["requested_period"] = {"from": start_dt.isoformat(), "to": (start_dt + timedelta(days=31, hours=23)).isoformat()}
    with pytest.raises(ValidationError, match="Requested period duration exceeds the maximum limit"):
        ClinicalSummaryRequest.model_validate(payload)

    # 5. Less than 31 days accepted
    payload["requested_period"] = {"from": start_dt.isoformat(), "to": (start_dt + timedelta(days=30, hours=23, minutes=59)).isoformat()}
    req = ClinicalSummaryRequest.model_validate(payload)
    assert req.requested_period.to - req.requested_period.from_ < timedelta(days=31)

    # 6. Equal start and end rejected
    payload["requested_period"] = {"from": start_dt.isoformat(), "to": start_dt.isoformat()}
    with pytest.raises(ValidationError, match="must be earlier than"):
        ClinicalSummaryRequest.model_validate(payload)

    # 7. Start after end rejected
    payload["requested_period"] = {"from": (start_dt + timedelta(days=1)).isoformat(), "to": start_dt.isoformat()}
    with pytest.raises(ValidationError, match="must be earlier than"):
        ClinicalSummaryRequest.model_validate(payload)


# 4. Sparse Context Mock Rendering Tests & Discussion Points Grounding
def test_mock_provider_sparse_glucose_only():
    payload = {
        "request_id": str(uuid4()),
        "request_type": "CLINICAL_SUMMARY",
        "prompt_version": "clinical-summary-v1",
        "patient_reference": "patient-ref-glucose-only",
        "requested_period": {"from": make_iso(2026, 7, 1, 0, 0), "to": make_iso(2026, 7, 10, 0, 0)},
        "glucose_summary": {
            "evidence_reference": "glucose-summary:selected-period",
            "unit": "mg/dL",
            "reading_count": 8,
            "average": 110.0,
            "minimum": 90.0,
            "maximum": 130.0,
            "high_reading_count": 0,
            "low_reading_count": 0,
        },
    }
    req = ClinicalSummaryRequest.model_validate(payload)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert "glucose information" in res.summary
    assert "glucose" in res.summary
    assert "adherence" not in res.summary
    assert len(res.discussion_points) == 1
    assert res.discussion_points[0] == "A healthcare professional may review the supplied glucose summary and recorded threshold counts."


def test_mock_provider_adherence_only():
    payload = {
        "request_id": str(uuid4()),
        "request_type": "CLINICAL_SUMMARY",
        "prompt_version": "clinical-summary-v1",
        "patient_reference": "patient-ref-adherence-only",
        "requested_period": {"from": make_iso(2026, 7, 1, 0, 0), "to": make_iso(2026, 7, 10, 0, 0)},
        "adherence_summary": {
            "evidence_reference": "adherence-summary:selected-period",
            "scheduled_administrations": 10,
            "recorded_administrations": 10,
            "delayed_administrations": 0,
            "missed_administrations": 0,
        },
    }
    req = ClinicalSummaryRequest.model_validate(payload)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert "recorded-adherence" in res.summary
    assert "glucose" not in res.summary
    assert len(res.discussion_points) == 1
    assert res.discussion_points[0] == "A healthcare professional may review the supplied administration and adherence summary."


def test_mock_provider_storage_only():
    payload = {
        "request_id": str(uuid4()),
        "request_type": "CLINICAL_SUMMARY",
        "prompt_version": "clinical-summary-v1",
        "patient_reference": "patient-ref-storage-only",
        "requested_period": {"from": make_iso(2026, 7, 1, 0, 0), "to": make_iso(2026, 7, 10, 0, 0)},
        "storage_summary": {
            "evidence_reference": "storage-summary:selected-period",
            "unit": "celsius",
            "reading_count": 10,
            "average_temperature": 5.0,
            "minimum_temperature": 4.0,
            "maximum_temperature": 6.0,
            "excursion_count": 0,
        },
    }
    req = ClinicalSummaryRequest.model_validate(payload)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert "storage-temperature" in res.summary
    assert len(res.discussion_points) == 1
    assert res.discussion_points[0] == "Review the supplied storage-temperature summary and recorded excursion count."


def test_mock_provider_inventory_only():
    payload = {
        "request_id": str(uuid4()),
        "request_type": "CLINICAL_SUMMARY",
        "prompt_version": "clinical-summary-v1",
        "patient_reference": "patient-ref-inv-only",
        "requested_period": {"from": make_iso(2026, 7, 1, 0, 0), "to": make_iso(2026, 7, 10, 0, 0)},
        "inventory_summary": {
            "evidence_reference": "inventory-summary:selected-period",
            "latest_status": "LOW",
            "latest_estimated_units": 12.0,
            "shortage_event_count": 0,
        },
    }
    req = ClinicalSummaryRequest.model_validate(payload)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert "inventory information" in res.summary
    assert len(res.discussion_points) == 1
    assert res.discussion_points[0] == "Review the supplied inventory status and shortage-event count."


def test_mock_provider_mixed_context():
    payload = {
        "request_id": str(uuid4()),
        "request_type": "CLINICAL_SUMMARY",
        "prompt_version": "clinical-summary-v1",
        "patient_reference": "patient-ref-mixed",
        "requested_period": {"from": make_iso(2026, 7, 1, 0, 0), "to": make_iso(2026, 7, 10, 0, 0)},
        "glucose_summary": {
            "evidence_reference": "glucose-summary:selected-period",
            "unit": "mg/dL",
            "reading_count": 8,
            "average": 110.0,
            "minimum": 90.0,
            "maximum": 130.0,
            "high_reading_count": 0,
            "low_reading_count": 0,
        },
        "inventory_summary": {
            "evidence_reference": "inventory-summary:selected-period",
            "latest_status": "OK",
            "latest_estimated_units": 100.0,
            "shortage_event_count": 0,
        },
    }
    req = ClinicalSummaryRequest.model_validate(payload)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert "glucose" in res.summary
    assert "inventory" in res.summary
    assert "storage" not in res.summary
    assert len(res.discussion_points) == 2
    assert "A healthcare professional may review the supplied glucose summary and recorded threshold counts." in res.discussion_points
    assert "Review the supplied inventory status and shortage-event count." in res.discussion_points


# 5. Expanded Medical-Safety Validation Tests
def make_base_response(summary: str = "A clean summary") -> ClinicalSummaryResponse:
    return ClinicalSummaryResponse(
        request_id=uuid4(),
        summary=summary,
        observations=[Observation(statement="Glucose was measured.", evidence_references=["glucose-summary:selected-period"])],
        correlations=[],
        uncertainties=["Telemetry is limited."],
        discussion_points=["Contact your doctor."],
        safety_notice=APPROVED_SAFETY_NOTICE,
        provider_metadata=ProviderMetadata(provider="mock", model="mock-model", prompt_version="clinical-summary-v1"),
    )


@pytest.mark.parametrize(
    "phrase",
    [
        # Diagnosis
        "ketoacidosis was observed",
        "diabetic complication is probable",
        "appears to have a complication",
        "confirms a diagnosis",
        "suffer from DKA",
        "has type 2 diabetes",
        "The patient has hyperglycemia.",
        "The patient has hypoglycemia.",
        "This indicates hypoglycemia.",
        "This confirms hyperglycemia.",
        "diagnosed with diabetes",
        # Dosage
        "Administer 12 units of insulin tonight.",
        "Take twelve units of insulin tonight.",
        "Increase your dose by two units.",
        "Decrease your evening dose.",
        "Raise your insulin by two units.",
        "Lower your evening dose.",
        "Modify the dose.",
        "Add two units.",
        "Remove two units.",
        "raise insulin by 4 units",
        "reduce the dose",
        "double the dose",
        # Medication
        "Begin insulin tomorrow.",
        "Start insulin tomorrow.",
        "Begin metformin tomorrow.",
        "Switch medication.",
        "Discontinue the medication.",
        "Replace the medication.",
        "begin metformin",
        "start taking metformin",
        "stop taking medication",
        "start a new medication",
        # Treatment
        "Change the prescription.",
        "change the administration schedule",
        "modify the treatment plan",
        "guarantee control",
        "I prescribe a higher dose.",
        # Causation
        "The delayed dose led to the glucose spike.",
        "The glucose spike resulted from the delayed injection.",
        "The missed dose is the reason for the high reading.",
        "Storage conditions caused the abnormal result.",
        # Medical Impersonation
        "As your doctor, I recommend this.",
        "I am a doctor.",
        "my medical advice is",
    ],
)
def test_expanded_medical_safety_rejections(phrase):
    res = make_base_response(summary=phrase)
    # Check that this validator does not claim to guarantee clinical safety, it just raises MedicalSafetyRejection
    with pytest.raises(MedicalSafetyRejection, match="Clinical safety violation"):
        validate_medical_safety(res)


@pytest.mark.parametrize(
    "phrase",
    [
        "The selected records contain elevated glucose readings.",
        "Some elevated readings were recorded near delayed administrations.",
        "The supplied records show a temporal association.",
        "This co-occurrence does not establish causation.",
        "The supplied information is insufficient to determine a medical cause.",
        "A healthcare professional may review these records.",
        "No treatment recommendation is provided.",
    ],
)
def test_permitted_medical_safety_phrases(phrase):
    res = make_base_response(summary=phrase)
    # Permitted variations should run without raising any safety violations
    validate_medical_safety(res)


# 6. Codebase Safety Checks (Ensuring no forbidden methods or constructs remain)
def test_no_forbidden_patterns_in_app_source():
    app_dir = os.path.join("d:\\3YP\\e21-3yp-dia-smart\\ai-service", "app")

    for root, _, files in os.walk(app_dir):
        for file in files:
            if file.endswith(".py"):
                file_path = os.path.join(root, file)
                with open(file_path, encoding="utf-8") as f:
                    content = f.read()

                    # 1. No request._receive
                    assert "request._receive" not in content, f"Forbidden request._receive usage in {file_path}"

                    # 2. No await request.body()
                    assert "request.body()" not in content, f"Forbidden request.body() usage in {file_path}"

                    # 3. No logger.exception in exception handlers
                    if file == "handlers.py":
                        assert "logger.exception" not in content, f"Forbidden logger.exception in exception handler {file_path}"
                        import re as test_re

                        assert not test_re.search(r"logger\.\w+\(.*(?:exc|e)\.message", content), f"Raw exc.message logging detected in {file_path}"
