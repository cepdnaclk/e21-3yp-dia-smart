import re

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.responses import ClinicalSummaryResponse

# NOTE: Deterministic safety validation reduces risk but cannot guarantee clinical safety.
# A stronger model-output policy layer is required before real Gemini activation.

# Rejection patterns categorized by risk type
CATEGORIZED_REJECTION_PHRASES = {
    "Diagnosis": [
        r"ketoacidosis",
        r"diabetic complication",
        r"appear(s)? to have a complication",
        r"confirm(s)? a diagnosis",
        r"suffer(ing|s)? from dka",
        r"has type \d diabetes",
        r"diagnosed with",
        r"hyperglycemia",
        r"hypoglycemia",
        r"indicates hypoglycemia",
        r"confirms hyperglycemia",
    ],
    "Dosage": [
        r"take \d+ units",
        r"take [a-z]+ units",
        r"use \d+ units",
        r"use [a-z]+ units",
        r"administer \d+ units",
        r"administer [a-z]+ units",
        r"raise (your )?insulin",
        r"raise.*unit(s)?",
        r"lower (the )?(evening|morning|afternoon|night|evening)?( insulin)?( dose)?",
        r"adjust (the )?insulin dose",
        r"adjust (the )?dose",
        r"dose to \d+ units",
        r"dosage recommendation",
        r"calculate(d)? insulin",
        r"reduce (the )?dose",
        r"double (the )?dose",
        r"increase (your )?dose",
        r"decrease (your )?(evening|morning|afternoon|night|evening)? dose",
        r"add \d+ units",
        r"add [a-z]+ units",
        r"remove \d+ units",
        r"remove [a-z]+ units",
        r"modify (the )?dose",
        r"increase (the )?insulin( dose)?",
        r"decrease (the )?insulin( dose)?",
    ],
    "Medication": [
        r"begin metformin",
        r"start metformin",
        r"begin insulin",
        r"start insulin",
        r"start taking metformin",
        r"stop taking",
        r"start (a )?new medication",
        r"switch to (another|other) medication",
        r"switch medication",
        r"replace (the )?medication",
        r"discontinue.*medication",
    ],
    "Treatment": [
        r"change (the )?prescription",
        r"change (the )?medication",
        r"change (the )?administration schedule",
        r"modify (the )?treatment plan",
        r"follow (this|my) treatment",
        r"guarantee(s|d)? control",
        r"prescribe a",
        r"i prescribe",
        r"increase medication",
    ],
    "Causation": [
        r"led to the glucose spike",
        r"resulted from the delayed injection",
        r"reason for the high reading",
        r"caused the abnormal result",
        r"caused the glucose (spike|increase|drop)",
        r"caused the abnormal",
        r"caused the reading",
        r"explains the glucose",
        r"caused by the delayed",
        r"due to the delayed",
        r"proves.*caused",
        r"storage conditions caused",
    ],
    "Medical impersonation": [
        r"as your doctor",
        r"i am a doctor",
        r"the ai is a doctor",
        r"my medical advice",
        r"medical advice is",
        r"clinical physician recommendation",
    ],
}

# Consolidate all patterns for execution
REJECTION_PHRASES = []
for category_phrases in CATEGORIZED_REJECTION_PHRASES.values():
    REJECTION_PHRASES.extend(category_phrases)

REJECTION_REGEXES = [re.compile(pattern, re.IGNORECASE) for pattern in REJECTION_PHRASES]


class MedicalSafetyRejection(Exception):
    """Exception raised when clinical safety checks are violated."""

    pass


def validate_medical_safety(response: ClinicalSummaryResponse) -> None:
    """
    Scans the response for any clinical safety violations.
    Raises MedicalSafetyRejection if unsafe text patterns are encountered.
    """
    # 1. Verify safety notice is unaltered
    if response.safety_notice != APPROVED_SAFETY_NOTICE:
        raise MedicalSafetyRejection("Missing or modified safety notice disclaimer")

    # 2. Verify uncertainty statements are present
    if not response.uncertainties:
        raise MedicalSafetyRejection("Response must contain at least one uncertainty statement")

    # Gather all text elements to analyze
    text_blocks = []
    text_blocks.append(response.summary)

    for obs in response.observations:
        text_blocks.append(obs.statement)

    for corr in response.correlations:
        text_blocks.append(corr.statement)

    for unc in response.uncertainties:
        text_blocks.append(unc)

    for dp in response.discussion_points:
        text_blocks.append(dp)

    # Examine text blocks against regexes after normalizing case and whitespaces
    for block in text_blocks:
        # Normalize casing, repeated whitespace, and strip common punctuation boundaries
        normalized = " ".join(block.lower().split())
        # Remove punctuation that might act as boundaries but keep words intact
        normalized = re.sub(r"[.,;:!?\(\)\[\]\{\}]", " ", normalized)
        normalized = " ".join(normalized.split())

        for regex in REJECTION_REGEXES:
            if regex.search(normalized):
                raise MedicalSafetyRejection(f"Clinical safety violation: phrase matching '{regex.pattern}' was detected. AI generation rejected.")
