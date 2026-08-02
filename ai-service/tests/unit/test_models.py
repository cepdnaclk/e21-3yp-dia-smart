import copy

import pytest
from pydantic import ValidationError

from app.models.requests import ClinicalSummaryRequest
from tests.fixtures.clinical_contexts import (
    ELEVATED_GLUCOSE_DELAYED_DOSE_PAYLOAD,
    STABLE_GLUCOSE_PAYLOAD,
)


def test_valid_requests_parsing():
    # Verify standard payloads load without errors using deepcopy
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    req = ClinicalSummaryRequest.model_validate(stable)
    assert req.patient_reference == "patient-ref-stable"
    assert req.request_type == "CLINICAL_SUMMARY"

    elevated = copy.deepcopy(ELEVATED_GLUCOSE_DELAYED_DOSE_PAYLOAD)
    req_elevated = ClinicalSummaryRequest.model_validate(elevated)
    assert len(req_elevated.relevant_alerts) == 1
    assert len(req_elevated.selected_events) == 2


def test_missing_all_context():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    # Remove context sections
    payload["glucose_summary"] = None
    payload["adherence_summary"] = None
    payload["relevant_alerts"] = []
    payload["selected_events"] = []

    with pytest.raises(ValidationError, match="At least one clinical context section"):
        ClinicalSummaryRequest.model_validate(payload)


def test_invalid_uuid():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["request_id"] = "not-a-uuid"
    with pytest.raises(ValidationError):
        ClinicalSummaryRequest.model_validate(payload)


def test_numeric_only_patient_ref():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["patient_reference"] = "12345"
    with pytest.raises(ValidationError, match="cannot be numeric-only"):
        ClinicalSummaryRequest.model_validate(payload)


def test_pii_patient_ref():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["patient_reference"] = "test@example.com"
    with pytest.raises(ValidationError, match="cannot contain email address"):
        ClinicalSummaryRequest.model_validate(payload)


def test_raw_db_identifier_patient_ref():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["patient_reference"] = "patient-1"
    with pytest.raises(ValidationError, match="cannot be an obvious raw database identifier"):
        ClinicalSummaryRequest.model_validate(payload)


def test_invalid_date_order():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["requested_period"] = {"from": "2026-07-10T00:00:00Z", "to": "2026-07-01T00:00:00Z"}
    with pytest.raises(ValidationError, match="from must be earlier than"):
        ClinicalSummaryRequest.model_validate(payload)


def test_timezone_naive_timestamps():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["requested_period"] = {"from": "2026-07-01T00:00:00", "to": "2026-07-10T00:00:00"}
    with pytest.raises(ValidationError, match="must be timezone-aware"):
        ClinicalSummaryRequest.model_validate(payload)


def test_negative_counts():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["glucose_summary"]["reading_count"] = -5
    with pytest.raises(ValidationError):
        ClinicalSummaryRequest.model_validate(payload)


def test_infinite_float():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["glucose_summary"]["average"] = float("inf")
    with pytest.raises(ValidationError, match="must be a finite number"):
        ClinicalSummaryRequest.model_validate(payload)


def test_nan_float():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["glucose_summary"]["average"] = float("nan")
    with pytest.raises(ValidationError, match="must be a finite number"):
        ClinicalSummaryRequest.model_validate(payload)


def test_duplicate_evidence_references():
    payload = copy.deepcopy(ELEVATED_GLUCOSE_DELAYED_DOSE_PAYLOAD)
    # Force duplication of evidence references
    payload["selected_events"][0]["evidence_reference"] = "alert-event:ref-001"
    with pytest.raises(ValidationError, match="Duplicate evidence references"):
        ClinicalSummaryRequest.model_validate(payload)


def test_invalid_evidence_ref_format():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["glucose_summary"]["evidence_reference"] = "invalid_format_no_colon"
    with pytest.raises(ValidationError, match="must follow 'category:opaque-ref'"):
        ClinicalSummaryRequest.model_validate(payload)


def test_unknown_evidence_category():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["glucose_summary"]["evidence_reference"] = "random-category:ref-001"
    with pytest.raises(ValidationError, match="Invalid evidence category"):
        ClinicalSummaryRequest.model_validate(payload)


def test_forbidden_extra_field():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    payload["random_extra_field"] = "value"
    with pytest.raises(ValidationError):
        ClinicalSummaryRequest.model_validate(payload)
