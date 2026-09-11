from fastapi.testclient import TestClient

from app.config.settings import Settings
from app.main import app


def test_development_docs_enabled():
    """In development mode, interactive docs and OpenAPI schema are accessible."""
    client = TestClient(app)
    res_docs = client.get("/docs")
    assert res_docs.status_code == 200

    res_openapi = client.get("/openapi.json")
    assert res_openapi.status_code == 200


def test_production_docs_disabled(monkeypatch):
    """In production mode, /docs, /redoc, and /openapi.json must be disabled (404)."""
    # Create production settings
    prod_settings = Settings(
        AI_ENVIRONMENT="production",
        AI_PROVIDER="gemini",
        AI_INTERNAL_SERVICE_TOKEN="some-random-handshake-token-32-chars-long",
        GEMINI_API_KEY="test-key-fake-1234567890",
        GEMINI_MODEL="gemini-2.5-flash",
    )
    monkeypatch.setattr("app.config.settings.get_settings", lambda: prod_settings)
    monkeypatch.setattr("app.api.health.get_settings", lambda: prod_settings)
    monkeypatch.setattr("app.main.settings", prod_settings)

    # Re-instantiate app under production configuration
    from fastapi import FastAPI

    from app.api.clinical_summary import router as clinical_summary_router
    from app.api.health import router as health_router

    is_production = prod_settings.AI_ENVIRONMENT.lower().strip() in {"production", "prod"}
    prod_app = FastAPI(
        title=prod_settings.AI_SERVICE_NAME,
        version=prod_settings.AI_SERVICE_VERSION,
        docs_url=None if is_production else "/docs",
        redoc_url=None if is_production else "/redoc",
        openapi_url=None if is_production else "/openapi.json",
    )
    prod_app.include_router(health_router)
    prod_app.include_router(clinical_summary_router)

    prod_client = TestClient(prod_app)

    # Verify docs endpoints return 404
    assert prod_client.get("/docs").status_code == 404
    assert prod_client.get("/redoc").status_code == 404
    assert prod_client.get("/openapi.json").status_code == 404

    # Verify health endpoint still works cleanly in production
    res_health = prod_client.get("/health")
    assert res_health.status_code == 200
    data = res_health.json()
    assert data["status"] == "ok"
    assert data["provider"] == "gemini"
    assert "token" not in data
    assert "test-key" not in data
