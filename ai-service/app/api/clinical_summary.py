from fastapi import APIRouter, Depends, Request

from app.models.errors import ErrorDetailResponse
from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse
from app.security.internal_auth import verify_internal_token
from app.services.clinical_summary_service import ClinicalSummaryService

router = APIRouter()


@router.post(
    "/internal/v1/insights/clinical-summary",
    response_model=ClinicalSummaryResponse,
    responses={
        413: {"model": ErrorDetailResponse, "description": "Payload Too Large"},
        422: {"model": ErrorDetailResponse, "description": "Request Validation Error"},
        502: {"model": ErrorDetailResponse, "description": "AI Provider or Response Validation Failure"},
        500: {"model": ErrorDetailResponse, "description": "Internal Server Error"},
    },
    tags=["Insights"],
)
async def generate_clinical_summary(
    clinical_request: ClinicalSummaryRequest,
    request: Request,
    _token: str = Depends(verify_internal_token),
) -> ClinicalSummaryResponse:
    """
    Protected endpoint to generate structured clinical insights from raw patient telemetry.
    Requires Bearer Token authentication via internal service-to-service handshake.
    """
    request.state.request_id = clinical_request.request_id
    service = ClinicalSummaryService()
    response = await service.generate_summary(clinical_request)
    return response
