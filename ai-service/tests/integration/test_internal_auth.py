import copy

from fastapi.testclient import TestClient

from app.main import app
from tests.fixtures.clinical_contexts import STABLE_GLUCOSE_PAYLOAD

client = TestClient(app)

# Use configured test token
VALID_TOKEN = "test-handshake-token-of-at-least-32-characters-long"
URL = "/internal/v1/insights/clinical-summary"


def test_missing_auth_header():
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable)
    assert res.status_code == 401

    data = res.json()
    assert data["error_code"] == "AI_UNAUTHORIZED"
    assert "Authorization header is missing" in data["message"]


def test_invalid_auth_scheme():
    headers = {"Authorization": f"Basic {VALID_TOKEN}"}
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=headers)
    assert res.status_code == 401

    data = res.json()
    assert data["error_code"] == "AI_UNAUTHORIZED"
    assert "Bearer scheme required" in data["message"]


def test_empty_token():
    headers = {"Authorization": "Bearer "}
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=headers)
    assert res.status_code == 401

    data = res.json()
    assert data["error_code"] == "AI_UNAUTHORIZED"
    assert "Bearer token is empty" in data["message"]


def test_incorrect_token():
    headers = {"Authorization": "Bearer wrong-token-value-here-which-is-invalid"}
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=headers)
    assert res.status_code == 401

    data = res.json()
    assert data["error_code"] == "AI_UNAUTHORIZED"
    assert "Invalid internal service token" in data["message"]


def test_successful_auth():
    headers = {"Authorization": f"Bearer {VALID_TOKEN}"}
    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=headers)
    assert res.status_code == 200

    # Assert request_id is returned in headers or payload
    data = res.json()
    assert data["request_id"] == stable["request_id"]
