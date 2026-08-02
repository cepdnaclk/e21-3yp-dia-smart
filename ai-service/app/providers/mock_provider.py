from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse, Correlation, Observation, ProviderMetadata


class MockProvider:
    """
    Deterministic provider producing consistent mock summaries using request context.
    Does not use network, external APIs, or keys.
    """

    async def generate_clinical_summary(
        self,
        request: ClinicalSummaryRequest,
    ) -> ClinicalSummaryResponse:
        request_id = request.request_id

        observations = []
        correlations = []
        discussion_points = []

        # Check for insufficient context data (total counts)
        total_records = 0
        if request.glucose_summary:
            total_records += request.glucose_summary.reading_count
        if request.storage_summary:
            total_records += request.storage_summary.reading_count
        if request.adherence_summary:
            total_records += request.adherence_summary.scheduled_administrations
        total_records += len(request.relevant_alerts)
        total_records += len(request.selected_events)

        # Define fallback evidence ref to satisfy schema validations
        fallback_ref = "glucose-event:ref-default"
        if request.glucose_summary:
            fallback_ref = request.glucose_summary.evidence_reference
        elif request.adherence_summary:
            fallback_ref = request.adherence_summary.evidence_reference
        elif request.storage_summary:
            fallback_ref = request.storage_summary.evidence_reference
        elif request.inventory_summary:
            fallback_ref = request.inventory_summary.evidence_reference
        elif request.relevant_alerts:
            fallback_ref = request.relevant_alerts[0].evidence_reference
        elif request.selected_events:
            fallback_ref = request.selected_events[0].evidence_reference

        if total_records < 5:
            summary = "The supplied records are limited and do not support a detailed clinical summary."
            observations.append(
                Observation(
                    statement="Only a limited number of records were provided for the selected period.",
                    evidence_references=[fallback_ref],
                )
            )
            uncertainties = ["The records provided are insufficient to form a clinically meaningful observation."]
            discussion_points.append("Consider collecting more frequent readings over a longer duration.")
        else:
            summary = "The selected period contains glucose, administration, storage, inventory, and alert information suitable for review."

            # 1. Glucose
            if request.glucose_summary:
                gs = request.glucose_summary
                observations.append(
                    Observation(
                        statement=f"A total of {gs.reading_count} glucose readings were processed with an average value of {gs.average} {gs.unit}.",
                        evidence_references=[gs.evidence_reference],
                    )
                )
                if gs.high_reading_count > 0:
                    observations.append(
                        Observation(
                            statement=f"{gs.high_reading_count} glucose readings were recorded above the configured high threshold.",
                            evidence_references=[gs.evidence_reference],
                        )
                    )
                if gs.low_reading_count > 0:
                    observations.append(
                        Observation(
                            statement=f"{gs.low_reading_count} glucose readings were recorded below the configured low threshold.",
                            evidence_references=[gs.evidence_reference],
                        )
                    )

            # 2. Adherence
            if request.adherence_summary:
                as_ = request.adherence_summary
                observations.append(
                    Observation(
                        statement=f"Out of {as_.scheduled_administrations} scheduled insulin administrations, {as_.recorded_administrations} were recorded.",
                        evidence_references=[as_.evidence_reference],
                    )
                )
                if as_.delayed_administrations > 0:
                    observations.append(
                        Observation(
                            statement=f"{as_.delayed_administrations} insulin administrations were recorded as delayed relative to the schedule.",
                            evidence_references=[as_.evidence_reference],
                        )
                    )
                if as_.missed_administrations > 0:
                    observations.append(
                        Observation(
                            statement=f"{as_.missed_administrations} insulin administrations were missed during the selected period.",
                            evidence_references=[as_.evidence_reference],
                        )
                    )

            # 3. Storage
            if request.storage_summary:
                ss = request.storage_summary
                excursion_text = "No temperature excursions were recorded." if ss.excursion_count == 0 else f"{ss.excursion_count} temperature excursions were recorded."
                observations.append(
                    Observation(
                        statement=f"Cold storage sensor recorded an average temperature of {ss.average_temperature} {ss.unit}. {excursion_text}",
                        evidence_references=[ss.evidence_reference],
                    )
                )

            # 4. Inventory
            if request.inventory_summary:
                inv = request.inventory_summary
                observations.append(
                    Observation(
                        statement=f"Insulin stock status was recorded as {inv.latest_status} with {inv.latest_estimated_units} units remaining.",
                        evidence_references=[inv.evidence_reference],
                    )
                )

            # 5. Alerts
            if request.relevant_alerts:
                alert_refs = [a.evidence_reference for a in request.relevant_alerts]
                observations.append(
                    Observation(
                        statement=f"{len(request.relevant_alerts)} clinical alerts were logged, highlighting potential events.",
                        evidence_references=alert_refs,
                    )
                )

            # 6. Selected Events
            if request.selected_events:
                event_refs = [e.evidence_reference for e in request.selected_events]
                observations.append(
                    Observation(
                        statement=f"{len(request.selected_events)} individual telemetry events were selected for clinical matching.",
                        evidence_references=event_refs,
                    )
                )

            # 7. Correlations
            if request.glucose_summary and request.adherence_summary:
                gs = request.glucose_summary
                as_ = request.adherence_summary
                if gs.high_reading_count > 0 and as_.delayed_administrations > 0:
                    correlations.append(
                        Correlation(
                            statement="The selected period contains both elevated glucose readings and delayed recorded administrations. This co-occurrence does not establish causation.",
                            confidence="moderate",
                            evidence_references=[gs.evidence_reference, as_.evidence_reference],
                        )
                    )

            if request.glucose_summary and request.relevant_alerts:
                gs = request.glucose_summary
                high_alerts = [a for a in request.relevant_alerts if "HIGH" in a.alert_type or "HIGH" in a.severity]
                if gs.high_reading_count > 0 and high_alerts:
                    correlations.append(
                        Correlation(
                            statement="Elevated glucose readings co-occurred alongside open high-glucose clinical alerts.",
                            confidence="high",
                            evidence_references=[
                                gs.evidence_reference,
                                high_alerts[0].evidence_reference,
                            ],
                        )
                    )

            uncertainties = [
                "The supplied records are not sufficient to determine the medical cause of the observed readings.",
                "Telemetry does not capture external context such as patient diet, stress, physical exercise, or device calibration issues.",
            ]
            discussion_points = [
                "A healthcare professional may review the recorded timing of elevated readings and delayed administrations.",
                "Discuss regular tracking habits and check if device sync schedules are operating correctly.",
            ]

        return ClinicalSummaryResponse(
            request_id=request_id,
            summary=summary,
            observations=observations,
            correlations=correlations,
            uncertainties=uncertainties,
            discussion_points=discussion_points,
            safety_notice=APPROVED_SAFETY_NOTICE,
            provider_metadata=ProviderMetadata(
                provider="mock",
                model="mock-clinical-summary-v1",
                prompt_version="clinical-summary-v1",
            ),
        )
