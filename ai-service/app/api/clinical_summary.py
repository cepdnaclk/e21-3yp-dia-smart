from fastapi import APIRouter, Depends

from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse
from app.security.internal_auth import verify_internal_token
from app.services.clinical_summary_service import ClinicalSummaryService

router = APIRouter()


@router.post(
    "/internal/v1/insights/clinical-summary",
    response_model=ClinicalSummaryResponse,
    tags=["Insights"],
)
async def generate_clinical_summary(request: ClinicalSummaryRequest, _token: str = Depends(verify_internal_token)) -> ClinicalSummaryResponse:
    """
    Protected endpoint to generate structured clinical insights from raw patient telemetry.
    Requires Bearer Token authentication via internal service-to-service handshake.
    """
    service = ClinicalSummaryService()
    response = await service.generate_summary(request)
    return response
