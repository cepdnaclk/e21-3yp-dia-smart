import asyncio
import json
import logging
import time
from typing import Any

import httpx
from google import genai
from google.genai import _transformers, errors, types
from pydantic import ValidationError

try:
    from google.genai._gaos.lib import compat_errors

    API_STATUS_EXCEPTIONS: tuple[type[BaseException], ...] = (compat_errors.APIStatusError,)
except (ImportError, AttributeError):
    API_STATUS_EXCEPTIONS = ()

from app.config.settings import Settings, get_settings
from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.exceptions.types import (
    AiBaseException,
    AiConfigurationError,
    AiProviderError,
    AiResponseValidationError,
    AiUnsupportedPromptVersionError,
)
from app.models.gemini import GeminiClinicalInsightPayload
from app.models.requests import ClinicalSummaryRequest
from app.models.responses import (
    ClinicalSummaryResponse,
    Correlation,
    Observation,
    ProviderMetadata,
)
from app.prompts.prompt_builder import build_prompt

logger = logging.getLogger("app.providers.gemini")


def _clean_gemini_schema(schema: dict[str, Any]) -> dict[str, Any]:
    """Recursively removes fields that Google Gemini REST API Schema protobuf rejects (e.g. additional_properties, title)."""
    cleaned: dict[str, Any] = {}
    for k, v in schema.items():
        if k in ("additional_properties", "additionalProperties", "title"):
            continue
        if isinstance(v, dict):
            cleaned[k] = _clean_gemini_schema(v)
        elif isinstance(v, list):
            cleaned[k] = [_clean_gemini_schema(i) if isinstance(i, dict) else i for i in v]
        else:
            cleaned[k] = v
    return cleaned


