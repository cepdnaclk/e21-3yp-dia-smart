import logging
from typing import Any
from uuid import UUID

from fastapi import Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.exceptions.types import AiBaseException
from app.models.errors import ErrorDetailResponse

logger = logging.getLogger("app.exceptions")


def safe_uuid(v: Any) -> UUID | None:
    """Safely converts a value to UUID or returns None if invalid."""
    if v is None:
        return None
    if isinstance(v, UUID):
        return v
    try:
        return UUID(str(v))
    except ValueError:
        return None


async def ai_base_exception_handler(request: Request, exc: AiBaseException) -> JSONResponse:
    """Handles all internal typed exceptions from the Dia-Smart AI pipeline."""
    request_id = getattr(request.state, "request_id", None)
    clean_id = safe_uuid(request_id)

    # Log only controlled metadata
    logger.error(
        "Dia-Smart AI controlled error error_code=%s request_id=%s exception_type=%s",
        exc.error_code,
        clean_id,
        type(exc).__name__,
    )

    # Enforce safe client messages by masking internal/raw exception messages
    if exc.error_code == "AI_PROVIDER_ERROR":
        client_message = "The AI provider could not complete the request."
    elif exc.error_code == "AI_RESPONSE_VALIDATION_ERROR":
        client_message = "The AI provider response could not be validated."
    elif exc.error_code == "AI_MEDICAL_SAFETY_REJECTION":
        client_message = "The AI provider response did not pass medical safety checks."
    elif exc.error_code == "AI_EVIDENCE_VALIDATION_ERROR":
        client_message = "The AI provider response contains invalid citations."
    else:
        client_message = exc.message

    error_detail = ErrorDetailResponse(
        error_code=exc.error_code,
        message=client_message,
        request_id=clean_id,
    )

    return JSONResponse(
        status_code=exc.status_code,
        content=error_detail.model_dump(mode="json"),
    )


async def fastapi_validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    """Handles FastAPI/Pydantic validation errors on entry routes."""
    request_id = getattr(request.state, "request_id", None)
    clean_id = safe_uuid(request_id)

    # Format error details for logging/response
    error_messages = []
    for err in exc.errors():
        loc = ".".join(str(x) for x in err["loc"] if x != "body")
        error_messages.append(f"{loc}: {err['msg']}")
    details_str = "; ".join(error_messages)

    logger.error(f"Dia-Smart AI validation error: {details_str}. Request ID: {request_id}")

    error_detail = ErrorDetailResponse(
        error_code="AI_REQUEST_VALIDATION_ERROR",
        message=f"The clinical-summary request is invalid: {details_str}",
        request_id=clean_id,
    )

    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content=error_detail.model_dump(mode="json"),
    )


async def generic_exception_handler(request: Request, exc: Exception) -> JSONResponse:
    """Catch-all handler mapping unexpected python exceptions to AI_INTERNAL_ERROR."""
    request_id = getattr(request.state, "request_id", None)
    clean_id = safe_uuid(request_id)

    # Secure logging contains ONLY controlled metadata (no stack traces or messages)
    logger.error(
        "Dia-Smart AI internal error request_id=%s exception_type=%s",
        clean_id,
        type(exc).__name__,
    )

    error_detail = ErrorDetailResponse(
        error_code="AI_INTERNAL_ERROR",
        message="Internal server error occurred",
        request_id=clean_id,
    )

    # Client response is fully sanitized with no exception details or logs leaked
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content=error_detail.model_dump(mode="json"),
    )
