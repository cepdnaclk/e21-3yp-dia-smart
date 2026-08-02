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
    unit: str
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


class AdherenceSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    scheduled_administrations: int = Field(..., ge=0)
    recorded_administrations: int = Field(..., ge=0)
    delayed_administrations: int = Field(..., ge=0)
    missed_administrations: int = Field(..., ge=0)


class StorageSummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    unit: str
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


class InventorySummary(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    latest_status: str
    latest_estimated_units: float
    shortage_event_count: int = Field(..., ge=0)

    @field_validator("latest_estimated_units")
    @classmethod
    def validate_finite(cls, v: float) -> float:
        check_finite(v)
        return v


class AlertItem(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    alert_type: str
    severity: str
    status: str
    recorded_at: datetime

    @field_validator("recorded_at")
    @classmethod
    def check_tz(cls, v: datetime) -> datetime:
        if v.tzinfo is None:
            raise ValueError("All timestamps must be timezone-aware")
        return v


class SelectedEvent(BaseModel):
    model_config = ConfigDict(extra="forbid")

    evidence_reference: EvidenceReference
    event_type: str
    recorded_at: datetime
    value: float | None = None
    unit: str | None = None
    status: str | None = None
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
        settings = get_settings()
        if len(v) > settings.AI_MAX_TEXT_LENGTH:
            raise ValueError(f"patient_reference length exceeds {settings.AI_MAX_TEXT_LENGTH}")

        # Reject numeric-only or obvious database IDs
        if v.isdigit():
            raise ValueError("patient_reference cannot be numeric-only")

        # Reject raw DB patterns like patient-1, user-10, db-15
        import re

        if re.match(r"^(patient|user|db)-\d+$", v, re.IGNORECASE):
            raise ValueError("patient_reference cannot be an obvious raw database identifier")

        # Reject PII patterns (emails, etc.)
        if "@" in v:
            raise ValueError("patient_reference cannot contain email address format")

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
        # Let's inspect selected_events descriptions
        email_regex = re.compile(r"[\w\.-]+@[\w\.-]+")
        for e in self.selected_events:
            if e.description:
                if len(e.description) > settings.AI_MAX_TEXT_LENGTH:
                    raise ValueError(f"event description length exceeds {settings.AI_MAX_TEXT_LENGTH}")
                if email_regex.search(e.description):
                    raise ValueError("pii warning: event description cannot contain emails")

        return self
