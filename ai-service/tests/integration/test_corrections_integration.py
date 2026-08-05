import asyncio
import copy
import json
import logging
from unittest.mock import AsyncMock, patch
from uuid import uuid4

from fastapi.testclient import TestClient

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.main import app
from app.models.responses import ClinicalSummaryResponse, Observation, ProviderMetadata
from tests.fixtures.clinical_contexts import STABLE_GLUCOSE_PAYLOAD

client = TestClient(app)

VALID_TOKEN = "test-handshake-token-of-at-least-32-characters-long"
HEADERS = {"Authorization": f"Bearer {VALID_TOKEN}"}
URL = "/internal/v1/insights/clinical-summary"


# Helper to run direct ASGI request simulations
async def run_asgi_request(
    app_instance,
    method: str,
    path: str,
    headers: list[tuple[bytes, bytes]],
    body_chunks: list[bytes],
):
    response_status = 0
    response_headers = []
    response_body = b""
    chunk_index = 0

    async def mock_receive() -> dict:
        nonlocal chunk_index
        if chunk_index < len(body_chunks):
            chunk = body_chunks[chunk_index]
            chunk_index += 1
            return {
                "type": "http.request",
                "body": chunk,
                "more_body": chunk_index < len(body_chunks),
            }
        return {
            "type": "http.request",
            "body": b"",
            "more_body": False,
        }

    async def mock_send(message: dict) -> None:
        nonlocal response_status, response_headers, response_body
        if message["type"] == "http.response.start":
            response_status = message["status"]
            response_headers = message["headers"]
        elif message["type"] == "http.response.body":
            response_body += message.get("body", b"")

    scope = {
        "type": "http",
        "asgi": {"version": "3.0", "spec_version": "2.0"},
        "method": method,
        "path": path,
        "raw_path": path.encode("utf-8"),
        "query_string": b"",
        "headers": headers,
    }

    try:
        await app_instance(scope, mock_receive, mock_send)
    except Exception:
        # If any unhandled exception leaks through the app, let the caller see it
        pass

    return response_status, response_headers, response_body


# 1. Request Size Limit Enforcement Tests (Direct ASGI and Integration)


def test_request_body_size_limit_exceeded():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES
    payload_str = " " * (limit + 100)

    res = client.post(
        URL,
        content=payload_str,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(payload_str)),
        },
    )
    assert res.status_code == 413
    data = res.json()
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"
    assert data["message"] == "The request body exceeds the permitted size."
    assert data["request_id"] is None


def test_body_below_limit():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    # Construct a valid JSON body padded to be under the limit
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    raw_json = json.dumps(payload)
    padded_payload = raw_json + " " * (limit - len(raw_json) - 100)

    res = client.post(
        URL,
        content=padded_payload,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(padded_payload)),
        },
    )
    assert res.status_code == 200


def test_body_exactly_at_limit():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    raw_json = json.dumps(payload)
    padded_payload = raw_json + " " * (limit - len(raw_json))

    res = client.post(
        URL,
        content=padded_payload,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(padded_payload)),
        },
    )
    assert res.status_code == 200


def test_body_one_byte_above_limit():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    raw_json = json.dumps(payload)
    padded_payload = raw_json + " " * (limit - len(raw_json) + 1)

    res = client.post(
        URL,
        content=padded_payload,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(padded_payload)),
        },
    )
    assert res.status_code == 413
    data = res.json()
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"
    assert data["message"] == "The request body exceeds the permitted size."


def test_missing_content_length_with_valid_body():
    payload = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    raw_json = json.dumps(payload).encode("utf-8")

    # Missing Content-Length header
    headers = [
        (b"host", b"testserver"),
        (b"content-type", b"application/json"),
        (b"authorization", f"Bearer {VALID_TOKEN}".encode()),
    ]
    status_code, _, body = asyncio.run(run_asgi_request(app, "POST", URL, headers, [raw_json]))
    assert status_code == 200


