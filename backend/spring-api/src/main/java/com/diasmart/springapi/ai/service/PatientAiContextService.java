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
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
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
        // 1. Authorize
        authorizationService.authorize(Permission.READ_PATIENT_READINGS, patientId);

        // 2. Resolve Patient
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PATIENT_NOT_FOUND", "Patient not found"));

        UUID requestId = UUID.randomUUID();
        String pseudonymousRef = "patient-ref-" + UUID.randomUUID();

        // 3. Aggregate Glucose
        AiGlucoseSummary glucoseSummary = aggregateGlucose(patient, from, to);

        // 4. Aggregate Adherence
        AiAdherenceSummary adherenceSummary = aggregateAdherence(patientId, from, to);

        // 5. Aggregate Storage
        AiStorageSummary storageSummary = aggregateStorage(patientId, from, to);

        // 6. Aggregate Inventory
        AiInventorySummary inventorySummary = aggregateInventory(patientId, from, to);

        // 7. Verify we have some data
        boolean hasData = glucoseSummary != null || adherenceSummary != null || storageSummary != null || inventorySummary != null;
        if (!hasData) {
            throw new AiInsufficientDataException();
        }

        // 8. Fetch Alerts
        int maxAlerts = aiProperties.getMaxAlerts() <= 0 ? 100 : aiProperties.getMaxAlerts();
        List<Alert> alerts = alertRepository.findByPatientIdAndCreatedAtBetweenOrderByCreatedAtDesc(
                patientId, from, to, PageRequest.of(0, maxAlerts)
        );
        List<AiAlertContext> relevantAlerts = new ArrayList<>();
        for (Alert a : alerts) {
            relevantAlerts.add(new AiAlertContext(
                    "alert:id-" + a.getAlertId(),
                    a.getAlertType(),
                    a.getSeverity(),
                    a.getStatus(),
                    a.getCreatedAt() != null ? a.getCreatedAt() : OffsetDateTime.now()
            ));
        }

        // 9. Build selected events timeline
        List<AiSelectedEvent> selectedEvents = buildSelectedEvents(patient, from, to, alerts);

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
        GlucoseStatsProjection stats = glucoseReadingRepository.getGlucoseStats(patient.getPatientId(), from, to);
        if (stats == null || stats.getCount() == null || stats.getCount() == 0) {
            return null;
        }

        int count = stats.getCount().intValue();
        double average = stats.getAverage();
        double minimum = stats.getMinimum();
        double maximum = stats.getMaximum();

        BigDecimal targetMin = patient.getTargetGlucoseMinMgDl();
        BigDecimal targetMax = patient.getTargetGlucoseMaxMgDl();
        double minThreshold = targetMin != null ? targetMin.doubleValue() : 70.0;
        double maxThreshold = targetMax != null ? targetMax.doubleValue() : 180.0;

        long highCount = glucoseReadingRepository.countHighReadings(patient.getPatientId(), from, to, maxThreshold);
        long lowCount = glucoseReadingRepository.countLowReadings(patient.getPatientId(), from, to, minThreshold);

        // Clamp to prevent logical contradiction from float precision or concurrent modifications
        int high = (int) Math.min(highCount, count);
        int low = (int) Math.min(lowCount, count - high);

        return new AiGlucoseSummary(
                "glucose_summary:selected-period",
                "mg/dL",
                count,
                average,
                minimum,
                maximum,
                high,
                low
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

        // Enforce validations in requests.py cleanly
        int recorded = Math.min(onTime + late, scheduled);
        int delayed = Math.min(late, recorded);
        int missedClamped = Math.min(missed, scheduled - recorded);

        return new AiAdherenceSummary(
                "adherence_summary:selected-period",
                scheduled,
                recorded,
                delayed,
                missedClamped
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

        long excursionCount = storageReadingRepository.countExcursions(patientId, from, to, 2.0, 8.0);
        int excursions = (int) Math.min(excursionCount, count);

        return new AiStorageSummary(
                "storage_summary:selected-period",
                "celsius",
                count,
                average,
                minimum,
                maximum,
                excursions
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

        String status = latest.getInventoryStatus() != null ? latest.getInventoryStatus() : "OK";
        double units = latest.getEstimatedUnitsRemaining() != null ? latest.getEstimatedUnitsRemaining() : 0.0;
        long shortageCount = inventoryReadingRepository.countShortageEvents(patientId, from, to, "LOW", "CRITICAL");

        return new AiInventorySummary(
                "inventory_summary:latest",
                status,
                units,
                (int) shortageCount
        );
    }

    private List<AiSelectedEvent> buildSelectedEvents(Patient patient, OffsetDateTime from, OffsetDateTime to, List<Alert> alerts) {
        List<AiSelectedEvent> events = new ArrayList<>();
        Long patientId = patient.getPatientId();

        // 1. High/Low Glucose Readings
        BigDecimal targetMin = patient.getTargetGlucoseMinMgDl();
        BigDecimal targetMax = patient.getTargetGlucoseMaxMgDl();
        double minThreshold = targetMin != null ? targetMin.doubleValue() : 70.0;
        double maxThreshold = targetMax != null ? targetMax.doubleValue() : 180.0;

        List<GlucoseReading> glucoseReadings = glucoseReadingRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                patientId, from, to, PageRequest.of(0, 50)
        );
        for (GlucoseReading g : glucoseReadings) {
            if (g.getGlucoseValueMgDl() != null) {
                OffsetDateTime ts = g.getMeasuredAt() != null ? g.getMeasuredAt() : OffsetDateTime.now();
                if (g.getGlucoseValueMgDl() > maxThreshold) {
                    events.add(new AiSelectedEvent(
                            "glucose-reading:id-" + g.getGlucoseReadingId(),
                            "GLUCOSE_HIGH",
                            ts,
                            g.getGlucoseValueMgDl(),
                            "mg/dL",
                            "HIGH",
                            "Glucose value: " + g.getGlucoseValueMgDl() + " mg/dL"
                    ));
                } else if (g.getGlucoseValueMgDl() < minThreshold) {
                    events.add(new AiSelectedEvent(
                            "glucose-reading:id-" + g.getGlucoseReadingId(),
                            "GLUCOSE_LOW",
                            ts,
                            g.getGlucoseValueMgDl(),
                            "mg/dL",
                            "LOW",
                            "Glucose value: " + g.getGlucoseValueMgDl() + " mg/dL"
                    ));
                }
            }
        }

        // 2. Storage excursions
        List<StorageReading> storageReadings = storageReadingRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(
                patientId, from, to, PageRequest.of(0, 50)
        );
        for (StorageReading s : storageReadings) {
            if (s.getTemperatureC() != null && (s.getTemperatureC() < 2.0 || s.getTemperatureC() > 8.0)) {
                OffsetDateTime ts = s.getMeasuredAt() != null ? s.getMeasuredAt() : OffsetDateTime.now();
                String type = s.getTemperatureC() < 2.0 ? "TEMP_LOW" : "TEMP_HIGH";
                String status = s.getTemperatureC() < 2.0 ? "LOW" : "HIGH";
                events.add(new AiSelectedEvent(
                        "storage-reading:id-" + s.getStorageReadingId(),
                        type,
                        ts,
                        s.getTemperatureC(),
                        "celsius",
                        status,
                        "Storage temperature: " + s.getTemperatureC() + " °C"
                ));
            }
        }

        // 3. Dose events
        List<DoseEvent> doseEvents = doseEventRepository.findByPatientIdAndInjectedAtBetween(patientId, from, to);
        for (DoseEvent d : doseEvents) {
            if (d.getDoseStatus() != null && !d.getDoseStatus().equals("ON_TIME")) {
                OffsetDateTime ts = d.getInjectedAt() != null ? d.getInjectedAt() : OffsetDateTime.now();
                events.add(new AiSelectedEvent(
                        "dose-event:id-" + d.getDoseEventId(),
                        "DOSE_EVENT",
                        ts,
                        d.getDoseUnits(),
                        "units",
                        d.getDoseStatus(),
                        "Insulin dose: " + d.getDoseUnits() + " units (" + d.getDoseStatus() + ")"
                ));
            }
        }

        // 4. Alerts
        for (Alert a : alerts) {
            OffsetDateTime ts = a.getCreatedAt() != null ? a.getCreatedAt() : OffsetDateTime.now();
            events.add(new AiSelectedEvent(
                    "alert:id-" + a.getAlertId(),
                    "ALERT_" + a.getAlertType(),
                    ts,
                    null,
                    null,
                    a.getSeverity(),
                    "Alert: " + a.getAlertType() + " (" + a.getSeverity() + ")"
            ));
        }

        // Sort by recorded_at ascending
        events.sort(Comparator.comparing(AiSelectedEvent::recordedAt));

        // Limit to maxSelectedEvents
        int maxEvents = aiProperties.getMaxSelectedEvents() <= 0 ? 100 : aiProperties.getMaxSelectedEvents();
        if (events.size() > maxEvents) {
            return events.subList(0, maxEvents);
        }
        return events;
    }
}