class GeminiProvider:
    """
    Production-grade Gemini AI provider for Dia-Smart clinical summary generation.
    Enforces:
    - Official Google Gen AI SDK (google-genai)
    - Google Gen AI Models API (client.aio.models.generate_content) with strict structured JSON schema
    - Server-controlled metadata (request_id, provider, model, prompt_version, safety_notice)
    - Zero tools, zero internet grounding, zero function calling
    - Stateless execution (store=False, no multi-turn, no previous context retention)
    - Sanitized logging and error masking (no leaked credentials, URLs, or clinical PHI)
    """

    def __init__(
        self,
        settings: Settings | None = None,
        client: genai.Client | None = None,
    ) -> None:
        self.settings = settings or get_settings()
        if client is not None:
            self._client = client
        else:
            api_key_val = (
                self.settings.GEMINI_API_KEY.get_secret_value().strip()
                if self.settings.GEMINI_API_KEY and hasattr(self.settings.GEMINI_API_KEY, "get_secret_value")
                else (str(self.settings.GEMINI_API_KEY).strip() if self.settings.GEMINI_API_KEY else "")
            )
            if not api_key_val:
                raise AiConfigurationError("GEMINI_API_KEY is not configured")
            if not self.settings.GEMINI_MODEL or not str(self.settings.GEMINI_MODEL).strip():
                raise AiConfigurationError("GEMINI_MODEL is not configured")
            self._client = genai.Client(api_key=api_key_val)

    async def generate_clinical_summary(
        self,
        request: ClinicalSummaryRequest,
    ) -> ClinicalSummaryResponse:
        start_time = time.monotonic()

        # 1. Build versioned structured prompt
        try:
            structured_prompt = build_prompt(request)
        except ValueError as e:
            raise AiUnsupportedPromptVersionError(str(e)) from e

        # 2. Count context sections for safe operational metrics
        section_count = sum(
            1
            for s in [
                request.glucose_summary,
                request.adherence_summary,
                request.storage_summary,
                request.inventory_summary,
                bool(request.relevant_alerts),
                bool(request.selected_events),
            ]
            if s
        )

        model_name = self.settings.GEMINI_MODEL or "gemini"

        logger.info(
            "Initiating Gemini request request_id=%s provider=gemini model=%s prompt_version=%s context_sections=%d",
            request.request_id,
            model_name,
            request.prompt_version,
            section_count,
        )

        # 3. Generate structured output schema
        # Clean schema strips protobuf-incompatible fields like additional_properties, $defs, title
        t_schema_obj = _transformers.t_schema(None, GeminiClinicalInsightPayload)
        raw_schema = t_schema_obj.to_json_dict() if t_schema_obj is not None else {}
        clean_schema = _clean_gemini_schema(raw_schema)

        output_text: str | None = None

        # 4. Invoke Google Gen AI API via models.generate_content
        try:
            config = types.GenerateContentConfig(
                system_instruction=structured_prompt.system_instruction,
                response_mime_type="application/json",
                response_schema=clean_schema,
                temperature=self.settings.GEMINI_TEMPERATURE,
                tools=None,
            )
            genai_response = await asyncio.wait_for(
                self._client.aio.models.generate_content(
                    model=model_name,
                    contents=structured_prompt.structured_context,
                    config=config,
                ),
                timeout=self.settings.GEMINI_TIMEOUT_SECONDS,
            )
            if hasattr(genai_response, "candidates") and genai_response.candidates:
                candidate = genai_response.candidates[0]
                finish_reason = getattr(candidate, "finish_reason", None)
                if finish_reason and str(finish_reason).upper() in ("SAFETY", "BLOCKLIST", "PROHIBITED_CONTENT", "SPII"):
                    duration = time.monotonic() - start_time
                    logger.error(
                        "Gemini response blocked by safety request_id=%s provider=gemini model=%s finish_reason=%s duration_sec=%.3f",
                        request.request_id,
                        model_name,
                        finish_reason,
                        duration,
                    )
                    raise AiProviderError("The AI provider response was blocked by safety filters.")
            output_text = getattr(genai_response, "text", None)
        except errors.APIError as exc:
            duration = time.monotonic() - start_time
            code = getattr(exc, "code", None)
            logger.error(
                "Gemini API error request_id=%s provider=gemini model=%s status_code=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                code,
                duration,
                type(exc).__name__,
            )
            if code in (401, 403):
                raise AiProviderError("The AI provider authentication or permission failed.") from exc
            if code == 429:
                raise AiProviderError("The AI provider rate limit was exceeded.") from exc
            if code in (500, 502, 503, 504):
                raise AiProviderError("The AI provider service is currently unavailable.") from exc
            raise AiProviderError("The AI provider returned an API error.") from exc
        except API_STATUS_EXCEPTIONS as exc:
            duration = time.monotonic() - start_time
            code = getattr(exc, "status_code", None)
            logger.error(
                "Gemini API status error request_id=%s provider=gemini model=%s status_code=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                code,
                duration,
                type(exc).__name__,
            )
            if code in (401, 403):
                raise AiProviderError("The AI provider authentication or permission failed.") from exc
            if code == 429:
                raise AiProviderError("The AI provider rate limit was exceeded.") from exc
            if code in (500, 502, 503, 504):
                raise AiProviderError("The AI provider service is currently unavailable.") from exc
            raise AiProviderError("The AI provider returned an API error.") from exc
        except (httpx.TimeoutException, TimeoutError) as exc:
            duration = time.monotonic() - start_time
            logger.error(
                "Gemini request timeout request_id=%s provider=gemini model=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                duration,
                type(exc).__name__,
            )
            raise AiProviderError("The AI provider request timed out.") from exc
        except (httpx.NetworkError, ConnectionError) as exc:
            duration = time.monotonic() - start_time
            logger.error(
                "Gemini network connection failure request_id=%s provider=gemini model=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                duration,
                type(exc).__name__,
            )
            raise AiProviderError("Failed to connect to the AI provider.") from exc
        except (errors.ClientError, errors.ServerError) as exc:
            duration = time.monotonic() - start_time
            logger.error(
                "Gemini SDK client/server error request_id=%s provider=gemini model=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                duration,
                type(exc).__name__,
            )
            raise AiProviderError("The AI provider service encountered an error.") from exc
        except AiBaseException:
            raise
        except Exception as exc:
            duration = time.monotonic() - start_time
            logger.error(
                "Gemini unexpected error request_id=%s provider=gemini model=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                duration,
                type(exc).__name__,
            )
            raise AiProviderError("The AI provider could not complete the request.") from exc

        if not output_text or not output_text.strip():
            duration = time.monotonic() - start_time
            logger.error(
                "Gemini request returned empty output request_id=%s provider=gemini model=%s duration_sec=%.3f",
                request.request_id,
                model_name,
                duration,
            )
            raise AiProviderError("The AI provider returned empty output.")

        # 6. Parse structured JSON output
        try:
            raw_payload = json.loads(output_text)
        except (json.JSONDecodeError, ValueError) as exc:
            duration = time.monotonic() - start_time
            logger.error(
                "Gemini response JSON decode failure request_id=%s provider=gemini model=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                duration,
                type(exc).__name__,
            )
            raise AiResponseValidationError("The AI provider output was not valid JSON.") from exc

        if not isinstance(raw_payload, dict):
            raise AiResponseValidationError("The AI provider output must be a JSON object.")

        # 7. Validate with strict internal Pydantic schema
        try:
            payload = GeminiClinicalInsightPayload.model_validate(raw_payload)
        except ValidationError as exc:
            duration = time.monotonic() - start_time
            logger.error(
                "Gemini response schema mismatch request_id=%s provider=gemini model=%s duration_sec=%.3f exception_type=%s",
                request.request_id,
                model_name,
                duration,
                type(exc).__name__,
            )
            raise AiResponseValidationError("The AI provider output did not match the expected schema.") from exc

        # 8. Assemble final ClinicalSummaryResponse with trusted server-controlled fields
        # Note: request_id, provider, model, prompt_version, safety_notice are server-controlled
        response = ClinicalSummaryResponse(
            request_id=request.request_id,
            summary=payload.summary,
            observations=[
                Observation(
                    statement=obs.statement,
                    evidence_references=obs.evidence_references,
                )
                for obs in payload.observations
            ],
            correlations=[
                Correlation(
                    statement=corr.statement,
                    confidence=corr.confidence,
                    evidence_references=corr.evidence_references,
                )
                for corr in payload.correlations
            ],
            uncertainties=payload.uncertainties,
            discussion_points=payload.discussion_points,
            safety_notice=APPROVED_SAFETY_NOTICE,
            provider_metadata=ProviderMetadata(
                provider="gemini",
                model=model_name,
                prompt_version=request.prompt_version,
            ),
        )

        duration = time.monotonic() - start_time
        logger.info(
            "Gemini request completed successfully request_id=%s provider=gemini model=%s duration_sec=%.3f",
            request.request_id,
            model_name,
            duration,
        )

        return response
