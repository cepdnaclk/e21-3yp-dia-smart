from app.constants.safety import APPROVED_SAFETY_NOTICE
from app.models.requests import ClinicalSummaryRequest
from app.models.responses import ClinicalSummaryResponse, Correlation, Observation, ProviderMetadata


class MockProvider:
    """
    Deterministic provider producing context-grounded mock summaries using request context.
    Does not use network, external APIs, or keys.
    """

    async def generate_clinical_summary(
        self,
        request: ClinicalSummaryRequest,
    ) -> ClinicalSummaryResponse:
        request_id = request.request_id

        observations = []
        correlations = []
        uncertainties = []
        discussion_points = []

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

        # Determine if overall context is limited
        has_dense_context = request.inventory_summary is not None or len(request.relevant_alerts) > 0 or len(request.selected_events) > 0
        total_numeric_records = 0
        if request.glucose_summary:
            total_numeric_records += request.glucose_summary.reading_count
        if request.storage_summary:
            total_numeric_records += request.storage_summary.reading_count
        if request.adherence_summary:
            total_numeric_records += request.adherence_summary.scheduled_administrations

        is_limited_overall = (total_numeric_records < 5) and (not has_dense_context)

        if is_limited_overall:
            summary = "The supplied records are limited and do not support a detailed clinical summary."
            observations.append(
                Observation(
                    statement="Only a limited number of records were provided for the selected period.",
                    evidence_references=[fallback_ref],
                )
            )
            uncertainties.append("The records provided are insufficient to form a clinically meaningful observation.")
        else:
            # 1. Build dynamic summary text matching present sections
            present_sections = []
            if request.glucose_summary:
                present_sections.append("glucose")
            if request.adherence_summary:
                present_sections.append("recorded-adherence")
            if request.storage_summary:
                present_sections.append("storage-temperature")
            if request.inventory_summary:
                present_sections.append("inventory")
            if request.relevant_alerts:
                present_sections.append("alert")
            if request.selected_events:
                present_sections.append("telemetry-event")

            if not present_sections:
                summary = "The selected period contains information suitable for review."
            elif len(present_sections) == 1:
                summary = f"The selected period contains {present_sections[0]} information suitable for review."
            elif len(present_sections) == 2:
                summary = f"The selected period contains {present_sections[0]} and {present_sections[1]} information suitable for review."
            else:
                summary = f"The selected period contains {', '.join(present_sections[:-1])}, and {present_sections[-1]} information suitable for review."

            # 2. Process Glucose Summary
            if request.glucose_summary:
                gs = request.glucose_summary
                if gs.reading_count >= 5:
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
                elif gs.reading_count > 0:
                    observations.append(
                        Observation(
                            statement=f"A limited number of glucose readings ({gs.reading_count}) were provided with an average of {gs.average} {gs.unit}.",
                            evidence_references=[gs.evidence_reference],
                        )
                    )
                    uncertainties.append("Glucose readings are too sparse to establish clinical trends.")
                else:
                    observations.append(
                        Observation(
                            statement="Glucose summary section was supplied but contains zero readings.",
                            evidence_references=[gs.evidence_reference],
                        )
                    )
                    uncertainties.append("No glucose readings are available for the selected period.")

            # 3. Process Adherence Summary
            if request.adherence_summary:
                as_ = request.adherence_summary
                if as_.scheduled_administrations >= 5:
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
                elif as_.scheduled_administrations > 0 or as_.recorded_administrations > 0:
                    observations.append(
                        Observation(
                            statement=f"A limited number of insulin administrations ({as_.recorded_administrations} recorded out of {as_.scheduled_administrations} scheduled) were provided.",
                            evidence_references=[as_.evidence_reference],
                        )
                    )
                    uncertainties.append("Scheduled insulin administration records are sparse, limiting adherence analysis.")
                else:
                    observations.append(
                        Observation(
                            statement="Adherence summary section was supplied but contains zero scheduled or recorded administrations.",
                            evidence_references=[as_.evidence_reference],
                        )
                    )
                    uncertainties.append("No scheduled or recorded insulin administrations are available for the selected period.")

            # 4. Process Storage Summary
            if request.storage_summary:
                ss = request.storage_summary
                if ss.reading_count >= 5:
                    excursion_text = "No temperature excursions were recorded." if ss.excursion_count == 0 else f"{ss.excursion_count} temperature excursions were recorded."
                    observations.append(
                        Observation(
                            statement=f"Cold storage sensor recorded an average temperature of {ss.average_temperature} {ss.unit}. {excursion_text}",
                            evidence_references=[ss.evidence_reference],
                        )
                    )
                elif ss.reading_count > 0:
                    excursion_text = "No temperature excursions were recorded." if ss.excursion_count == 0 else f"{ss.excursion_count} temperature excursions were recorded."
                    observations.append(
                        Observation(
                            statement=f"A limited number of storage temperature readings ({ss.reading_count}) were provided with an average of {ss.average_temperature} {ss.unit}. {excursion_text}",
                            evidence_references=[ss.evidence_reference],
                        )
                    )
                    uncertainties.append("Storage readings are too sparse to evaluate temperature stability.")
                else:
                    observations.append(
                        Observation(
                            statement="Storage summary section was supplied but contains zero readings.",
                            evidence_references=[ss.evidence_reference],
                        )
                    )
                    uncertainties.append("No storage readings are available for the selected period.")

            # 5. Process Inventory Summary
            if request.inventory_summary:
                inv = request.inventory_summary
                observations.append(
                    Observation(
                        statement=f"Insulin stock status was recorded as {inv.latest_status} with {inv.latest_estimated_units} units remaining.",
                        evidence_references=[inv.evidence_reference],
                    )
                )

            # 6. Process Alerts
            if request.relevant_alerts:
                alert_refs = [a.evidence_reference for a in request.relevant_alerts]
                observations.append(
                    Observation(
                        statement=f"{len(request.relevant_alerts)} clinical alerts were logged, highlighting potential events.",
                        evidence_references=alert_refs,
                    )
                )

            # 7. Process Selected Events
            if request.selected_events:
                event_refs = [e.evidence_reference for e in request.selected_events]
                observations.append(
                    Observation(
                        statement=f"{len(request.selected_events)} individual telemetry events were selected for clinical matching.",
                        evidence_references=event_refs,
                    )
                )

            # 8. Build Correlations (only if dependencies are present)
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
                high_alerts = [a for a in request.relevant_alerts if "HIGH" in a.alert_type.upper() or "HIGH" in a.severity.upper()]
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

            # 9. Build Uncertainties (guarantee at least one)
            if not uncertainties:
                uncertainties.append("The supplied records are not sufficient to determine the medical cause of the observed readings.")
            uncertainties.append("Telemetry does not capture external context such as patient diet, stress, physical exercise, or device calibration issues.")

        # 10. Build Discussion Points based only on present context blocks
        if request.glucose_summary:
            discussion_points.append("A healthcare professional may review the supplied glucose summary and recorded threshold counts.")
        if request.adherence_summary:
            discussion_points.append("A healthcare professional may review the supplied administration and adherence summary.")
        if request.storage_summary:
            discussion_points.append("Review the supplied storage-temperature summary and recorded excursion count.")
        if request.inventory_summary:
            discussion_points.append("Review the supplied inventory status and shortage-event count.")
        if request.relevant_alerts:
            discussion_points.append("Review the supplied alerts and their recorded statuses.")
        if request.selected_events:
            discussion_points.append("Review the supplied event timeline and associated evidence references.")

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
