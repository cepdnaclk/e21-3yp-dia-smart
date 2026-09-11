from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse


class EvidenceValidationError(Exception):
    """Exception raised when evidence references fail integrity validation."""

    pass


def collect_request_references(request: ClinicalSummaryRequest) -> set[str]:
    """Extracts all valid evidence references declared in the request context."""
    refs = set()

    if request.glucose_summary:
        refs.add(request.glucose_summary.evidence_reference)
    if request.adherence_summary:
        refs.add(request.adherence_summary.evidence_reference)
    if request.storage_summary:
        refs.add(request.storage_summary.evidence_reference)
    if request.inventory_summary:
        refs.add(request.inventory_summary.evidence_reference)

    for alert in request.relevant_alerts:
        refs.add(alert.evidence_reference)

    for event in request.selected_events:
        refs.add(event.evidence_reference)

    return refs


def validate_evidence(request: ClinicalSummaryRequest, response: ClinicalSummaryResponse) -> None:
    """
    Validates that:
    1. All evidence references returned in the response were present in the request.
    2. Observations contain at least one evidence reference.
    3. Correlations contain at least two evidence references.
    """
    request_refs = collect_request_references(request)

    # Verify observations
    for i, obs in enumerate(response.observations):
        if not obs.evidence_references:
            raise EvidenceValidationError(f"Observation {i} must contain at least one evidence reference.")

        for ref in obs.evidence_references:
            if ref not in request_refs:
                raise EvidenceValidationError(f"Invented evidence reference '{ref}' detected in observation. Not present in request context.")

    # Verify correlations
    for i, corr in enumerate(response.correlations):
        # Correlations typically link multiple data points together (e.g. glucose highs & delayed doses)
        if len(corr.evidence_references) < 2:
            raise EvidenceValidationError(f"Correlation {i} must reference at least two distinct evidence sources.")

        for ref in corr.evidence_references:
            if ref not in request_refs:
                raise EvidenceValidationError(f"Invented evidence reference '{ref}' detected in correlation. Not present in request context.")
