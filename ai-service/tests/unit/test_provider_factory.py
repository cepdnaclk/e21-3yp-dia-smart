from unittest.mock import patch

import pytest

from app.config.settings import Settings
from app.exceptions.types import AiConfigurationError, AiUnsupportedProviderError
from app.providers.factory import get_provider
from app.providers.gemini_provider import GeminiProvider
from app.providers.mock_provider import MockProvider


def test_factory_returns_mock_provider_by_default():
    mock_settings = Settings(
        AI_PROVIDER="mock",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
    )
    with patch("app.providers.factory.get_settings", return_value=mock_settings):
        provider = get_provider()
        assert isinstance(provider, MockProvider)


def test_factory_returns_gemini_provider_when_configured():
    gemini_settings = Settings(
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
        GEMINI_API_KEY="test-key-fake-1234567890",
        GEMINI_MODEL="gemini-2.5-flash",
    )
    with patch("app.providers.factory.get_settings", return_value=gemini_settings):
        provider = get_provider()
        assert isinstance(provider, GeminiProvider)
        assert provider.settings.GEMINI_MODEL == "gemini-2.5-flash"


def test_factory_rejects_unsupported_provider():
    # If settings somehow has an unsupported provider
    bad_settings = Settings.model_construct(
        AI_PROVIDER="unsupported_engine",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
    )
    with patch("app.providers.factory.get_settings", return_value=bad_settings):
        with pytest.raises(AiUnsupportedProviderError, match="Unsupported AI provider"):
            get_provider()


def test_mock_provider_requires_no_gemini_key():
    mock_settings = Settings(
        AI_PROVIDER="mock",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
        GEMINI_API_KEY=None,
        GEMINI_MODEL=None,
    )
    with patch("app.providers.factory.get_settings", return_value=mock_settings):
        provider = get_provider()
        assert isinstance(provider, MockProvider)


def test_gemini_missing_config_rejected_in_gemini_mode():
    # Construct settings with missing key/model in gemini mode
    broken_settings = Settings.model_construct(
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="a" * 32,
        GEMINI_API_KEY=None,
        GEMINI_MODEL=None,
    )
    with patch("app.providers.factory.get_settings", return_value=broken_settings):
        with pytest.raises(AiConfigurationError, match="GEMINI_API_KEY is not configured"):
            get_provider()
