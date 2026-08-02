import os

import pytest

# Inject required environment configurations before application modules boot
os.environ["AI_INTERNAL_SERVICE_TOKEN"] = "test-handshake-token-of-at-least-32-characters-long"
os.environ["AI_PROVIDER"] = "mock"
os.environ["AI_ENVIRONMENT"] = "testing"
os.environ["AI_LOG_LEVEL"] = "WARNING"


@pytest.fixture(autouse=True)
def clean_testing_env():
    """Ensures environment keys remain stable across testing scopes."""
    os.environ["AI_INTERNAL_SERVICE_TOKEN"] = "test-handshake-token-of-at-least-32-characters-long"
    os.environ["AI_PROVIDER"] = "mock"
    os.environ["AI_ENVIRONMENT"] = "testing"
    os.environ["AI_LOG_LEVEL"] = "WARNING"
    yield
