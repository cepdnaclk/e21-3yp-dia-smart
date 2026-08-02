from typing import Any

from pydantic import ValidationError

from app.models.responses import ClinicalSummaryResponse


class ResponseValidationError(Exception):
    """Exception raised when the generated summary fails Pydantic schema validation."""

    pass


def validate_response_schema(raw_response: Any) -> ClinicalSummaryResponse:
    """
    Parses and validates raw provider output against the strict ClinicalSummaryResponse model.
    Raises ResponseValidationError if parsing fails.
    """
    try:
        if isinstance(raw_response, ClinicalSummaryResponse):
            # Already validated model
            return raw_response

        if isinstance(raw_response, dict):
            return ClinicalSummaryResponse.model_validate(raw_response)

        raise ResponseValidationError(f"Unsupported raw response type: {type(raw_response)}. Expected dict or ClinicalSummaryResponse.")
    except ValidationError as e:
        raise ResponseValidationError(f"Provider response failed schema validation: {e.errors()}") from e
