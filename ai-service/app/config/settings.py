from functools import lru_cache

from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    AI_SERVICE_NAME: str = "Dia-Smart AI Service"
    AI_SERVICE_VERSION: str = "0.1.0"
    AI_ENVIRONMENT: str = "development"
    AI_PROVIDER: str = "mock"

    # Internal bear token
    AI_INTERNAL_SERVICE_TOKEN: str = ""

    # Security limits
    AI_MAX_DATE_RANGE_DAYS: int = 31
    AI_MAX_CONTEXT_RECORDS: int = 500
    AI_MAX_SELECTED_EVENTS: int = 100
    AI_MAX_ALERTS: int = 100
    AI_MAX_TEXT_LENGTH: int = 1000
    AI_MAX_REQUEST_BODY_BYTES: int = 1048576

    # Logging
    AI_LOG_LEVEL: str = "INFO"

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    @model_validator(mode="after")
    def validate_settings(self) -> "Settings":
        # Part 2 requirement: only mock provider is allowed
        if self.AI_PROVIDER != "mock":
            raise ValueError("Part 2 supports only 'mock' as the AI provider")

        # Validate internal bearer token length
        token = self.AI_INTERNAL_SERVICE_TOKEN.strip()
        if not token:
            raise ValueError("AI_INTERNAL_SERVICE_TOKEN is missing or empty")
        if len(token) < 32:
            raise ValueError("AI_INTERNAL_SERVICE_TOKEN must be at least 32 characters long")

        # Validate limits
        if self.AI_MAX_REQUEST_BODY_BYTES <= 0:
            raise ValueError("AI_MAX_REQUEST_BODY_BYTES must be greater than 0")
        if self.AI_MAX_DATE_RANGE_DAYS <= 0:
            raise ValueError("AI_MAX_DATE_RANGE_DAYS must be greater than 0")
        if self.AI_MAX_CONTEXT_RECORDS <= 0:
            raise ValueError("AI_MAX_CONTEXT_RECORDS must be greater than 0")
        if self.AI_MAX_SELECTED_EVENTS <= 0:
            raise ValueError("AI_MAX_SELECTED_EVENTS must be greater than 0")
        if self.AI_MAX_ALERTS <= 0:
            raise ValueError("AI_MAX_ALERTS must be greater than 0")
        if self.AI_MAX_TEXT_LENGTH <= 0:
            raise ValueError("AI_MAX_TEXT_LENGTH must be greater than 0")

        # Validate log level
        valid_levels = {"DEBUG", "INFO", "WARNING", "ERROR", "CRITICAL"}
        if self.AI_LOG_LEVEL.upper() not in valid_levels:
            raise ValueError(f"Invalid AI_LOG_LEVEL '{self.AI_LOG_LEVEL}'. Supported: {valid_levels}")

        return self


@lru_cache
def get_settings() -> Settings:
    return Settings()
