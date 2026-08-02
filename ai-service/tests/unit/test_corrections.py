import asyncio
import copy
from uuid import uuid4

import pytest
from pydantic import ValidationError

from app.exceptions.handlers import safe_uuid
from app.models.requests import ClinicalSummaryRequest
from app.providers.mock_provider import MockProvider
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
    # Contradiction: average (150) is greater than maximum (130)
    payload["glucose_summary"]["average"] = 150.0
    with pytest.raises(ValidationError, match="Glucose statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


def test_glucose_consistency_reading_counts():
    payload = get_base_payload()
    # Contradiction: high + low (10 + 10 = 20) exceeds total reading_count (15)
    payload["glucose_summary"]["reading_count"] = 15
    payload["glucose_summary"]["high_reading_count"] = 10
    payload["glucose_summary"]["low_reading_count"] = 10
    with pytest.raises(ValidationError, match="Glucose statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


def test_adherence_consistency_recorded_exceeds_scheduled():
    payload = get_base_payload()
    # Contradiction: recorded administrations (25) exceeds scheduled (20)
    payload["adherence_summary"]["recorded_administrations"] = 25
    with pytest.raises(ValidationError, match="Adherence statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


def test_adherence_consistency_delayed_exceeds_recorded():
    payload = get_base_payload()
    # Contradiction: delayed (15) exceeds recorded (10)
    payload["adherence_summary"]["recorded_administrations"] = 10
    payload["adherence_summary"]["delayed_administrations"] = 15
    with pytest.raises(ValidationError, match="Adherence statistics contradiction"):
        ClinicalSummaryRequest.model_validate(payload)


# 3. Time Period Validation Tests
def test_alert_outside_requested_period():
    payload = get_base_payload()
    # Period: 2026-07-01 to 2026-07-10
    payload["relevant_alerts"] = [
        {
            "evidence_reference": "alert-event:ref-001",
            "alert_type": "GLUCOSE_HIGH",
            "severity": "HIGH",
            "status": "OPEN",
            "recorded_at": make_iso(2026, 7, 15, 12, 0),  # OUTSIDE
        }
    ]
    with pytest.raises(ValidationError, match="falls outside requested period"):
        ClinicalSummaryRequest.model_validate(payload)


def test_event_outside_requested_period():
    payload = get_base_payload()
    payload["selected_events"] = [
        {
            "evidence_reference": "glucose-event:ref-019",
            "event_type": "GLUCOSE_READING",
            "recorded_at": make_iso(2026, 6, 28, 12, 0),  # OUTSIDE
            "value": 140.0,
            "unit": "mg/dL",
        }
    ]
    with pytest.raises(ValidationError, match="falls outside requested period"):
        ClinicalSummaryRequest.model_validate(payload)


# 4. Sparse Context Mock Rendering Tests
def test_mock_provider_sparse_context():
    # Construct an alert-only request
    payload = {
        "request_id": str(uuid4()),
        "request_type": "CLINICAL_SUMMARY",
        "prompt_version": "clinical-summary-v1",
        "patient_reference": "patient-ref-alert-only",
        "requested_period": {"from": make_iso(2026, 7, 1, 0, 0), "to": make_iso(2026, 7, 10, 0, 0)},
        "relevant_alerts": [
            {
                "evidence_reference": "alert-event:ref-001",
                "alert_type": "GLUCOSE_HIGH",
                "severity": "HIGH",
                "status": "OPEN",
                "recorded_at": make_iso(2026, 7, 5, 12, 0),
            }
        ],
    }
    req = ClinicalSummaryRequest.model_validate(payload)
    provider = MockProvider()
    res = asyncio.run(provider.generate_clinical_summary(req))

    assert "alert information" in res.summary
    assert len(res.observations) == 1
    assert "1 clinical alerts were logged" in res.observations[0].statement
    assert "glucose" not in res.summary


def test_mock_provider_inventory_only():
    # Construct an inventory-only request
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
    assert len(res.observations) == 1
    assert "Insulin stock status was recorded as LOW" in res.observations[0].statement


# 5. Safe UUID utility validation
def test_safe_uuid_conversion():
    assert safe_uuid(None) is None
    uid = uuid4()
    assert safe_uuid(uid) == uid
    assert safe_uuid(str(uid)) == uid
    assert safe_uuid("not-a-valid-uuid-string") is None
