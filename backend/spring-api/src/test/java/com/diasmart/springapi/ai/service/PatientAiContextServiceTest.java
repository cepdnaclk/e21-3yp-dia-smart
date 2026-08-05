package com.diasmart.springapi.ai.service;

import com.diasmart.springapi.ai.config.AiProperties;
import com.diasmart.springapi.ai.dto.GlucoseStatsProjection;
import com.diasmart.springapi.ai.dto.StorageStatsProjection;
import com.diasmart.springapi.ai.dto.gateway.*;
import com.diasmart.springapi.ai.exception.AiInsufficientDataException;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.analytics.service.AdherenceAnalyticsService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.dose.repository.DoseEventRepository;
import com.diasmart.springapi.dose_schedules.repository.DoseScheduleRepository;
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import com.diasmart.springapi.patients.entity.Patient;
import com.diasmart.springapi.patients.repository.PatientRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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
        lenient().when(aiProperties.getMaxAlerts()).thenReturn(100);
        lenient().when(aiProperties.getMaxSelectedEvents()).thenReturn(100);
    }

    @Test
    void shouldAuthorizeBeforeAggregation() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(aiProperties.getMaxSelectedEvents()).thenReturn(100);

        // Stub stats to return at least something, so no insufficient data exception
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

        // Returns no stats from DB
        when(glucoseReadingRepository.getGlucoseStats(any(), any(), any())).thenReturn(null);
        when(storageReadingRepository.getStorageStats(any(), any(), any())).thenReturn(null);
        when(inventoryReadingRepository.findTopByPatientIdAndMeasuredAtLessThanEqualOrderByMeasuredAtDesc(any(), any())).thenReturn(null);
        when(inventoryReadingRepository.findTopByPatientIdOrderByMeasuredAtDesc(any())).thenReturn(null);
        when(doseScheduleRepository.findByPatientIdAndActiveTrue(any())).thenReturn(Collections.emptyList());

        assertThrows(AiInsufficientDataException.class, () -> service.buildGatewayRequest(patientId, from, to));
    }

    @Test
    void shouldGeneratePseudonymousRefAndEvidenceRefs() {
        when(patientRepository.findById(patientId)).thenReturn(Optional.of(patient));
        when(aiProperties.getMaxSelectedEvents()).thenReturn(100);

        GlucoseStatsProjection stats = mock(GlucoseStatsProjection.class);
        when(stats.getCount()).thenReturn(5L);
        when(stats.getAverage()).thenReturn(115.0);
        when(stats.getMinimum()).thenReturn(72.0);
        when(stats.getMaximum()).thenReturn(150.0);
        when(glucoseReadingRepository.getGlucoseStats(eq(patientId), eq(from), eq(to))).thenReturn(stats);

        AiClinicalSummaryGatewayRequest request = service.buildGatewayRequest(patientId, from, to);

        assertNotNull(request);
        assertNotNull(request.requestId());
        assertTrue(request.patientReference().startsWith("patient-ref-"));
        assertFalse(request.patientReference().contains("John Doe"));
        assertFalse(request.patientReference().contains("12"));

        assertNotNull(request.glucoseSummary());
        assertEquals("glucose_summary:selected-period", request.glucoseSummary().evidenceReference());
        assertEquals(5, request.glucoseSummary().readingCount());
    }
}
