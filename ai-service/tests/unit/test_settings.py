import pytest

from app.config.settings import Settings, get_settings


def test_valid_default_settings():
    settings = get_settings()
    assert settings.AI_SERVICE_NAME == "Dia-Smart AI Service"
    assert settings.AI_PROVIDER == "mock"
    assert len(settings.AI_INTERNAL_SERVICE_TOKEN) >= 32


def test_unsupported_provider():
    # Setting an unsupported provider must throw validation error in settings
    with pytest.raises(ValueError, match="supports only 'mock'"):
        Settings(
            AI_PROVIDER="gemini",
            AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
        )


def test_missing_token():
    with pytest.raises(ValueError, match="missing or empty"):
        Settings(AI_PROVIDER="mock", AI_INTERNAL_SERVICE_TOKEN="")


def test_short_token():
    with pytest.raises(ValueError, match="must be at least 32 characters"):
        Settings(AI_PROVIDER="mock", AI_INTERNAL_SERVICE_TOKEN="too-short")


def test_invalid_log_level():
    with pytest.raises(ValueError, match="Invalid AI_LOG_LEVEL"):
        Settings(
            AI_PROVIDER="mock",
            AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
            AI_LOG_LEVEL="VERBOSE",
        )


def test_invalid_limits():
    with pytest.raises(ValueError, match="must be greater than 0"):
        Settings(
            AI_PROVIDER="mock",
            AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
            AI_MAX_DATE_RANGE_DAYS=0,
        )
