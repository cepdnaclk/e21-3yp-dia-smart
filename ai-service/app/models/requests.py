import math
import re
from datetime import datetime
from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.config.settings import get_settings
from app.models.common import EvidenceReference


# Enforce strict float checking for Pydantic v2
def check_finite(value: float | None) -> float | None:
    if value is not None and (math.isnan(value) or math.isinf(value)):
        raise ValueError("Value must be a finite number")
    return value


def check_non_whitespace(value: str | None) -> str | None:
    if value is not None:
        if not value or value.isspace():
            raise ValueError("String value cannot be empty or whitespace-only")
    return value


class Period(BaseModel):
    model_config = ConfigDict(extra="forbid")

    from_: datetime = Field(..., alias="from")
    to: datetime

    @field_validator("from_", "to")
    @classmethod
    def check_tz_aware(cls, v: datetime) -> datetime:
        if v.tzinfo is None:
            raise ValueError("All timestamps must be timezone-aware")
        return v

    @model_validator(mode="after")
    def validate_dates(self) -> "Period":
        if self.from_ >= self.to:
            raise ValueError("requested_period.from must be earlier than requested_period.to")

        # Check range limit
        settings = get_settings()
        diff = self.to - self.from_
        if diff.days > settings.AI_MAX_DATE_RANGE_DAYS:
            raise ValueError(f"Requested period duration ({diff.days} days) exceeds the maximum limit of {settings.AI_MAX_DATE_RANGE_DAYS} days")
        return self


class GlucoseSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    unit: str = Field(..., min_length=1, max_length=32)
    reading_count: int = Field(..., ge=0)
    average: float
    minimum: float
    maximum: float
    high_reading_count: int = Field(..., ge=0)
    low_reading_count: int = Field(..., ge=0)

    @field_validator("average", "minimum", "maximum")
    @classmethod
    def validate_finite(cls, v: float) -> float:
        check_finite(v)
        return v

    @field_validator("unit")
    @classmethod
    def validate_unit_str(cls, v: str) -> str:
        check_non_whitespace(v)
        return v

    @model_validator(mode="after")
    def validate_glucose_consistency(self) -> "GlucoseSummary":
        if not (self.minimum <= self.average <= self.maximum):
            raise ValueError("Glucose statistics contradiction: minimum <= average <= maximum must hold")
        if self.high_reading_count + self.low_reading_count > self.reading_count:
            raise ValueError("Glucose statistics contradiction: high_reading_count + low_reading_count cannot exceed reading_count")
        return self


class AdherenceSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    scheduled_administrations: int = Field(..., ge=0)
    recorded_administrations: int = Field(..., ge=0)
    delayed_administrations: int = Field(..., ge=0)
    missed_administrations: int = Field(..., ge=0)

    @model_validator(mode="after")
    def validate_adherence_consistency(self) -> "AdherenceSummary":
        # Document semantic assumption: recorded_administrations represents subset of scheduled doses actually taken
        if self.recorded_administrations > self.scheduled_administrations:
            raise ValueError("Adherence statistics contradiction: recorded_administrations cannot exceed scheduled_administrations")
        if self.delayed_administrations > self.recorded_administrations:
            raise ValueError("Adherence statistics contradiction: delayed_administrations cannot exceed recorded_administrations")
        if self.missed_administrations > self.scheduled_administrations:
            raise ValueError("Adherence statistics contradiction: missed_administrations cannot exceed scheduled_administrations")
        if self.delayed_administrations + self.missed_administrations > self.scheduled_administrations:
            raise ValueError("Adherence statistics contradiction: delayed_administrations + missed_administrations cannot exceed scheduled_administrations")
        return self


class StorageSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    unit: str = Field(..., min_length=1, max_length=32)
    reading_count: int = Field(..., ge=0)
    average_temperature: float
    minimum_temperature: float
    maximum_temperature: float
    excursion_count: int = Field(..., ge=0)

    @field_validator("average_temperature", "minimum_temperature", "maximum_temperature")
    @classmethod
    def validate_finite(cls, v: float) -> float:
        check_finite(v)
        return v

    @field_validator("unit")
    @classmethod
    def validate_unit_str(cls, v: str) -> str:
        check_non_whitespace(v)
        return v

    @model_validator(mode="after")
    def validate_storage_consistency(self) -> "StorageSummary":
        if not (self.minimum_temperature <= self.average_temperature <= self.maximum_temperature):
            raise ValueError("Storage statistics contradiction: minimum_temperature <= average_temperature <= maximum_temperature must hold")
        if self.excursion_count > self.reading_count:
            raise ValueError("Storage statistics contradiction: excursion_count cannot exceed reading_count")
        return self


class InventorySummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    latest_status: str = Field(..., min_length=1, max_length=64)
    latest_estimated_units: float = Field(..., ge=0)
    shortage_event_count: int = Field(..., ge=0)

    @field_validator("latest_estimated_units")
    @classmethod
    def validate_finite(cls, v: float) -> float:
        check_finite(v)
        return v

    @field_validator("latest_status")
    @classmethod
    def validate_status_str(cls, v: str) -> str:
        check_non_whitespace(v)
        return v


class AlertItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    alert_type: str = Field(..., min_length=1, max_length=64)
    severity: str = Field(..., min_length=1, max_length=32)
    status: str = Field(..., min_length=1, max_length=64)
    recorded_at: datetime

    @field_validator("recorded_at")
    @classmethod
    def check_tz(cls, v: datetime) -> datetime:
        if v.tzinfo is None:
            raise ValueError("All timestamps must be timezone-aware")
        return v

    @field_validator("alert_type", "severity", "status")
    @classmethod
    def validate_alert_fields(cls, v: str) -> str:
        check_non_whitespace(v)
        return v


