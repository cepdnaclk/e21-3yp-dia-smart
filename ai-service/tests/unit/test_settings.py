import pytest

from app.config.settings import Settings, get_settings


def test_valid_default_settings():
    settings = get_settings()
    assert settings.AI_SERVICE_NAME == "Dia-Smart AI Service"
    assert settings.AI_PROVIDER == "mock"
    assert len(settings.AI_INTERNAL_SERVICE_TOKEN) >= 32


def test_unsupported_provider():
    # Setting an unknown provider must throw validation error in settings
    with pytest.raises(ValueError, match="Unsupported AI provider"):
        Settings(
            AI_PROVIDER="unsupported-provider",
            AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
        )


def test_gemini_missing_api_key():
    with pytest.raises(ValueError, match="GEMINI_API_KEY is required"):
        Settings(
            AI_PROVIDER="gemini",
            AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
            GEMINI_MODEL="gemini-2.5-flash",
        )


def test_gemini_missing_model():
    with pytest.raises(ValueError, match="GEMINI_MODEL is required"):
        Settings(
            AI_PROVIDER="gemini",
            AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
            GEMINI_API_KEY="test-key-fake-1234567890",
        )


def test_valid_gemini_settings():
    settings = Settings(
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
        GEMINI_API_KEY="test-key-fake-1234567890",
        GEMINI_MODEL="gemini-2.5-flash",
        GEMINI_TIMEOUT_SECONDS=45.0,
        GEMINI_TEMPERATURE=0.5,
    )
    assert settings.AI_PROVIDER == "gemini"
    assert settings.GEMINI_MODEL == "gemini-2.5-flash"
    assert settings.GEMINI_TIMEOUT_SECONDS == 45.0
    assert settings.GEMINI_TEMPERATURE == 0.5
    # Ensure key is masked in repr
    assert "test-key-fake-1234567890" not in repr(settings)


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


def test_production_mock_provider_prohibited():
    with pytest.raises(ValueError, match="MockProvider is not permitted in production"):
        Settings(
            AI_ENVIRONMENT="production",
            AI_PROVIDER="mock",
            AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
        )


def test_production_gemini_provider_allowed():
    settings = Settings(
        AI_ENVIRONMENT="production",
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
        GEMINI_API_KEY="test-key-fake-1234567890",
        GEMINI_MODEL="gemini-2.5-flash",
    )
    assert settings.AI_ENVIRONMENT == "production"
    assert settings.AI_PROVIDER == "gemini"