def test_missing_content_length_with_oversized_streamed_body():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    # Missing Content-Length, but actual streamed body is oversized
    headers = [
        (b"host", b"testserver"),
        (b"content-type", b"application/json"),
        (b"authorization", f"Bearer {VALID_TOKEN}".encode()),
    ]
    chunk1 = b" " * (limit - 1000)
    chunk2 = b" " * 2000

    status_code, _, body = asyncio.run(run_asgi_request(app, "POST", URL, headers, [chunk1, chunk2]))
    assert status_code == 413
    data = json.loads(body.decode("utf-8"))
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"


def test_falsely_small_content_length_with_oversized_actual_body():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    # Content-Length says 10, but we stream more than limit
    headers = [
        (b"host", b"testserver"),
        (b"content-type", b"application/json"),
        (b"content-length", b"10"),
        (b"authorization", f"Bearer {VALID_TOKEN}".encode()),
    ]
    chunk1 = b" " * (limit + 50)

    status_code, _, body = asyncio.run(run_asgi_request(app, "POST", URL, headers, [chunk1]))
    assert status_code == 413
    data = json.loads(body.decode("utf-8"))
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"


def test_falsely_large_content_length_with_small_actual_body():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    # Content-Length is larger than limit (early rejection), actual body is small
    headers = [
        (b"host", b"testserver"),
        (b"content-type", b"application/json"),
        (b"content-length", str(limit + 500).encode("utf-8")),
        (b"authorization", f"Bearer {VALID_TOKEN}".encode()),
    ]
    chunk1 = b"short"

    status_code, _, body = asyncio.run(run_asgi_request(app, "POST", URL, headers, [chunk1]))
    assert status_code == 413
    data = json.loads(body.decode("utf-8"))
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"


def test_chunked_oversized_body():

    headers = [
        (b"host", b"testserver"),
        (b"content-type", b"application/json"),
        (b"transfer-encoding", b"chunked"),
        (b"authorization", f"Bearer {VALID_TOKEN}".encode()),
    ]
    # Send multiple small chunks summing up to > limit
    chunks = [b" " * 300000] * 4  # 1.2 MB total
    status_code, _, body = asyncio.run(run_asgi_request(app, "POST", URL, headers, chunks))
    assert status_code == 413
    data = json.loads(body.decode("utf-8"))
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"


def test_oversized_authenticated_request():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    payload_str = " " * (limit + 50)
    res = client.post(
        URL,
        content=payload_str,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(payload_str)),
        },
    )
    assert res.status_code == 413
    data = res.json()
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"


def test_oversized_unauthenticated_request():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    payload_str = " " * (limit + 50)
    res = client.post(
        URL,
        content=payload_str,
        headers={
            "Content-Type": "application/json",
            "Content-Length": str(len(payload_str)),
        },
    )
    # Reject size check before validating auth
    assert res.status_code == 413
    data = res.json()
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"


@patch("app.api.clinical_summary.generate_clinical_summary")
def test_endpoint_not_invoked_after_rejection(mock_route):
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    payload_str = " " * (limit + 50)
    res = client.post(
        URL,
        content=payload_str,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(payload_str)),
        },
    )
    assert res.status_code == 413
    assert not mock_route.called


@patch("app.security.internal_auth.verify_internal_token")
def test_authentication_not_invoked_after_early_rejection(mock_auth):
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    headers = [
        (b"host", b"testserver"),
        (b"content-type", b"application/json"),
        (b"content-length", str(limit + 500).encode("utf-8")),
    ]
    status_code, _, _ = asyncio.run(run_asgi_request(app, "POST", URL, headers, [b" "]))
    assert status_code == 413
    assert not mock_auth.called


