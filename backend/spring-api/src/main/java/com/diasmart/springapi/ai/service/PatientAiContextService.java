package com.diasmart.springapi.ai.service;

import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.GlucoseStatsProjection;
import com.diasmart.springapi.ai.dto.StorageStatsProjection;
import com.diasmart.springapi.ai.dto.gateway.*;
import com.diasmart.springapi.ai.exception.AiInsufficientDataException;
import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.analytics.dto.AdherenceAnalyticsResponse;
import com.diasmart.springapi.analytics.service.AdherenceAnalyticsService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.dose.entity.DoseEvent;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.glucose.entity.GlucoseReading;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.inventory.entity.InventoryReading;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PatientAiContextService {

    private static final Logger log = LoggerFactory.getLogger(PatientAiContextService.class);

    private static final List<String> TRUSTED_STORAGE_STATUSES = List.of("SAFE", "LOW", "HIGH");
    private static final List<String> STORAGE_EXCURSION_STATUSES = List.of("LOW", "HIGH");

    private final PatientRepository patientRepository;
    private final GlucoseReadingRepository glucoseReadingRepository;
    private final StorageReadingRepository storageReadingRepository;
    private final InventoryReadingRepository inventoryReadingRepository;
    private final AlertRepository alertRepository;
    private final DoseEventRepository doseEventRepository;
    private final DoseScheduleRepository doseScheduleRepository;
    private final AdherenceAnalyticsService adherenceAnalyticsService;
    private final AuthorizationService authorizationService;
    private final AiProperties aiProperties;

    public PatientAiContextService(
            PatientRepository patientRepository,
            GlucoseReadingRepository glucoseReadingRepository,
            StorageReadingRepository storageReadingRepository,
            InventoryReadingRepository inventoryReadingRepository,
            AlertRepository alertRepository,
            DoseEventRepository doseEventRepository,
            DoseScheduleRepository doseScheduleRepository,
            AdherenceAnalyticsService adherenceAnalyticsService,
            AuthorizationService authorizationService,
            AiProperties aiProperties
    ) {
        this.patientRepository = patientRepository;
        this.glucoseReadingRepository = glucoseReadingRepository;
        this.storageReadingRepository = storageReadingRepository;
        this.inventoryReadingRepository = inventoryReadingRepository;
        this.alertRepository = alertRepository;
        this.doseEventRepository = doseEventRepository;
        this.doseScheduleRepository = doseScheduleRepository;
        this.adherenceAnalyticsService = adherenceAnalyticsService;
        this.authorizationService = authorizationService;
        this.aiProperties = aiProperties;
    }

    public AiClinicalSummaryGatewayRequest buildGatewayRequest(
            Long patientId,
            OffsetDateTime from,
            OffsetDateTime to
    ) {
        // 1. Authorize access
        authorizationService.authorize(Permission.READ_PATIENT_READINGS, patientId);

        // 2. Resolve Patient
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", "Patient not found"));

        UUID requestId = UUID.randomUUID();
        String pseudonymousRef = "patient-ref-" + UUID.randomUUID();

        // 3. Aggregate Glucose (trusted patient targets only, no hardcoded clinical fallbacks)
        AiGlucoseSummary glucoseSummary = aggregateGlucose(patient, from, to);

        // 4. Aggregate Adherence (genuine counts without schema-forcing clamping)
        AiAdherenceSummary adherenceSummary = aggregateAdherence(patientId, from, to);

        // 5. Aggregate Storage (trusted classification only, no hardcoded temperature fallbacks)
        AiStorageSummary storageSummary = aggregateStorage(patientId, from, to);

        // 6. Aggregate Inventory
        AiInventorySummary inventorySummary = aggregateInventory(patientId, from, to);

        // 7. Fetch Alerts
        int maxAlerts = aiProperties.getMaxAlerts() <= 0 ? 100 : aiProperties.getMaxAlerts();
        List<Alert> alerts = new ArrayList<>(alertRepository.findByPatientIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                patientId, from, to, PageRequest.of(0, maxAlerts)
        ));

        // 8. Build timeline candidate events
        List<CandidateEvent> candidateEvents = buildCandidateEvents(patient, from, to, alerts);

        // Deterministic sorting of candidate events: recordedAt ascending, then eventType, then category
        candidateEvents.sort(Comparator
                .comparing(CandidateEvent::recordedAt)
                .thenComparing(CandidateEvent::eventType)
                .thenComparing(CandidateEvent::category)
        );

        int maxEvents = aiProperties.getMaxSelectedEvents() <= 0 ? 100 : aiProperties.getMaxSelectedEvents();
        if (candidateEvents.size() > maxEvents) {
            candidateEvents = new ArrayList<>(candidateEvents.subList(0, maxEvents));
        }

        // 9. Assign sequential opaque references across all evidence items (no database IDs)
        int refIndex = 1;

        // Deterministic sorting of alerts
        alerts.sort(Comparator
                .comparing((Alert a) -> a.getCreatedAt() != null ? a.getCreatedAt() : from)
                .thenComparing(Alert::getAlertType)
        );

        List<AiAlertContext> relevantAlerts = new ArrayList<>();
        for (Alert a : alerts) {
            OffsetDateTime recordedAt = a.getCreatedAt() != null ? a.getCreatedAt() : from;
            // Bound to requested period boundaries
            if (recordedAt.isBefore(from)) {
                recordedAt = from;
            } else if (recordedAt.isAfter(to)) {
                recordedAt = to;
            }
            String ref = String.format("alert-event:ref-%03d", refIndex++);
            relevantAlerts.add(new AiAlertContext(
                    ref,
                    a.getAlertType(),
                    a.getSeverity(),
                    a.getStatus(),
                    recordedAt
            ));
        }

        List<AiSelectedEvent> selectedEvents = new ArrayList<>();
        for (CandidateEvent c : candidateEvents) {
            OffsetDateTime recordedAt = c.recordedAt();
            if (recordedAt.isBefore(from)) {
                recordedAt = from;
            } else if (recordedAt.isAfter(to)) {
                recordedAt = to;
            }
            String ref = String.format("%s:ref-%03d", c.category(), refIndex++);
            selectedEvents.add(new AiSelectedEvent(
                    ref,
                    c.eventType(),
                    recordedAt,
                    c.value(),
                    c.unit(),
                    c.status(),
                    c.description()
            ));
        }

        // 10. Verify we have some supported context
        boolean hasData = glucoseSummary != null || adherenceSummary != null || storageSummary != null
                || inventorySummary != null || !relevantAlerts.isEmpty() || !selectedEvents.isEmpty();
        if (!hasData) {
            throw new AiInsufficientDataException();
        }

        AiRequestedPeriod requestedPeriod = new AiRequestedPeriod(from, to);

        return new AiClinicalSummaryGatewayRequest(
                requestId,
                "CLINICAL_SUMMARY",
                aiProperties.getPromptVersion(),
                pseudonymousRef,
                requestedPeriod,
                glucoseSummary,
                adherenceSummary,
                storageSummary,
                inventorySummary,
                relevantAlerts,
                selectedEvents
        );
    }

    private AiGlucoseSummary aggregateGlucose(Patient patient, OffsetDateTime from, OffsetDateTime to) {
        BigDecimal targetMin = patient.getTargetGlucoseMinMgDl();
        BigDecimal targetMax = patient.getTargetGlucoseMaxMgDl();
        if (targetMin == null || targetMax == null) {
            log.info("Patient-specific glucose thresholds not configured for patient {}. Omission of glucose summary to prevent unverified clinical inferences.", patient.getPatientId());
            return null;
        }

        double minThreshold = targetMin.doubleValue();
        double maxThreshold = targetMax.doubleValue();
        if (minThreshold >= maxThreshold) {
            log.warn("Configured glucose thresholds inconsistent (min {} >= max {}) for patient {}. Omitting glucose summary.", minThreshold, maxThreshold, patient.getPatientId());
            return null;
        }

        GlucoseStatsProjection stats = glucoseReadingRepository.getGlucoseStats(patient.getPatientId(), from, to);
        if (stats == null || stats.getCount() == null || stats.getCount() == 0) {
            return null;
        }

        int count = stats.getCount().intValue();
        double average = stats.getAverage();
        double minimum = stats.getMinimum();
        double maximum = stats.getMaximum();

        if (!(minimum <= average && average <= maximum)) {
            log.warn("Glucose statistics contradiction (min <= avg <= max violated) for patient {}. Omitting glucose summary.", patient.getPatientId());
            return null;
        }

        long highCount = glucoseReadingRepository.countHighReadings(patient.getPatientId(), from, to, maxThreshold);
        long lowCount = glucoseReadingRepository.countLowReadings(patient.getPatientId(), from, to, minThreshold);

        if (highCount + lowCount > count) {
            log.warn("Glucose reading counts exceed total readings (high {} + low {} > total {}) for patient {}. Omitting glucose summary.", highCount, lowCount, count, patient.getPatientId());
            return null;
        }

        return new AiGlucoseSummary(
                "glucose-summary:selected-period",
                "mg/dL",
                count,
                average,
                minimum,
                maximum,
                (int) highCount,
                (int) lowCount
        );
    }

    private AiAdherenceSummary aggregateAdherence(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        List<DoseSchedule> activeSchedules = doseScheduleRepository.findByPatientIdAndActiveTrue(patientId);
        if (activeSchedules.isEmpty()) {
            return null;
        }

        LocalDate startDate = from.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();
        LocalDate endDate = to.atZoneSameInstant(ZoneOffset.UTC).toLocalDate();

        AdherenceAnalyticsResponse response = adherenceAnalyticsService.getAdherenceAnalytics(patientId, startDate, endDate);
        if (response == null) {
            return null;
        }

        int scheduled = response.getTotalScheduled();
        int late = response.getLate();
        int missed = response.getMissed();
        int onTime = response.getOnTime();
        int recorded = onTime + late;
        int delayed = late;

        // Verify adherence consistency without clamping
        if (scheduled <= 0 || recorded < 0 || delayed < 0 || missed < 0
                || recorded > scheduled
                || delayed > recorded
                || missed > scheduled
                || (delayed + missed) > scheduled) {
            log.warn("ADHERENCE_CONTEXT_OMITTED_INCONSISTENT_DATA for patient {}", patientId);
            return null;
        }

        return new AiAdherenceSummary(
                "adherence-summary:selected-period",
                scheduled,
                recorded,
                delayed,
                missed
        );
    }

    private AiStorageSummary aggregateStorage(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        StorageStatsProjection stats = storageReadingRepository.getStorageStats(patientId, from, to);
        if (stats == null || stats.getCount() == null || stats.getCount() == 0) {
            return null;
        }

        int count = stats.getCount().intValue();
        double average = stats.getAverage();
        double minimum = stats.getMinimum();
        double maximum = stats.getMaximum();

        if (!(minimum <= average && average <= maximum)) {
            log.warn("Storage statistics contradiction (min <= avg <= max violated) for patient {}. Omitting storage summary.", patientId);
            return null;
        }

        long classifiedCount = storageReadingRepository.countClassifiedReadings(patientId, from, to, TRUSTED_STORAGE_STATUSES);
        if (classifiedCount == 0) {
            log.info("No trusted storage classification exists for patient {} in selected period. Omitting storage summary.", patientId);
            return null;
        }

        long excursionCount = storageReadingRepository.countExcursionsByStatus(patientId, from, to, STORAGE_EXCURSION_STATUSES);
        if (excursionCount > count) {
            log.warn("Storage excursion count exceeds total readings for patient {}. Omitting storage summary.", patientId);
            return null;
        }

        return new AiStorageSummary(
                "storage-summary:selected-period",
                "celsius",
                count,
                average,
                minimum,
                maximum,
                (int) excursionCount
        );
    }

    private AiInventorySummary aggregateInventory(Long patientId, OffsetDateTime from, OffsetDateTime to) {
        InventoryReading latest = inventoryReadingRepository.findTopByPatientIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(patientId, to);
        if (latest == null) {
            latest = inventoryReadingRepository.findTopByPatientIdOrderByMeasuredAtDesc(patientId);
        }
        if (latest == null) {
            return null;
        }

        String status = latest.getInventoryStatus() != null && !latest.getInventoryStatus().isBlank() ? latest.getInventoryStatus() : "OK";
        double units = latest.getEstimatedUnitsRemaining() != null ? latest.getEstimatedUnitsRemaining() : 0.0;
        long shortageCount = inventoryReadingRepository.countShortageEvents(patientId, from, to, "LOW", "CRITICAL");

        return new AiInventorySummary(
                "inventory-summary:selected-period",
                status,
                units,
                (int) shortageCount
        );
    }

    private record CandidateEvent(
            String category,
            String eventType,
            OffsetDateTime recordedAt,
            Double value,
            String unit,
            String status,
            String description
    ) {}

    private List<CandidateEvent> buildCandidateEvents(Patient patient, OffsetDateTime from, OffsetDateTime to, List<Alert> alerts) {
        List<CandidateEvent> candidates = new ArrayList<>();
        Long patientId = patient.getPatientId();

        // 1. High/Low Glucose Readings (only if patient has valid configured thresholds)
        BigDecimal targetMin = patient.getTargetGlucoseMinMgDl();
        BigDecimal targetMax = patient.getTargetGlucoseMaxMgDl();
        if (targetMin != null && targetMax != null && targetMin.compareTo(targetMax) < 0) {
            double minThreshold = targetMin.doubleValue();
            double maxThreshold = targetMax.doubleValue();

            List<GlucoseReading> glucoseReadings = glucoseReadingRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                    patientId, from, to, PageRequest.of(0, 50)
            );
            for (GlucoseReading g : glucoseReadings) {
                if (g.getGlucoseValueMgDl() != null) {
                    OffsetDateTime ts = g.getMeasuredAt() != null ? g.getMeasuredAt() : from;
                    if (g.getGlucoseValueMgDl() > maxThreshold) {
                        candidates.add(new CandidateEvent(
                                "glucose-event",
                                "GLUCOSE_HIGH",
                                ts,
                                g.getGlucoseValueMgDl(),
                                "mg/dL",
                                "HIGH",
                                "Glucose reading above target range: " + g.getGlucoseValueMgDl() + " mg/dL"
                        ));
                    } else if (g.getGlucoseValueMgDl() < minThreshold) {
                        candidates.add(new CandidateEvent(
                                "glucose-event",
                                "GLUCOSE_LOW",
                                ts,
                                g.getGlucoseValueMgDl(),
                                "mg/dL",
                                "LOW",
                                "Glucose reading below target range: " + g.getGlucoseValueMgDl() + " mg/dL"
                        ));
                    }
                }
            }
        }

        // 2. Storage excursions (based strictly on trusted existing temperatureStatus classification)
        List<StorageReading> storageReadings = storageReadingRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                patientId, from, to, PageRequest.of(0, 50)
        );
        for (StorageReading s : storageReadings) {
            String status = s.getTemperatureStatus();
            if (status != null && STORAGE_EXCURSION_STATUSES.contains(status.toUpperCase())) {
                OffsetDateTime ts = s.getMeasuredAt() != null ? s.getMeasuredAt() : from;
                String type = "LOW".equalsIgnoreCase(status) ? "TEMP_LOW" : "TEMP_HIGH";
                candidates.add(new CandidateEvent(
                        "storage-event",
                        type,
                        ts,
                        s.getTemperatureC(),
                        "celsius",
                        status.toUpperCase(),
                        "Storage temperature excursion: " + s.getTemperatureC() + " °C (" + status.toUpperCase() + ")"
                ));
            }
        }

        // 3. Dose events (non ON_TIME doses)
        List<DoseEvent> doseEvents = doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, from, to);
        for (DoseEvent d : doseEvents) {
            if (d.getDoseStatus() != null && !d.getDoseStatus().equalsIgnoreCase("ON_TIME")) {
                OffsetDateTime ts = d.getInjectedAt() != null ? d.getInjectedAt() : from;
                candidates.add(new CandidateEvent(
                        "administration-event",
                        "DOSE_EVENT",
                        ts,
                        d.getDoseUnits(),
                        "units",
                        d.getDoseStatus(),
                        "Insulin administration: " + d.getDoseUnits() + " units (" + d.getDoseStatus() + ")"
                ));
            }
        }

        // 4. Alerts
        for (Alert a : alerts) {
            OffsetDateTime ts = a.getCreatedAt() != null ? a.getCreatedAt() : from;
            candidates.add(new CandidateEvent(
                    "alert-event",
                    "ALERT_" + a.getAlertType(),
                    ts,
                    null,
                    null,
                    a.getSeverity(),
                    "Clinical alert: " + a.getAlertType() + " (" + a.getSeverity() + ")"
            ));
        }

        return candidates;
    }
}
