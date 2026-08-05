import logging
from typing import Any, cast

from fastapi import FastAPI
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
from app.middlewares.request_size import RequestSizeLimitMiddleware
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

# Add request body size limit protection middleware
app.add_middleware(RequestSizeLimitMiddleware, max_size=settings.AI_MAX_REQUEST_BODY_BYTES)

# Register endpoints
app.include_router(health_router)
app.include_router(clinical_summary_router)

# Register centralized exception handlers. We cast to Any to bypass Starlette strict handler variance check.
app.add_exception_handler(AiBaseException, cast(Any, ai_base_exception_handler))
app.add_exception_handler(RequestValidationError, cast(Any, fastapi_validation_exception_handler))
app.add_exception_handler(Exception, cast(Any, generic_exception_handler))


@app.on_event("startup")
async def startup_event() -> None:
    logger.info("Dia-Smart AI service startup checks complete.")
