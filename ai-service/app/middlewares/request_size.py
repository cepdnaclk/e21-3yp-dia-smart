import json
from typing import Any

from starlette.datastructures import Headers
from starlette.types import ASGIApp, Message, Receive, Scope, Send


class RequestSizeLimitMiddleware:
    """
    ASGI middleware that rejects incoming request payloads if their total size
    exceeds a configured threshold, preventing memory exhaustion.
    """

    def __init__(self, app: ASGIApp, max_size: int):
        self.app = app
        self.max_size = max_size

    async def __call__(self, scope: Scope, receive: Receive, send: Send) -> None:
        if scope["type"] != "http":
            await self.app(scope, receive, send)
            return

        headers = Headers(scope=scope)
        content_length = headers.get("content-length")

        # 1. Enforce size limit via Content-Length header early rejection
        if content_length is not None:
            try:
                length = int(content_length)
                if length > self.max_size:
                    await self._send_error_response(send)
                    return
            except ValueError:
                # Ignore malformed integer headers, stream validation will still guard it
                pass

        # 2. Enforce size limit via stream validation
        bytes_received = 0
        limit_exceeded = False

        async def wrapped_receive() -> Message:
            nonlocal bytes_received, limit_exceeded
            if limit_exceeded:
                return {"type": "http.request", "body": b"", "more_body": False}

            message = await receive()
            if message["type"] == "http.request":
                body = message.get("body", b"")
                bytes_received += len(body)
                if bytes_received > self.max_size:
                    limit_exceeded = True
                    raise RuntimeError("AI_REQUEST_TOO_LARGE")
            return message

        try:
            await self.app(scope, wrapped_receive, send)
        except RuntimeError as exc:
            if str(exc) == "AI_REQUEST_TOO_LARGE":
                await self._send_error_response(send)
            else:
                raise exc

    async def _send_error_response(self, send: Send) -> None:
        response_body = json.dumps({
            "error_code": "AI_REQUEST_TOO_LARGE",
            "message": "Request payload exceeds the maximum allowed limit",
            "request_id": None,
        }).encode("utf-8")

        await send({
            "type": "http.response.start",
            "status": 413,
            "headers": [
                (b"content-type", b"application/json"),
                (b"content-length", str(len(response_body)).encode("utf-8")),
            ],
        })
        await send({
            "type": "http.response.body",
            "body": response_body,
            "more_body": False,
        })
