from fastapi.testclient import TestClient

from app.main import app

client = TestClient(app)


def test_health_success():
    res = client.get("/health")
    assert res.status_code == 200

    data = res.json()
    assert data["status"] == "ok"
    assert data["service"] == "Dia-Smart AI Service"
    assert data["version"] == "0.1.0"
    assert data["provider"] == "mock"
    assert data["prompt_version"] == "clinical-summary-v1"

    # Verify no credentials or env secrets leaked
    assert "token" not in data
    assert "secret" not in data
    assert "env" not in data
    assert "path" not in data
