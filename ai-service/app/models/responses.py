from typing import Literal
from uuid import UUID

from pydantic import BaseModel, ConfigDict, Field, field_validator, model_validator

from app.config.settings import get_settings
from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.common import EvidenceReference


class Observation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    statement: str
    evidence_references: list[EvidenceReference] = Field(..., min_length=1)

    @field_validator("statement")
    @classmethod
    def check_statement_len(cls, v: str) -> str:
        settings = get_settings()
        if not v or v.isspace():
            raise ValueError("Observation statement cannot be empty")
        if len(v) > settings.AI_MAX_TEXT_LENGTH:
            raise ValueError(f"Observation statement length exceeds {settings.AI_MAX_TEXT_LENGTH}")
        return v


class Correlation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    statement: str
    confidence: Literal["low", "moderate", "high"]
    evidence_references: list[EvidenceReference] = Field(..., min_length=1)

    @field_validator("statement")
    @classmethod
    def check_statement_len(cls, v: str) -> str:
        settings = get_settings()
        if not v or v.isspace():
            raise ValueError("Correlation statement cannot be empty")
        if len(v) > settings.AI_MAX_TEXT_LENGTH:
            raise ValueError(f"Correlation statement length exceeds {settings.AI_MAX_TEXT_LENGTH}")
        return v


class ProviderMetadata(BaseModel):
    model_config = ConfigDict(extra="forbid")

    provider: Literal["mock"]
    model: str
    prompt_version: Literal["clinical-summary-v1"]


class ClinicalSummaryResponse(BaseModel):
    model_config = ConfigDict(extra="forbid")

    request_id: UUID
    summary: str
    observations: list[Observation]
    correlations: list[Correlation]
    uncertainties: list[str] = Field(..., min_length=1)
    discussion_points: list[str]
    safety_notice: str
    provider_metadata: ProviderMetadata

    @field_validator("safety_notice")
    @classmethod
    def validate_safety_notice(cls, v: str) -> str:
        if v != APPROVED_SAFETY_NOTICE:
            raise ValueError(f"safety_notice must exactly match: '{APPROVED_SAFETY_NOTICE}'")
        return v

    @field_validator("summary")
    @classmethod
    def check_summary_len(cls, v: str) -> str:
        settings = get_settings()
        if not v or v.isspace():
            raise ValueError("Summary cannot be empty")
        if len(v) > settings.AI_MAX_TEXT_LENGTH:
            raise ValueError(f"Summary length exceeds {settings.AI_MAX_TEXT_LENGTH}")
        return v

    @model_validator(mode="after")
    def validate_response_limits(self) -> "ClinicalSummaryResponse":
        settings = get_settings()

        # Enforce list sizes
        if len(self.observations) > settings.AI_MAX_CONTEXT_RECORDS:
            raise ValueError("Too many observations in response")
        if len(self.correlations) > settings.AI_MAX_CONTEXT_RECORDS:
            raise ValueError("Too many correlations in response")
        if len(self.uncertainties) > settings.AI_MAX_CONTEXT_RECORDS:
            raise ValueError("Too many uncertainties in response")
        if len(self.discussion_points) > settings.AI_MAX_CONTEXT_RECORDS:
            raise ValueError("Too many discussion points in response")

        # Verify text limits in arrays
        for obs in self.observations:
            if len(obs.statement) > settings.AI_MAX_TEXT_LENGTH:
                raise ValueError("Observation statement too long")
        for corr in self.correlations:
            if len(corr.statement) > settings.AI_MAX_TEXT_LENGTH:
                raise ValueError("Correlation statement too long")
        for u in self.uncertainties:
            if not u or u.isspace():
                raise ValueError("Uncertainty statement cannot be empty")
            if len(u) > settings.AI_MAX_TEXT_LENGTH:
                raise ValueError("Uncertainty statement too long")
        for d in self.discussion_points:
            if not d or d.isspace():
                raise ValueError("Discussion point cannot be empty")
            if len(d) > settings.AI_MAX_TEXT_LENGTH:
                raise ValueError("Discussion point too long")

        return self