def test_no_body_content_returned():
    from app.config.settings import get_settings

    limit = get_settings().AI_MAX_REQUEST_BODY_BYTES

    payload_str = "sensitive-content-should-never-be-leaked" + " " * limit
    res = client.post(
        URL,
        content=payload_str,
        headers={
            "Authorization": f"Bearer {VALID_TOKEN}",
            "Content-Type": "application/json",
            "Content-Length": str(len(payload_str)),
        },
    )
    assert res.status_code == 413
    data = res.json()
    # Check that none of the request body is leaked in response message or errors
    assert "sensitive-content" not in res.text
    assert data["error_code"] == "AI_REQUEST_TOO_LARGE"
    assert data["message"] == "The request body exceeds the permitted size."


# 2. Request-ID Correlation Mismatch Rejection Tests


@patch("app.services.clinical_summary_service.get_provider")
def test_request_id_correlation_mismatch(mock_get_provider):
    stable_req = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)

    mismatched_response = ClinicalSummaryResponse(
        request_id=uuid4(),  # Different UUID
        summary="A safe summary description",
        observations=[
            Observation(
                statement="Glucose average was normal.",
                evidence_references=["glucose-summary:selected-period"],
            )
        ],
        correlations=[],
        uncertainties=["Telemetry is limited."],
        discussion_points=["Contact your doctor."],
        safety_notice=APPROVED_SAFETY_NOTICE,
        provider_metadata=ProviderMetadata(provider="mock", model="mock-clinical-summary-v1", prompt_version="clinical-summary-v1"),
    )

    mock_provider = AsyncMock()
    mock_provider.generate_clinical_summary.return_value = mismatched_response
    mock_get_provider.return_value = mock_provider

    res = client.post(URL, json=stable_req, headers=HEADERS)
    assert res.status_code == 502

    data = res.json()
    assert data["error_code"] == "AI_RESPONSE_VALIDATION_ERROR"
    assert "response could not be validated" in data["message"]


# 3. Error Masking Behavior Tests


@patch("app.services.clinical_summary_service.get_provider")
def test_raw_provider_exception_masked(mock_get_provider):
    mock_provider = AsyncMock()
    mock_provider.generate_clinical_summary.side_effect = RuntimeError("Exception: connection failed to http://gemini.googleapis.com/v1/models/gemini-pro. File: C:\\Users\\secret\\key.txt")
    mock_get_provider.return_value = mock_provider

    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=HEADERS)
    assert res.status_code == 502

    data = res.json()
    assert data["error_code"] == "AI_PROVIDER_ERROR"
    assert "gemini.googleapis.com" not in data["message"]
    assert "C:\\Users\\secret" not in data["message"]
    assert "The AI provider could not complete the request." in data["message"]


@patch("app.services.clinical_summary_service.get_provider")
def test_sensitive_data_leak_prevention(mock_get_provider, caplog):
    caplog.set_level(logging.ERROR)

    mock_provider = AsyncMock()
    sensitive_message = (
        "Connection failed to https://internal.example/private. "
        "File: C:\\secret\\provider\\config.json. "
        "Credentials: api_key=FAKE-SECRET-123, Bearer FAKE-TOKEN, "
        "GEMINI_API_KEY, postgresql://user:password@host/database"
    )
    mock_provider.generate_clinical_summary.side_effect = RuntimeError(sensitive_message)
    mock_get_provider.return_value = mock_provider

    stable = copy.deepcopy(STABLE_GLUCOSE_PAYLOAD)
    res = client.post(URL, json=stable, headers=HEADERS)
    assert res.status_code == 502

    # 1. Verify HTTP response is fully masked
    data = res.json()
    assert data["error_code"] == "AI_PROVIDER_ERROR"
    assert data["message"] == "The AI provider could not complete the request."
    for leak_word in ["https://internal.example", "C:\\secret", "api_key", "Bearer FAKE", "GEMINI_API_KEY", "postgresql://", "password"]:
        assert leak_word not in data["message"]

    # 2. Verify captured logs are sanitized and contain no raw exception text
    for record in caplog.records:
        log_message = record.getMessage()
        for leak_word in ["https://internal.example", "C:\\secret", "api_key", "Bearer FAKE", "GEMINI_API_KEY", "postgresql://", "password"]:
            assert leak_word not in log_message
