import logging

from app.exceptions.types import (
    AiEvidenceValidationError,
    AiMedicalSafetyRejectionError,
    AiProviderError,
    AiResponseValidationError,
    AiUnsupportedPromptVersionError,
)
from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse
from app.prompts.prompt_builder import build_prompt
from app.providers.factory import get_provider
from app.validators.evidence_validator import EvidenceValidationError, validate_evidence
from app.validators.medical_safety_validator import MedicalSafetyRejection, validate_medical_safety
from app.validators.response_validator import ResponseValidationError, validate_response_schema

logger = logging.getLogger("app.services.clinical_summary")


class ClinicalSummaryService:
    """
    Coordinates the validation and execution pipeline for clinical summaries.
    Enforces sequential checks: prompt validation, provider run, schema mapping,
    clinical safety filtering, and citation integrity.
    """

    async def generate_summary(self, request: ClinicalSummaryRequest) -> ClinicalSummaryResponse:
        logger.info(f"Initializing clinical summary generation pipeline. Request ID: {request.request_id}")

        # 1. Prompt version validation & prompt building
        try:
            build_prompt(request)
        except ValueError as e:
            raise AiUnsupportedPromptVersionError(str(e)) from e

        # 2. AI Provider execution
        provider = get_provider()
        try:
            provider_response = await provider.generate_clinical_summary(request)
        except Exception as e:
            raise AiProviderError(f"AI provider execution failed: {e}") from e

        # 3. Response-schema validation
        try:
            validated_response = validate_response_schema(provider_response)
        except ResponseValidationError as e:
            raise AiResponseValidationError(str(e)) from e

        # 4. Medical-safety validation
        try:
            validate_medical_safety(validated_response)
        except MedicalSafetyRejection as e:
            raise AiMedicalSafetyRejectionError(str(e)) from e

        # 5. Evidence-reference validation
        try:
            validate_evidence(request, validated_response)
        except EvidenceValidationError as e:
            raise AiEvidenceValidationError(str(e)) from e

        logger.info(f"Clinical summary pipeline completed successfully. Request ID: {request.request_id}")
        return validated_response
