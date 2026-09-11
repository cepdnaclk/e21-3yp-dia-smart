package com.diasmart.springapi.ai.privacy;

import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.GlucoseStatsProjection;
import com.diasmart.springapi.ai.dto.StorageStatsProjection;
import com.diasmart.springapi.ai.dto.gateway.AiClinicalSummaryGatewayRequest;
import com.diasmart.springapi.ai.dto.gateway.AiSelectedEvent;
import com.diasmart.springapi.ai.service.PatientAiContextService;
import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.analytics.dto.AdherenceAnalyticsResponse;
import com.diasmart.springapi.analytics.service.AdherenceAnalyticsService;
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
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class AiSerializationPrivacyTest {

    @Mock private PatientRepository patientRepository;
    @Mock private GlucoseReadingRepository glucoseReadingRepository;
    @Mock private StorageReadingRepository storageReadingRepository;
    @Mock private InventoryReadingRepository inventoryReadingRepository;
    @Mock private AlertRepository alertRepository;
    @Mock private DoseEventRepository doseEventRepository;
    @Mock private DoseScheduleRepository doseScheduleRepository;
    @Mock private AdherenceAnalyticsService adherenceAnalyticsService;
    @Mock private AuthorizationService authorizationService;
    @Mock private AiProperties aiProperties;

    @InjectMocks
    private PatientAiContextService patientAiContextService;

    private ObjectMapper objectMapper;

    private static final Long DISTINCTIVE_PATIENT_ID = 456789L;
    private static final Long DISTINCTIVE_GLUCOSE_ID = 918273645L;
    private static final Long DISTINCTIVE_STORAGE_ID = 782364L;
    private static final Long DISTINCTIVE_DOSE_ID = 55443322L;
    private static final Long DISTINCTIVE_ALERT_ID = 11223344L;
    private static final Long DISTINCTIVE_DEVICE_ID = 88776655L;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        when(aiProperties.getMaxAlerts()).thenReturn(100);
        when(aiProperties.getMaxSelectedEvents()).thenReturn(100);
        when(aiProperties.getPromptVersion()).thenReturn("clinical-summary-v1");
    }

    @Test
    void shouldEnsureDistinctiveDatabaseAndPatientIdsNeverAppearInSerializedJson() throws Exception {
        OffsetDateTime from = OffsetDateTime.parse("2026-07-01T00:00:00Z");
        OffsetDateTime to = OffsetDateTime.parse("2026-07-07T00:00:00Z");

        // 1. Setup Patient
        Patient patient = new Patient();
        patient.setPatientId(DISTINCTIVE_PATIENT_ID);
        patient.setFullName("Distinctive Synthetic User");
        patient.setTargetGlucoseMinMgDl(new BigDecimal("70.0"));
        patient.setTargetGlucoseMaxMgDl(new BigDecimal("180.0"));
        when(patientRepository.findById(DISTINCTIVE_PATIENT_ID)).thenReturn(Optional.of(patient));

        // 2. Glucose stats & reading with distinctive ID
        GlucoseStatsProjection gStats = mock(GlucoseStatsProjection.class);
        when(gStats.getCount()).thenReturn(1L);
        when(gStats.getAverage()).thenReturn(195.0);
        when(gStats.getMinimum()).thenReturn(195.0);
        when(gStats.getMaximum()).thenReturn(195.0);
        when(glucoseReadingRepository.getGlucoseStats(eq(DISTINCTIVE_PATIENT_ID), any(), any())).thenReturn(gStats);
        when(glucoseReadingRepository.countHighReadings(eq(DISTINCTIVE_PATIENT_ID), any(), any(), eq(180.0))).thenReturn(1L);
        when(glucoseReadingRepository.countLowReadings(eq(DISTINCTIVE_PATIENT_ID), any(), any(), eq(70.0))).thenReturn(0L);

        GlucoseReading gReading = new GlucoseReading();
        gReading.setGlucoseReadingId(DISTINCTIVE_GLUCOSE_ID);
        gReading.setPatientId(DISTINCTIVE_PATIENT_ID);
        gReading.setDeviceId(DISTINCTIVE_DEVICE_ID);
        gReading.setGlucoseValueMgDl(195.0);
        gReading.setMeasuredAt(from.plusDays(1));
        when(glucoseReadingRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(eq(DISTINCTIVE_PATIENT_ID), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(gReading));

        // 3. Storage stats & reading with distinctive ID
        StorageStatsProjection sStats = mock(StorageStatsProjection.class);
        when(sStats.getCount()).thenReturn(1L);
        when(sStats.getAverage()).thenReturn(10.5);
        when(sStats.getMinimum()).thenReturn(10.5);
        when(sStats.getMaximum()).thenReturn(10.5);
        when(storageReadingRepository.getStorageStats(eq(DISTINCTIVE_PATIENT_ID), any(), any())).thenReturn(sStats);
        when(storageReadingRepository.countClassifiedReadings(eq(DISTINCTIVE_PATIENT_ID), any(), any(), any())).thenReturn(1L);
        when(storageReadingRepository.countExcursionsByStatus(eq(DISTINCTIVE_PATIENT_ID), any(), any(), any())).thenReturn(1L);

        StorageReading sReading = new StorageReading();
        sReading.setStorageReadingId(DISTINCTIVE_STORAGE_ID);
        sReading.setPatientId(DISTINCTIVE_PATIENT_ID);
        sReading.setDeviceId(DISTINCTIVE_DEVICE_ID);
        sReading.setTemperatureC(10.5);
        sReading.setTemperatureStatus("HIGH");
        sReading.setMeasuredAt(from.plusDays(2));
        when(storageReadingRepository.findByPatientIdAndMeasuredAtBetweenOrderByMeasuredAtDesc(eq(DISTINCTIVE_PATIENT_ID), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(sReading));

        // 4. Dose event with distinctive ID
        DoseEvent dose = new DoseEvent();
        dose.setDoseEventId(DISTINCTIVE_DOSE_ID);
        dose.setPatientId(DISTINCTIVE_PATIENT_ID);
        dose.setDeviceId(DISTINCTIVE_DEVICE_ID);
        dose.setDoseUnits(12.0);
        dose.setDoseStatus("LATE");
        dose.setInjectedAt(from.plusDays(3));
        when(doseEventRepository.findByPatientIdAndInjectedAtBetween(eq(DISTINCTIVE_PATIENT_ID), any(), any()))
                .thenReturn(List.of(dose));

        when(doseScheduleRepository.findByPatientIdAndActiveTrue(DISTINCTIVE_PATIENT_ID))
                .thenReturn(List.of(mock(DoseSchedule.class)));
        AdherenceAnalyticsResponse adh = new AdherenceAnalyticsResponse();
        adh.setPatientId(DISTINCTIVE_PATIENT_ID);
        adh.setStartDate(from.toLocalDate());
        adh.setEndDate(to.toLocalDate());
        adh.setTotalScheduled(10);
        adh.setOnTime(8);
        adh.setLate(1);
        adh.setMissed(1);
        when(adherenceAnalyticsService.getAdherenceAnalytics(eq(DISTINCTIVE_PATIENT_ID), any(), any())).thenReturn(adh);

        // 5. Alert with distinctive ID
        Alert alert = new Alert();
        alert.setAlertId(DISTINCTIVE_ALERT_ID);
        alert.setPatientId(DISTINCTIVE_PATIENT_ID);
        alert.setDeviceId(DISTINCTIVE_DEVICE_ID);
        alert.setAlertType("TEMP_HIGH");
        alert.setSeverity("CRITICAL");
        alert.setStatus("OPEN");
        alert.setCreatedAt(from.plusDays(2));
        when(alertRepository.findByPatientIdAndCreatedAtBetweenOrderByCreatedAtDesc(eq(DISTINCTIVE_PATIENT_ID), any(), any(), any(Pageable.class)))
                .thenReturn(List.of(alert));

        // 6. Inventory
        InventoryReading inv = new InventoryReading();
        inv.setInventoryReadingId(998877L);
        inv.setPatientId(DISTINCTIVE_PATIENT_ID);
        inv.setInventoryStatus("OK");
        inv.setEstimatedUnitsRemaining(250.0);
        when(inventoryReadingRepository.findTopByPatientIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(eq(DISTINCTIVE_PATIENT_ID), any())).thenReturn(inv);

        // Build the gateway request
        AiClinicalSummaryGatewayRequest gatewayRequest = patientAiContextService.buildGatewayRequest(DISTINCTIVE_PATIENT_ID, from, to);

        assertNotNull(gatewayRequest);

        // Serialize to JSON
        String json = objectMapper.writeValueAsString(gatewayRequest);

        // Assert distinctive synthetic database IDs are ABSENT from the entire serialized JSON string
        assertFalse(json.contains(DISTINCTIVE_PATIENT_ID.toString()), "Patient ID must not appear in gateway JSON");
        assertFalse(json.contains(DISTINCTIVE_GLUCOSE_ID.toString()), "Glucose Reading ID must not appear in gateway JSON");
        assertFalse(json.contains(DISTINCTIVE_STORAGE_ID.toString()), "Storage Reading ID must not appear in gateway JSON");
        assertFalse(json.contains(DISTINCTIVE_DOSE_ID.toString()), "Dose Event ID must not appear in gateway JSON");
        assertFalse(json.contains(DISTINCTIVE_ALERT_ID.toString()), "Alert ID must not appear in gateway JSON");
        assertFalse(json.contains(DISTINCTIVE_DEVICE_ID.toString()), "Device ID must not appear in gateway JSON");
        assertFalse(json.contains("Distinctive Synthetic User"), "Patient full name must not appear in gateway JSON");

        // Verify all references are strictly opaque
        assertTrue(gatewayRequest.patientReference().startsWith("patient-ref-"));
        assertEquals("glucose-summary:selected-period", gatewayRequest.glucoseSummary().evidenceReference());
        assertEquals("storage-summary:selected-period", gatewayRequest.storageSummary().evidenceReference());
        assertEquals("adherence-summary:selected-period", gatewayRequest.adherenceSummary().evidenceReference());
        assertEquals("inventory-summary:selected-period", gatewayRequest.inventorySummary().evidenceReference());

        // Verify alert and selected event references follow valid opaque pattern
        assertEquals("alert-event:ref-001", gatewayRequest.relevantAlerts().get(0).evidenceReference());

        for (AiSelectedEvent ev : gatewayRequest.selectedEvents()) {
            assertTrue(ev.evidenceReference().matches("^[a-z]+-event:ref-\\d{3}$"),
                    "Selected event reference must match opaque format, got: " + ev.evidenceReference());
            assertFalse(ev.description().contains(DISTINCTIVE_PATIENT_ID.toString()));
            assertFalse(ev.description().contains(DISTINCTIVE_GLUCOSE_ID.toString()));
            assertFalse(ev.description().contains(DISTINCTIVE_STORAGE_ID.toString()));
            assertFalse(ev.description().contains(DISTINCTIVE_DOSE_ID.toString()));
            assertFalse(ev.description().contains(DISTINCTIVE_ALERT_ID.toString()));
        }
    }
}
