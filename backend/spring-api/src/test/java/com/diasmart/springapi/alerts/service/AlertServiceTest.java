package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AlertService Tests")
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PatientAccessService patientAccessService;

    @InjectMocks
    private AlertService alertService;

    private AppUser adminUser;
    private AppUser patientUser;
    private Alert testAlert;
    private Pageable testPageable;

    @BeforeEach
    void setUp() {
        // Setup admin user
        adminUser = new AppUser();
        adminUser.setUserId(1L);
        adminUser.setRole(UserRole.ADMIN);
        adminUser.setEmail("admin@example.com");

        // Setup patient user
        patientUser = new AppUser();
        patientUser.setUserId(2L);
        patientUser.setRole(UserRole.PATIENT);
        patientUser.setEmail("patient@example.com");

        // Setup test alert
        testAlert = new Alert();
        testAlert.setAlertId(1L);
        testAlert.setPatientId(1L);
        testAlert.setAlertType("GLUCOSE_HIGH");
        testAlert.setSeverity("MEDIUM");
        testAlert.setTitle("High Blood Glucose");
        testAlert.setMessage("Blood glucose level is abnormally high");
        testAlert.setStatus("PENDING");
        testAlert.setCreatedAt(OffsetDateTime.now());

        testPageable = PageRequest.of(0, 10);
    }

    // =====================================================
    // GET ALERTS FOR CURRENT USER TESTS
    // =====================================================

    @Test
    @DisplayName("Should return all alerts when current user is ADMIN")
    void testGetAlertsForAdmin() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        List<Alert> alerts = Arrays.asList(testAlert);
        Page<Alert> alertPage = new PageImpl<>(alerts, testPageable, 1);
        when(alertRepository.findAll(testPageable)).thenReturn(alertPage);

        // Act
        Page<AlertResponse> response = alertService.getAlerts(testPageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(alertRepository, times(1)).findAll(testPageable);
        verify(patientAccessService, never()).getViewablePatientIdsForCurrentUser();
    }

    @Test
    @DisplayName("Should return only viewable alerts for non-admin user")
    void testGetAlertsForNonAdminUser() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        List<Long> viewablePatientIds = Arrays.asList(1L, 2L);
        when(patientAccessService.getViewablePatientIdsForCurrentUser()).thenReturn(viewablePatientIds);
        
        List<Alert> alerts = Arrays.asList(testAlert);
        Page<Alert> alertPage = new PageImpl<>(alerts, testPageable, 1);
        when(alertRepository.findByPatientIdInOrderByCreatedAtDesc(viewablePatientIds, testPageable))
                .thenReturn(alertPage);

        // Act
        Page<AlertResponse> response = alertService.getAlerts(testPageable);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.getContent().size());
        verify(patientAccessService, times(1)).getViewablePatientIdsForCurrentUser();
    }

    @Test
    @DisplayName("Should return empty page when non-admin user has no viewable patients")
    void testGetAlertsEmptyForNonAdminNoAccess() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(patientAccessService.getViewablePatientIdsForCurrentUser()).thenReturn(Collections.emptyList());

        // Act
        Page<AlertResponse> response = alertService.getAlerts(testPageable);

        // Assert
        assertNotNull(response);
        assertTrue(response.isEmpty());
        verify(alertRepository, never()).findByPatientIdInOrderByCreatedAtDesc(any(), any());
    }

    @Test
    @DisplayName("Should correctly map Alert to AlertResponse")
    void testGetAlertsResponseMapping() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        List<Alert> alerts = Arrays.asList(testAlert);
        Page<Alert> alertPage = new PageImpl<>(alerts, testPageable, 1);
        when(alertRepository.findAll(testPageable)).thenReturn(alertPage);

        // Act
        Page<AlertResponse> response = alertService.getAlerts(testPageable);

        // Assert
        AlertResponse mappedAlert = response.getContent().get(0);
        assertEquals(testAlert.getAlertId(), mappedAlert.getAlertId());
        assertEquals(testAlert.getAlertType(), mappedAlert.getAlertType());
        assertEquals(testAlert.getSeverity(), mappedAlert.getSeverity());
        assertEquals(testAlert.getTitle(), mappedAlert.getTitle());
        assertEquals(testAlert.getMessage(), mappedAlert.getMessage());
    }

    // =====================================================
    // GET LATEST ALERTS FOR PATIENT TESTS
    // =====================================================

    @Test
    @DisplayName("Should retrieve latest alerts for patient with limit")
    void testGetLatestAlertsForPatientSuccess() {
        // Arrange
        List<Alert> alerts = Arrays.asList(testAlert);
        Page<Alert> alertPage = new PageImpl<>(alerts, PageRequest.of(0, 5), 1);
        when(alertRepository.findByPatientIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 5)))
                .thenReturn(alertPage);

        // Act
        List<AlertResponse> response = alertService.getLatestAlertsForPatient(1L, 5);

        // Assert
        assertNotNull(response);
        assertEquals(1, response.size());
        assertEquals(testAlert.getAlertId(), response.get(0).getAlertId());
    }

    @Test
    @DisplayName("Should return empty list when no alerts for patient")
    void testGetLatestAlertsForPatientEmpty() {
        // Arrange
        Page<Alert> emptyPage = new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 5), 0);
        when(alertRepository.findByPatientIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 5)))
                .thenReturn(emptyPage);

        // Act
        List<AlertResponse> response = alertService.getLatestAlertsForPatient(1L, 5);

        // Assert
        assertNotNull(response);
        assertTrue(response.isEmpty());
    }

    @Test
    @DisplayName("Should respect limit parameter")
    void testGetLatestAlertsRespectLimit() {
        // Arrange
        Alert alert1 = new Alert();
        alert1.setAlertId(1L);
        alert1.setPatientId(1L);
        
        Alert alert2 = new Alert();
        alert2.setAlertId(2L);
        alert2.setPatientId(1L);
        
        List<Alert> alerts = Arrays.asList(alert1, alert2);
        Page<Alert> alertPage = new PageImpl<>(alerts, PageRequest.of(0, 10), 2);
        when(alertRepository.findByPatientIdOrderByCreatedAtDesc(1L, PageRequest.of(0, 10)))
                .thenReturn(alertPage);

        // Act
        List<AlertResponse> response = alertService.getLatestAlertsForPatient(1L, 10);

        // Assert
        assertEquals(2, response.size());
    }

    // =====================================================
    // GET SINGLE ALERT TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully retrieve single alert by ID")
    void testGetAlertSuccess() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanViewPatient(testAlert.getPatientId());

        // Act
        AlertResponse response = alertService.getAlert(1L);

        // Assert
        assertNotNull(response);
        assertEquals(testAlert.getAlertId(), response.getAlertId());
    }

    @Test
    @DisplayName("Should throw exception when alert not found")
    void testGetAlertNotFound() {
        // Arrange
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> alertService.getAlert(999L),
                "Alert not found");
    }

    @Test
    @DisplayName("Should verify patient access permission before returning alert")
    void testGetAlertVerifiesAccess() {
        // Arrange
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanViewPatient(testAlert.getPatientId());

        // Act
        alertService.getAlert(1L);

        // Assert
        verify(patientAccessService, times(1)).requireCanViewPatient(testAlert.getPatientId());
    }

    // =====================================================
    // ACKNOWLEDGE ALERT TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully acknowledge alert")
    void testAcknowledgeAlertSuccess() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        
        Alert acknowledgedAlert = new Alert();
        acknowledgedAlert.setAlertId(testAlert.getAlertId());
        acknowledgedAlert.setStatus("ACKNOWLEDGED");
        acknowledgedAlert.setAcknowledgedAt(OffsetDateTime.now());
        acknowledgedAlert.setAcknowledgedBy(patientUser.getUserId());
        
        when(alertRepository.save(any(Alert.class))).thenReturn(acknowledgedAlert);

        // Act
        AlertResponse response = alertService.acknowledgeAlert(1L);

        // Assert
        assertNotNull(response);
        assertEquals("ACKNOWLEDGED", response.getStatus());
        assertNotNull(response.getAcknowledgedAt());
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    @DisplayName("Should throw exception when acknowledging non-existent alert")
    void testAcknowledgeAlertNotFound() {
        // Arrange
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> alertService.acknowledgeAlert(999L),
                "Alert not found");
    }

    @Test
    @DisplayName("Should verify patient access permission before acknowledging alert")
    void testAcknowledgeAlertVerifiesAccess() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        alertService.acknowledgeAlert(1L);

        // Assert
        verify(patientAccessService, times(1)).requireCanAcknowledgeAlerts(testAlert.getPatientId());
    }

    @Test
    @DisplayName("Should set current user as acknowledgedBy when acknowledging alert")
    void testAcknowledgeAlertSetsCurrentUser() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        alertService.acknowledgeAlert(1L);

        // Assert
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(patientUser.getUserId(), alertCaptor.getValue().getAcknowledgedBy());
    }

    // =====================================================
    // RESOLVE ALERT TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully resolve alert with resolution note")
    void testResolveAlertSuccess() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        
        Alert resolvedAlert = new Alert();
        resolvedAlert.setAlertId(testAlert.getAlertId());
        resolvedAlert.setStatus("RESOLVED");
        resolvedAlert.setResolvedAt(OffsetDateTime.now());
        resolvedAlert.setResolvedBy(patientUser.getUserId());
        resolvedAlert.setResolutionNote("Issue resolved");
        
        when(alertRepository.save(any(Alert.class))).thenReturn(resolvedAlert);

        // Act
        AlertResponse response = alertService.resolveAlert(1L, "Issue resolved");

        // Assert
        assertNotNull(response);
        assertEquals("RESOLVED", response.getStatus());
        assertNotNull(response.getResolvedAt());
        verify(alertRepository, times(1)).save(any(Alert.class));
    }

    @Test
    @DisplayName("Should throw exception when resolving non-existent alert")
    void testResolveAlertNotFound() {
        // Arrange
        when(alertRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> alertService.resolveAlert(999L, "Note"),
                "Alert not found");
    }

    @Test
    @DisplayName("Should verify patient access permission before resolving alert")
    void testResolveAlertVerifiesAccess() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        alertService.resolveAlert(1L, "Note");

        // Assert
        verify(patientAccessService, times(1)).requireCanAcknowledgeAlerts(testAlert.getPatientId());
    }

    @Test
    @DisplayName("Should set resolution note when resolving alert")
    void testResolveAlertSetsResolutionNote() {
        // Arrange
        String resolutionNote = "Medication adjusted";
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        alertService.resolveAlert(1L, resolutionNote);

        // Assert
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(resolutionNote, alertCaptor.getValue().getResolutionNote());
    }

    @Test
    @DisplayName("Should set current user as resolvedBy when resolving alert")
    void testResolveAlertSetsCurrentUser() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        alertService.resolveAlert(1L, "Note");

        // Assert
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertEquals(patientUser.getUserId(), alertCaptor.getValue().getResolvedBy());
    }

    // =====================================================
    // EDGE CASES
    // =====================================================

    @Test
    @DisplayName("Should handle alert with null resolved at time")
    void testResolveAlertSetsTimestamp() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        
        Alert savedAlert = new Alert();
        savedAlert.setResolvedAt(OffsetDateTime.now());
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);

        // Act
        alertService.resolveAlert(1L, "Note");

        // Assert
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertNotNull(alertCaptor.getValue().getResolvedAt());
    }

    @Test
    @DisplayName("Should handle null resolution note")
    void testResolveAlertWithNullNote() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(patientUser);
        when(alertRepository.findById(1L)).thenReturn(Optional.of(testAlert));
        doNothing().when(patientAccessService).requireCanAcknowledgeAlerts(testAlert.getPatientId());
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // Act
        alertService.resolveAlert(1L, null);

        // Assert
        ArgumentCaptor<Alert> alertCaptor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(alertCaptor.capture());
        assertNull(alertCaptor.getValue().getResolutionNote());
    }

    @Test
    @DisplayName("Should handle pagination with different page sizes")
    void testGetAlertsWithDifferentPageSizes() {
        // Arrange
        when(currentUserService.getCurrentUser()).thenReturn(adminUser);
        Page<Alert> alertPage = new PageImpl<>(Arrays.asList(testAlert), PageRequest.of(0, 20), 1);
        when(alertRepository.findAll(PageRequest.of(0, 20))).thenReturn(alertPage);

        // Act
        Page<AlertResponse> response = alertService.getAlerts(PageRequest.of(0, 20));

        // Assert
        assertEquals(1, response.getContent().size());
    }
}
