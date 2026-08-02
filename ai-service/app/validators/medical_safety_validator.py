import re

from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.responses import ClinicalSummaryResponse

# Rejection patterns targeting diagnostic, prescription, dosage, and causation assertions
REJECTION_PHRASES = [
    # 1. Diagnoses
    r"diabetic ketoacidosis",
    r"severe diabetic complication",
    r"suffer(ing|s)? from dka",
    r"has type \d diabetes",
    # 2. Dosage adjustments / calculations
    r"increase (the )?insulin( dose)?",
    r"decrease (the )?insulin( dose)?",
    r"reduce (the )?(evening|morning|afternoon|night)?( insulin)?( dose)?",
    r"adjust (the )?dose",
    r"dose to \d+ units",
    r"dosage recommendation",
    r"calculate(d)? insulin",
    # 3. Treatment / prescription modifications
    r"stop (the )?current medication",
    r"stop taking",
    r"start taking",
    r"start (a )?new medication",
    r"change (the )?prescription",
    r"change (the )?medication",
    r"change (the )?administration schedule",
    r"prescribe a",
    r"i prescribe",
    # 4. Definite causation claims
    r"caused (the )?glucose (spike|increase|drop)",
    r"delayed injection caused",
    r"injection caused",
    r"caused by (the )?delayed",
    r"due to (the )?delayed",
    # 5. Guaranteed clinical outcomes
    r"guarantee(s|d)? stable glucose",
    r"guarantee(s|d)? clinical",
    r"will guarantee",
    # 6. Impersonating a doctor
    r"as your doctor",
    r"i am a doctor",
    r"the ai is a doctor",
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

    # Examine text blocks against regexes
    for block in text_blocks:
        for regex in REJECTION_REGEXES:
            if regex.search(block):
                raise MedicalSafetyRejection(f"Clinical safety violation: phrase matching '{regex.pattern}' was detected. AI generation rejected.")
