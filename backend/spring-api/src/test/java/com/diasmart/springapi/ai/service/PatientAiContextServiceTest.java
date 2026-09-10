package com.diasmart.springapi.ai.service;

import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.GlucoseStatsProjection;
import com.diasmart.springapi.ai.dto.StorageStatsProjection;
import com.diasmart.springapi.ai.dto.gateway.*;
import com.diasmart.springapi.ai.exception.AiInsufficientDataException;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.analytics.dto.AdherenceAnalyticsResponse;
import com.diasmart.springapi.analytics.service.AdherenceAnalyticsService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.dose_schedules.entity.DoseSchedule;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PatientAiContextServiceTest {

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
    private PatientAiContextService service;

    private Patient patient;
    private Long patientId = 12L;
    private OffsetDateTime from = OffsetDateTime.now().minusDays(7);
    private OffsetDateTime to = OffsetDateTime.now();

    @BeforeEach
    void setUp() {
        patient = new Patient();
        patient.setPatientId(patientId);
        patient.setFullName("John Doe");
        patient.setTargetGlucoseMinMgDl(new BigDecimal("70.0"));
        patient.setTargetGlucoseMaxMgDl(new BigDecimal("180.0"));
        when(aiProperties.getMaxAlerts()).thenReturn(100);
        when(aiProperties.getMaxSelectedEvents()).thenReturn(100);
        when(aiProperties.getPromptVersion()).thenReturn("clinical-summary-v1");
    }

    @Test
    void shouldAuthorizeBeforeAggregation() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        GlucoseStatsProjection stats = mock(GlucoseStatsProjection.class);
        when(stats.getCount()).thenReturn(1L);
        when(stats.getAverage()).thenReturn(100.0);
        when(stats.getMinimum()).thenReturn(80.0);
        when(stats.getMaximum()).thenReturn(120.0);
        when(glucoseReadingRepository.getGlucoseStats(eq(patientId), eq(from), eq(to))).thenReturn(stats);

        assertDoesNotThrow(() -> service.buildGatewayRequest(patientId, from, to));

        verify(authorizationService).authorize(Permission.READ_PATIENT_READINGS, patientId);
    }

    @Test
    void shouldThrowNotFoundWhenPatientDoesNotExist() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.empty());

        ApiException ex = assertThrows(ApiException.class, () -> service.buildGatewayRequest(patientId, from, to));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("PATIENT_NOT_FOUND", ex.getErrorCode());
    }

    @Test
    void shouldThrowInsufficientDataWhenNoDataAggregated() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        when(glucoseReadingRepository.getGlucoseStats(any(), any(), any())).thenReturn(null);
        when(storageReadingRepository.getStorageStats(any(), any(), any())).thenReturn(null);
        when(inventoryReadingRepository.findTopByPatientIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(any(), any())).thenReturn(null);
        when(inventoryReadingRepository.findTopByPatientIdOrderByMeasuredAtDesc(any())).thenReturn(null);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(any())).thenReturn(Collections.emptyList());

        assertThrows(AiInsufficientDataException.class, () -> service.buildGatewayRequest(patientId, from, to));
    }

    @Test
    void shouldGeneratePseudonymousRefAndOpaqueEvidenceRefs() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        GlucoseStatsProjection stats = mock(GlucoseStatsProjection.class);
        when(stats.getCount()).thenReturn(5L);
        when(stats.getAverage()).thenReturn(115.0);
        when(stats.getMinimum()).thenReturn(72.0);
        when(stats.getMaximum()).thenReturn(150.0);
        when(glucoseReadingRepository.getGlucoseStats(eq(patientId), eq(from), eq(to))).thenReturn(stats);
        when(glucoseReadingRepository.countHighReadings(eq(patientId), eq(from), eq(to), eq(180.0))).thenReturn(1L);
        when(glucoseReadingRepository.countLowReadings(eq(patientId), eq(from), eq(to), eq(70.0))).thenReturn(0L);

        AiClinicalSummaryGatewayRequest request = service.buildGatewayRequest(patientId, from, to);

        assertNotNull(request);
        assertNotNull(request.requestId());
        assertTrue(request.patientReference().startsWith("patient-ref-"));
        assertFalse(request.patientReference().contains("John Doe"));
        assertFalse(request.patientReference().contains("12"));

        assertNotNull(request.glucoseSummary());
        assertEquals("glucose-summary:selected-period", request.glucoseSummary().evidenceReference());
        assertEquals(5, request.glucoseSummary().readingCount());
        assertEquals(1, request.glucoseSummary().highReadingCount());
        assertEquals(0, request.glucoseSummary().lowReadingCount());
    }

    @Test
    void shouldOmitGlucoseSummaryWhenPatientThresholdsMissingNoHardcodedDefaults() {
        // Patient has NO target bounds configured
        patient.setTargetGlucoseMinMgDl(null);
        patient.setTargetGlucoseMaxMgDl(null);
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        // Add inventory data so buildGatewayRequest has some data
        when(inventoryReadingRepository.findTopByPatientIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(eq(patientId), any()))
                .thenReturn(mock(com.diasmart.springapi.inventory.entity.InventoryReading.class));

        AiClinicalSummaryGatewayRequest request = service.buildGatewayRequest(patientId, from, to);

        assertNotNull(request);
        // Prove no 70/180 fallback was applied: glucose summary MUST be omitted
        assertNull(request.glucoseSummary());
    }

    @Test
    void shouldOmitStorageSummaryWhenNoTrustedClassificationExistsNoHardcoded2To8Defaults() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        StorageStatsProjection stats = mock(StorageStatsProjection.class);
        when(stats.getCount()).thenReturn(10L);
        when(stats.getAverage()).thenReturn(4.5);
        when(stats.getMinimum()).thenReturn(3.0);
        when(stats.getMaximum()).thenReturn(6.0);
        when(storageReadingRepository.getStorageStats(eq(patientId), eq(from), eq(to))).thenReturn(stats);

        // Classified count = 0 (no readings with SAFE, LOW, HIGH)
        when(storageReadingRepository.countClassifiedReadings(eq(patientId), eq(from), eq(to), any())).thenReturn(0L);

        // Also stub inventory so request doesn't throw insufficient data
        when(inventoryReadingRepository.findTopByPatientIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(eq(patientId), any()))
                .thenReturn(mock(com.diasmart.springapi.inventory.entity.InventoryReading.class));

        AiClinicalSummaryGatewayRequest request = service.buildGatewayRequest(patientId, from, to);

        assertNotNull(request);
        // Prove no 2-8°C fallback was used: storage summary MUST be omitted when unclassified
        assertNull(request.storageSummary());
    }

    @Test
    void shouldIncludeStorageSummaryWhenTrustedClassificationExists() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));

        StorageStatsProjection stats = mock(StorageStatsProjection.class);
        when(stats.getCount()).thenReturn(10L);
        when(stats.getAverage()).thenReturn(5.0);
        when(stats.getMinimum()).thenReturn(2.5);
        when(stats.getMaximum()).thenReturn(9.0);
        when(storageReadingRepository.getStorageStats(eq(patientId), eq(from), eq(to))).thenReturn(stats);

        when(storageReadingRepository.countClassifiedReadings(eq(patientId), eq(from), eq(to), any())).thenReturn(10L);
        when(storageReadingRepository.countExcursionsByStatus(eq(patientId), eq(from), eq(to), any())).thenReturn(2L);

        AiClinicalSummaryGatewayRequest request = service.buildGatewayRequest(patientId, from, to);

        assertNotNull(request);
        assertNotNull(request.storageSummary());
        assertEquals("storage-summary:selected-period", request.storageSummary().evidenceReference());
        assertEquals(10, request.storageSummary().readingCount());
        assertEquals(2, request.storageSummary().excursionCount());
    }

    @Test
    void shouldOmitAdherenceWhenDataIsInconsistentRatherThanClamping() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(mock(DoseSchedule.class)));

        // Inconsistent response: onTime(8) + late(5) = recorded(13), but scheduled is only 10!
        AdherenceAnalyticsResponse inconsistentResponse = new AdherenceAnalyticsResponse();
        inconsistentResponse.setPatientId(patientId);
        inconsistentResponse.setStartDate(from.toLocalDate());
        inconsistentResponse.setEndDate(to.toLocalDate());
        inconsistentResponse.setTotalScheduled(10);
        inconsistentResponse.setOnTime(8);
        inconsistentResponse.setLate(5); // recorded would be 13 > 10 (scheduled)
        inconsistentResponse.setMissed(2);
        when(adherenceAnalyticsService.getAdherenceAnalytics(eq(patientId), any(), any()))
                .thenReturn(inconsistentResponse);

        // Provide glucose so request has some data
        GlucoseStatsProjection stats = mock(GlucoseStatsProjection.class);
        when(stats.getCount()).thenReturn(2L);
        when(stats.getAverage()).thenReturn(100.0);
        when(stats.getMinimum()).thenReturn(90.0);
        when(stats.getMaximum()).thenReturn(110.0);
        when(glucoseReadingRepository.getGlucoseStats(eq(patientId), eq(from), eq(to))).thenReturn(stats);

        AiClinicalSummaryGatewayRequest request = service.buildGatewayRequest(patientId, from, to);

        assertNotNull(request);
        // Adherence summary MUST be omitted because recorded > scheduled; it must NOT be clamped
        assertNull(request.adherenceSummary());
        assertNotNull(request.glucoseSummary());
    }

    @Test
    void shouldPreserveReliableAdherenceCounts() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(patientId))
                .thenReturn(List.of(mock(DoseSchedule.class)));

        // Valid, consistent adherence metrics: scheduled=10, onTime=7, late=2, missed=1
        AdherenceAnalyticsResponse validResponse = new AdherenceAnalyticsResponse();
        validResponse.setPatientId(patientId);
        validResponse.setStartDate(from.toLocalDate());
        validResponse.setEndDate(to.toLocalDate());
        validResponse.setTotalScheduled(10);
        validResponse.setOnTime(7);
        validResponse.setLate(2);
        validResponse.setMissed(1);
        when(adherenceAnalyticsService.getAdherenceAnalytics(eq(patientId), any(), any()))
                .thenReturn(validResponse);

        AiClinicalSummaryGatewayRequest request = service.buildGatewayRequest(patientId, from, to);

        assertNotNull(request);
        assertNotNull(request.adherenceSummary());
        assertEquals("adherence-summary:selected-period", request.adherenceSummary().evidenceReference());
        assertEquals(10, request.adherenceSummary().scheduledAdministrations());
        assertEquals(9, request.adherenceSummary().recordedAdministrations());
        assertEquals(2, request.adherenceSummary().delayedAdministrations());
        assertEquals(1, request.adherenceSummary().missedAdministrations());
    }
}
