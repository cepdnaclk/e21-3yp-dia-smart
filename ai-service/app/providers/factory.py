from app.config.settings import get_settings
from app.providers.base import AIProvider
from app.providers.mock_provider import MockProvider


def get_provider() -> AIProvider:
    """
    Factory function resolving the configured AI provider.
    Part 2 only supports 'mock' provider.
    """
    settings = get_settings()
    provider_name = settings.AI_PROVIDER.lower().strip()

    if provider_name == "mock":
        return MockProvider()

    # Safeguard against unsupported providers in Part 2
    raise ValueError(f"Unsupported AI provider '{settings.AI_PROVIDER}'. Only 'mock' is supported in Part 2.")
