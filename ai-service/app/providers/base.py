from typing import Protocol

from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse


class AIProvider(Protocol):
    """
    Abstract interface for AI engines supplying clinical summaries.
    Converts external API models and exceptions into internal Dia-Smart structures.
    """

    async def generate_clinical_summary(
        self,
        request: ClinicalSummaryRequest,
    ) -> ClinicalSummaryResponse: ...
