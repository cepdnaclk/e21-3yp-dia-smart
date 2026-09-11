from app.config.settings import get_settings
from app.exceptions.types import AiUnsupportedProviderError
from app.providers.base import AIProvider
from app.providers.gemini_provider import GeminiProvider
from app.providers.mock_provider import MockProvider


def get_provider() -> AIProvider:
    """
    Factory function resolving the configured AI provider.
    Supports 'mock' (default) and 'gemini'.
    """
    settings = get_settings()
    provider_name = settings.AI_PROVIDER.lower().strip()

    if provider_name == "mock":
        return MockProvider()

    if provider_name == "gemini":
        return GeminiProvider(settings=settings)

    raise AiUnsupportedProviderError(f"Unsupported AI provider '{settings.AI_PROVIDER}'. Supported providers: 'mock', 'gemini'.")
