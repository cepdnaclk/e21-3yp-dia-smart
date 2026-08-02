import json
from typing import Any

from app.models.requests import ClinicalSummaryRequest
from app.prompts.clinical_summary_v1 import PROMPT_VERSION, SYSTEM_INSTRUCTION


class StructuredPrompt:
    def __init__(self, system_instruction: str, structured_context: str, prompt_version: str):
        self.system_instruction = system_instruction
        self.structured_context = structured_context
        self.prompt_version = prompt_version


def build_prompt(request: ClinicalSummaryRequest) -> StructuredPrompt:
    if request.prompt_version != PROMPT_VERSION:
        raise ValueError(f"Unsupported prompt version '{request.prompt_version}'")

    # Serialize context safely, sanitizing patient-entered strings
    context_dict: dict[str, Any] = {
        "patient_reference": request.patient_reference,
        "requested_period": {
            "from": request.requested_period.from_.isoformat(),
            "to": request.requested_period.to.isoformat(),
        },
    }

    if request.glucose_summary:
        context_dict["glucose_summary"] = request.glucose_summary.model_dump(mode="json")

    if request.adherence_summary:
        context_dict["adherence_summary"] = request.adherence_summary.model_dump(mode="json")

    if request.storage_summary:
        context_dict["storage_summary"] = request.storage_summary.model_dump(mode="json")

    if request.inventory_summary:
        context_dict["inventory_summary"] = request.inventory_summary.model_dump(mode="json")

    if request.relevant_alerts:
        context_dict["relevant_alerts"] = [a.model_dump(mode="json") for a in request.relevant_alerts]

    if request.selected_events:
        events_list = []
        for e in request.selected_events:
            e_data = e.model_dump(mode="json")
            if e.description:
                # Wrap the untrusted description inside strict boundary markers to isolate it
                e_data["description"] = f"[UNTRUSTED_USER_CONTENT_START] {e.description} [UNTRUSTED_USER_CONTENT_END]"
            events_list.append(e_data)
        context_dict["selected_events"] = events_list

    structured_context_str = json.dumps(context_dict, indent=2)

    return StructuredPrompt(
        system_instruction=SYSTEM_INSTRUCTION,
        structured_context=structured_context_str,
        prompt_version=PROMPT_VERSION,
    )
