import logging

from fastapi import Request, status
from fastapi.exceptions import RequestValidationError
from fastapi.responses import JSONResponse

from app.exceptions.types import AiBaseException

logger = logging.getLogger("app.exceptions")


async def ai_base_exception_handler(request: Request, exc: AiBaseException) -> JSONResponse:
    """Handles all internal typed exceptions from the AI service pipeline."""
    request_id = getattr(request.state, "request_id", None)

    logger.error(f"Dia-Smart AI controlled error: [{exc.error_code}] - {exc.message}. Request ID: {request_id}")

    return JSONResponse(
        status_code=exc.status_code,
        content={
            "error_code": exc.error_code,
            "message": exc.message,
            "request_id": str(request_id) if request_id else None,
        },
    )


async def fastapi_validation_exception_handler(request: Request, exc: RequestValidationError) -> JSONResponse:
    """Handles FastAPI/Pydantic validation errors on entry routes."""
    request_id = getattr(request.state, "request_id", None)

    # Format error details for logging/response
    error_messages = []
    for err in exc.errors():
        loc = ".".join(str(x) for x in err["loc"] if x != "body")
        error_messages.append(f"{loc}: {err['msg']}")
    details_str = "; ".join(error_messages)

    logger.error(f"Dia-Smart AI validation error: {details_str}. Request ID: {request_id}")

    return JSONResponse(
        status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
        content={
            "error_code": "AI_REQUEST_VALIDATION_ERROR",
            "message": f"The clinical-summary request is invalid: {details_str}",
            "request_id": str(request_id) if request_id else None,
        },
    )


async def generic_exception_handler(request: Request, _exc: Exception) -> JSONResponse:
    """Catch-all handler mapping unexpected python exceptions to AI_INTERNAL_ERROR."""
    request_id = getattr(request.state, "request_id", None)

    # Secure logging contains complete exception info locally
    logger.exception(f"Dia-Smart AI unhandled runtime exception. Request ID: {request_id}")

    # Client response is fully sanitized with no exception details or logs leaked
    return JSONResponse(
        status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
        content={
            "error_code": "AI_INTERNAL_ERROR",
            "message": "Internal server error occurred",
            "request_id": str(request_id) if request_id else None,
        },
    )
