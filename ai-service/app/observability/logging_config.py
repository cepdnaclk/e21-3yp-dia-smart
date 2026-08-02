import logging
import sys

from app.config.settings import get_settings


def setup_logging() -> None:
    settings = get_settings()
    log_level_str = settings.AI_LOG_LEVEL.upper()

    # Resolve standard log level from settings
    log_level = getattr(logging, log_level_str, logging.INFO)

    # Configure root logger
    logging.basicConfig(
        level=log_level,
        format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
        handlers=[logging.StreamHandler(sys.stdout)],
    )

    # Suppress verbose logs from third-party libraries if necessary
    logging.getLogger("uvicorn.access").setLevel(logging.WARNING)

    logger = logging.getLogger("app.observability")
    logger.info(f"Logging initialized with level: {log_level_str}. Service Name: {settings.AI_SERVICE_NAME}")
