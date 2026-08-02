# Safety-related constants for the clinical summary service

# The exact safety notice disclaimer required in all response contracts
APPROVED_SAFETY_NOTICE = "This AI-generated information is intended for review and does not provide a diagnosis, prescription, insulin-dosage recommendation, or treatment recommendation."

# Supported categories for evidence references
VALID_EVIDENCE_CATEGORIES = {
    "glucose-summary",
    "adherence-summary",
    "storage-summary",
    "inventory-summary",
    "glucose-event",
    "administration-event",
    "storage-event",
    "inventory-event",
    "alert-event",
}
