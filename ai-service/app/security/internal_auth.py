import hmac

from fastapi import Request

from app.config.settings import get_settings
from app.exceptions.types import AiUnauthorizedError


def verify_internal_token(request: Request) -> str:
    """
    Validates internal service Bearer token authentication directly from raw headers.
    Ensures correct scheme checks and constant-time string comparisons.
    """
    auth_header = request.headers.get("authorization")
    if not auth_header:
        raise AiUnauthorizedError("Authorization header is missing or empty")

    header_parts = auth_header.strip().split(" ", 1)
    scheme = header_parts[0]
    if scheme.lower() != "bearer":
        raise AiUnauthorizedError("Invalid authentication scheme. Bearer scheme required")

    token = header_parts[1].strip() if len(header_parts) > 1 else ""
    if not token:
        raise AiUnauthorizedError("Bearer token is empty or whitespace-only")

    settings = get_settings()
    configured_token = settings.AI_INTERNAL_SERVICE_TOKEN.strip()

    # Constant-time comparison to prevent timing attacks
    if not hmac.compare_digest(token.encode("utf-8"), configured_token.encode("utf-8")):
        raise AiUnauthorizedError("Invalid internal service token")

    return token
