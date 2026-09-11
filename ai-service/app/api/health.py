from fastapi import APIRouter

from app.config.settings import get_settings

router = APIRouter()


@router.get("/health", tags=["Monitoring"])
async def get_health() -> dict[str, str]:
    """
    Public health check endpoint.
    Exposes service health status without disclosing internal credentials or secrets.
    """
    settings = get_settings()
    return {
        "status": "ok",
        "service": settings.AI_SERVICE_NAME,
        "version": settings.AI_SERVICE_VERSION,
        "provider": settings.AI_PROVIDER,
        "prompt_version": "clinical-summary-v1",
    }
