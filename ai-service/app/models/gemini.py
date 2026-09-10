from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.config.settings import get_settings
from app.models.common import EvidenceReference


class GeminiObservation(BaseModel):
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


class GeminiCorrelation(BaseModel):
    model_config = ConfigDict(extra="forbid")

    statement: str
    confidence: Literal["low", "moderate", "high"]
    evidence_references: list[EvidenceReference] = Field(..., min_length=2)

    @field_validator("statement")
    @classmethod
    def check_statement_len(cls, v: str) -> str:
        settings = get_settings()
        if not v or v.isspace():
            raise ValueError("Correlation statement cannot be empty")
        if len(v) > settings.AI_MAX_TEXT_LENGTH:
            raise ValueError(f"Correlation statement length exceeds {settings.AI_MAX_TEXT_LENGTH}")
        return v


class GeminiClinicalInsightPayload(BaseModel):
    """
    Internal strict Pydantic model representing ONLY the model-generated
    clinical insight fields returned by Gemini.
    Server-controlled fields (request_id, provider, model, prompt_version,
    safety_notice) are strictly excluded and attached by trusted FastAPI code.
    """

    model_config = ConfigDict(extra="forbid")

    summary: str
    observations: list[GeminiObservation] = Field(default_factory=list)
    correlations: list[GeminiCorrelation] = Field(default_factory=list)
    uncertainties: list[str] = Field(..., min_length=1)
    discussion_points: list[str] = Field(default_factory=list)

    @field_validator("summary")
    @classmethod
    def check_summary_len(cls, v: str) -> str:
        settings = get_settings()
        if not v or v.isspace():
            raise ValueError("Summary cannot be empty")
        if len(v) > settings.AI_MAX_TEXT_LENGTH:
            raise ValueError(f"Summary length exceeds {settings.AI_MAX_TEXT_LENGTH}")
        return v

    @field_validator("uncertainties")
    @classmethod
    def validate_uncertainties(cls, v: list[str]) -> list[str]:
        settings = get_settings()
        if not v:
            raise ValueError("Uncertainties list cannot be empty")
        for u in v:
            if not u or u.isspace():
                raise ValueError("Uncertainty statement cannot be empty")
            if len(u) > settings.AI_MAX_TEXT_LENGTH:
                raise ValueError(f"Uncertainty statement length exceeds {settings.AI_MAX_TEXT_LENGTH}")
        return v

    @field_validator("discussion_points")
    @classmethod
    def validate_discussion_points(cls, v: list[str]) -> list[str]:
        settings = get_settings()
        for d in v:
            if not d or d.isspace():
                raise ValueError("Discussion point cannot be empty")
            if len(d) > settings.AI_MAX_TEXT_LENGTH:
                raise ValueError(f"Discussion point length exceeds {settings.AI_MAX_TEXT_LENGTH}")
        return v
