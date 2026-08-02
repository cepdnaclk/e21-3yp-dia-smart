import re

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.responses import ClinicalSummaryResponse

# NOTE: Deterministic safety validation reduces risk but cannot guarantee clinical safety.
# A stronger model-output policy layer is required before real Gemini activation.

# Rejection patterns targeting diagnostic, prescription, dosage, treatment, and causation assertions
REJECTION_PHRASES = [
    # 1. Diagnoses
    r"ketoacidosis",
    r"diabetic complication",
    r"appear(s)? to have a complication",
    r"confirm(s)? a diagnosis",
    r"suffer(ing|s)? from dka",
    r"has type \d diabetes",
    # 2. Dosage adjustments / calculations
    r"increase (the )?insulin( dose)?",
    r"decrease (the )?insulin( dose)?",
    r"take \d+ units",
    r"use \d+ units",
    r"raise (your )?insulin",
    r"raise.*unit(s)?",
    r"lower (the )?(evening|morning|afternoon|night|evening)?( insulin)?( dose)?",
    r"adjust (the )?insulin dose",
    r"adjust (the )?dose",
    r"dose to \d+ units",
    r"dosage recommendation",
    r"calculate(d)? insulin",
    r"increase medication",
    r"reduce (the )?dose",
    r"double (the )?dose",
    # 3. Treatment / prescription modifications
    r"begin metformin",
    r"start taking metformin",
    r"stop taking",
    r"start (a )?new medication",
    r"switch to (another|other) medication",
    r"replace (the )?medication",
    r"change (the )?prescription",
    r"change (the )?medication",
    r"change (the )?administration schedule",
    r"modify (the )?treatment plan",
    r"follow (this|my) treatment",
    r"guarantee(s|d)? control",
    r"prescribe a",
    r"i prescribe",
    # 4. Definite causation claims
    r"(late injection|delayed administration|delayed injection|storage conditions|dose timing).*caused",
    r"caused (the )?glucose (spike|increase|drop)",
    r"caused (the )?abnormal",
    r"caused (the )?reading",
    r"explains (the )?glucose",
    r"caused by (the )?delayed",
    r"due to (the )?delayed",
    r"proves.*caused",
    # 5. Guaranteed clinical outcomes
    r"guarantee(s|d)? stable glucose",
    r"guarantee(s|d)? clinical",
    # 6. Impersonating a doctor
    r"as your doctor",
    r"i am a doctor",
    r"the ai is a doctor",
    r"my medical advice",
    r"medical advice is",
    r"clinical physician recommendation",
]

# Compile patterns for fast check
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
