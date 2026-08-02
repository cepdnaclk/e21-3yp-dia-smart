import json
import logging
from typing import Any, Awaitable, Callable, Dict, cast
from fastapi import FastAPI, Request, Response
from fastapi.exceptions import RequestValidationError

from app.api.clinical_summary import router as clinical_summary_router
from app.api.health import router as health_router
from app.config.settings import get_settings
from app.exceptions.handlers import (
    ai_base_exception_handler,
    fastapi_validation_exception_handler,
    generic_exception_handler,
)
from app.exceptions.types import AiBaseException
from app.observability.logging_config import setup_logging

# Initialize safe logging configuration
setup_logging()
logger = logging.getLogger("app.main")

settings = get_settings()

app = FastAPI(
    title=settings.AI_SERVICE_NAME,
    version=settings.AI_SERVICE_VERSION,
    description="Dia-Smart IoT Diabetes Management System AI Subsystem",
    docs_url="/docs",  # Default enabled in development; recommended to disable or protect in production
    redoc_url="/redoc",
)

# Register endpoints
app.include_router(health_router)
app.include_router(clinical_summary_router)

# Register centralized exception handlers. We cast to Any to bypass Starlette strict handler variance check.
app.add_exception_handler(AiBaseException, cast(Any, ai_base_exception_handler))
app.add_exception_handler(RequestValidationError, cast(Any, fastapi_validation_exception_handler))
app.add_exception_handler(Exception, cast(Any, generic_exception_handler))


@app.middleware("http")
async def extract_request_id_middleware(
    request: Request,
    call_next: Callable[[Request], Awaitable[Response]]
) -> Response:
    """
    Middleware that intercept POST/PUT request bodies to pre-parse the request_id.
    This ensures that validation failures or internal exceptions are logged
    and returned with the matching client request_id.
    """
    request.state.request_id = None
    content_type = request.headers.get("content-type", "")

    if request.method in ("POST", "PUT", "PATCH") and "application/json" in content_type:
        try:
            body = await request.body()

            # Reset body read pointer for downstream route binding
            async def receive() -> Dict[str, Any]:
                return {"type": "http.request", "body": body, "more_body": False}

            request._receive = receive

            # Attempt to extract request_id safely
            data = json.loads(body)
            if isinstance(data, dict) and "request_id" in data:
                request.state.request_id = data["request_id"]
        except Exception:
            # Let route validators handle JSON/structural parsing errors
            pass

    response = await call_next(request)
    return response


@app.on_event("startup")
async def startup_event() -> None:
    logger.info("Dia-Smart AI service startup checks complete.")
