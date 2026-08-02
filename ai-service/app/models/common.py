from typing import Annotated

from pydantic import BeforeValidator

from app.constants.safety import VALID_EVIDENCE_CATEGORIES


def validate_evidence_ref(v: str) -> str:
    if not isinstance(v, str):
        raise ValueError("Evidence reference must be a string")

    if ":" not in v:
        raise ValueError("Evidence reference must follow 'category:opaque-ref' format")

    parts = v.split(":", 1)
    category, suffix = parts[0], parts[1]

    if category not in VALID_EVIDENCE_CATEGORIES:
        raise ValueError(f"Invalid evidence category '{category}'. Supported: {', '.join(sorted(VALID_EVIDENCE_CATEGORIES))}")

    if not suffix or suffix.isspace():
        raise ValueError("Evidence reference suffix cannot be empty or whitespace-only")

    # Check for whitespace, commas, or special chars indicating email/passwords/sensitive text
    if any(c.isspace() or c in ",@;()[]{}" for c in suffix):
        raise ValueError("Evidence reference suffix cannot contain whitespace or special delimiters")

    # Ensure it's not a raw numeric database ID
    if suffix.isdigit():
        raise ValueError("Evidence reference suffix cannot be a raw database numeric identifier")

    return v


# Annotated type reusable in Pydantic models
EvidenceReference = Annotated[str, BeforeValidator(validate_evidence_ref)]