class SelectedEvent(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    event_type: str = Field(..., min_length=1, max_length=64)
    recorded_at: datetime
    value: float | None = None
    unit: str | None = Field(None, min_length=1, max_length=32)
    status: str | None = Field(None, min_length=1, max_length=64)
    description: str | None = None

    @field_validator("recorded_at")
    @classmethod
    def check_tz(cls, v: datetime) -> datetime:
        if v.tzinfo is None:
            raise ValueError("All timestamps must be timezone-aware")
        return v

    @field_validator("value")
    @classmethod
    def validate_finite(cls, v: float | None) -> float | None:
        return check_finite(v)

    @field_validator("event_type", "unit", "status", "description")
    @classmethod
    def validate_event_fields(cls, v: str | None) -> str | None:
        if v is not None:
            check_non_whitespace(v)
        return v


class ClinicalSummaryRequest(BaseModel):
    model_config = ConfigDict(extra="forbid", populate_by_name=True)

    request_id: UUID
    request_type: Literal["CLINICAL_SUMMARY"]
    prompt_version: Literal["clinical-summary-v1"]
    patient_reference: str
    requested_period: Period

    glucose_summary: GlucoseSummary | None = None
    adherence_summary: AdherenceSummary | None = None
    storage_summary: StorageSummary | None = None
    inventory_summary: InventorySummary | None = None

    relevant_alerts: list[AlertItem] = Field(default_factory=list)
    selected_events: list[SelectedEvent] = Field(default_factory=list)

    @field_validator("patient_reference")
    @classmethod
    def validate_patient_ref(cls, v: str) -> str:
        if not v or v.isspace():
            raise ValueError("patient_reference cannot be empty or whitespace-only")

        if "@" in v:
            raise ValueError("patient_reference cannot contain email address format")

        if " " in v:
            raise ValueError("patient_reference cannot contain spaces")

        if v.isdigit():
            raise ValueError("patient_reference cannot be numeric-only")

        # Reject obvious raw database identifiers
        lower_v = v.lower()
        if re.search(r"^(patient|user|db)[_-]\d+$", lower_v):
            raise ValueError("patient_reference cannot be an obvious raw database identifier")

        # Pseudonymous format validation (starts with a letter, 8-128 chars, only specific symbols)
        ref_regex = re.compile(r"^[A-Za-z][A-Za-z0-9._:-]{7,127}$")
        if not ref_regex.match(v):
            raise ValueError("patient_reference must be a pseudonymous identifier (8-128 chars, starting with letter, containing only alphanumeric, dot, underscore, colon, hyphen)")

        return v

    @model_validator(mode="after")
    def validate_safety_limits_and_duplicates(self) -> "ClinicalSummaryRequest":
        settings = get_settings()

        # Enforce list sizes
        if len(self.relevant_alerts) > settings.AI_MAX_ALERTS:
            raise ValueError(f"relevant_alerts length exceeds limit of {settings.AI_MAX_ALERTS}")
        if len(self.selected_events) > settings.AI_MAX_SELECTED_EVENTS:
            raise ValueError(f"selected_events length exceeds limit of {settings.AI_MAX_SELECTED_EVENTS}")

        # At least one context block must be supplied
        has_any_context = (
            self.glucose_summary is not None
            or self.adherence_summary is not None
            or self.storage_summary is not None
            or self.inventory_summary is not None
            or len(self.relevant_alerts) > 0
            or len(self.selected_events) > 0
        )
        if not has_any_context:
            raise ValueError("At least one clinical context section, alert, or selected event must be supplied")

        # Validate that alerts and events fall within the requested period boundaries
        period_from = self.requested_period.from_
        period_to = self.requested_period.to

        for i, a in enumerate(self.relevant_alerts):
            if not (period_from <= a.recorded_at <= period_to):
                raise ValueError(f"Alert {i} recorded_at ({a.recorded_at}) falls outside requested period ({period_from} to {period_to})")

        for i, e in enumerate(self.selected_events):
            if not (period_from <= e.recorded_at <= period_to):
                raise ValueError(f"Selected event {i} recorded_at ({e.recorded_at}) falls outside requested period ({period_from} to {period_to})")

        # Enforce uniqueness of evidence references across all inputs
        all_refs = []
        if self.glucose_summary:
            all_refs.append(self.glucose_summary.evidence_reference)
        if self.adherence_summary:
            all_refs.append(self.adherence_summary.evidence_reference)
        if self.storage_summary:
            all_refs.append(self.storage_summary.evidence_reference)
        if self.inventory_summary:
            all_refs.append(self.inventory_summary.evidence_reference)

        for a in self.relevant_alerts:
            all_refs.append(a.evidence_reference)
        for e in self.selected_events:
            all_refs.append(e.evidence_reference)

        if len(all_refs) != len(set(all_refs)):
            raise ValueError("Duplicate evidence references across supplied context objects are forbidden")

        # PII checks on descriptions/notes
        email_regex = re.compile(r"[\w\.-]+@[\w\.-]+")
        for e in self.selected_events:
            if e.description:
                if len(e.description) > settings.AI_MAX_TEXT_LENGTH:
                    raise ValueError(f"event description length exceeds {settings.AI_MAX_TEXT_LENGTH}")
                if email_regex.search(e.description):
                    raise ValueError("pii warning: event description cannot contain emails")

        return self
