package com.diasmart.springapi.dashboard.service;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.alerts.service.AlertService;
import com.diasmart.springapi.dashboard.dto.DashboardSummaryResponse;
import com.diasmart.springapi.dashboard.service.bridge.DoseServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.GlucoseServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.InventoryServiceBridge;
import com.diasmart.springapi.dashboard.service.bridge.StorageServiceBridge;
import com.diasmart.springapi.dose.dto.DoseEventResponse;
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponse;
import com.diasmart.springapi.inventory.dto.InventoryReadingResponse;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.storage.dto.StorageReadingResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService Tests")
class DashboardServiceTest {

    @Mock
    private GlucoseServiceBridge glucoseServiceBridge;

    @Mock
    private DoseServiceBridge doseServiceBridge;

    @Mock
    private StorageServiceBridge storageServiceBridge;

    @Mock
    private InventoryServiceBridge inventoryServiceBridge;

    @Mock
    private AlertService alertService;

    @Mock
    private AuthorizationService authorizationService;

    @InjectMocks
    private DashboardService dashboardService;

    private GlucoseReadingResponse glucoseReading;
    private DoseEventResponse doseEvent;
    private StorageReadingResponse storageReading;
    private InventoryReadingResponse inventoryReading;
    private List<AlertResponse> alertList;
    private Long testPatientId;

    @BeforeEach
    void setUp() {
        testPatientId = 1L;

        // Setup glucose reading
        glucoseReading = new GlucoseReadingResponse();
        glucoseReading.setGlucoseValueMgDl(120.0);
        glucoseReading.setMeasuredAt(OffsetDateTime.now());

        // Setup dose event
        doseEvent = new DoseEventResponse();
        doseEvent.setDoseUnits(10.0);
        doseEvent.setInjectedAt(OffsetDateTime.now());

        // Setup storage reading
        storageReading = new StorageReadingResponse();
        storageReading.setTemperatureC(5.0);
        storageReading.setMeasuredAt(OffsetDateTime.now());

        // Setup inventory reading
        inventoryReading = new InventoryReadingResponse();
        inventoryReading.setEstimatedRemainingPercent(50.0);
        inventoryReading.setMeasuredAt(OffsetDateTime.now());

        // Setup alerts
        AlertResponse alert1 = new AlertResponse();
        alert1.setAlertId(1L);
        alert1.setAlertType("GLUCOSE_HIGH");
        alert1.setSeverity("MEDIUM");

        AlertResponse alert2 = new AlertResponse();
        alert2.setAlertId(2L);
        alert2.setAlertType("LOW_INVENTORY");
        alert2.setSeverity("MEDIUM");

        alertList = Arrays.asList(alert1, alert2);
    }

    // =====================================================
    // SUCCESSFUL GET DASHBOARD SUMMARY TESTS
    // =====================================================

    @Test
    @DisplayName("Should successfully retrieve dashboard summary with all data")
    void testGetDashboardSummarySuccess() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertEquals(glucoseReading, response.getLatestGlucoseReading());
        assertEquals(doseEvent, response.getLatestDoseEvent());
        assertEquals(storageReading, response.getLatestStorageReading());
        assertEquals(inventoryReading, response.getLatestInventoryReading());
        assertEquals(alertList, response.getActiveAlerts());
    }

    @Test
    @DisplayName("Should verify authorization before retrieving dashboard")
    void testGetDashboardSummaryAuthorizationCheck() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        dashboardService.getDashboardSummary(testPatientId);

        // Assert
        verify(authorizationService, times(1)).authorize(Permission.READ_DASHBOARD, testPatientId);
    }

    @Test
    @DisplayName("Should retrieve glucose reading from bridge")
    void testGetDashboardSummaryCallsGlucoseBridge() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        dashboardService.getDashboardSummary(testPatientId);

        // Assert
        verify(glucoseServiceBridge, times(1)).getLatestReading(testPatientId);
    }

    @Test
    @DisplayName("Should retrieve dose event from bridge")
    void testGetDashboardSummaryCallsDoseBridge() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        dashboardService.getDashboardSummary(testPatientId);

        // Assert
        verify(doseServiceBridge, times(1)).getLatestDose(testPatientId);
    }

    @Test
    @DisplayName("Should retrieve storage reading from bridge")
    void testGetDashboardSummaryCallsStorageBridge() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        dashboardService.getDashboardSummary(testPatientId);

        // Assert
        verify(storageServiceBridge, times(1)).getLatestStorage(testPatientId);
    }

    @Test
    @DisplayName("Should retrieve inventory reading from bridge")
    void testGetDashboardSummaryCallsInventoryBridge() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        dashboardService.getDashboardSummary(testPatientId);

        // Assert
        verify(inventoryServiceBridge, times(1)).getLatestInventory(testPatientId);
    }

    @Test
    @DisplayName("Should retrieve alerts with limit of 5")
    void testGetDashboardSummaryCallsAlertService() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        dashboardService.getDashboardSummary(testPatientId);

        // Assert
        verify(alertService, times(1)).getLatestAlertsForPatient(testPatientId, 5);
    }

    // =====================================================
    // NULL DATA HANDLING TESTS
    // =====================================================

    @Test
    @DisplayName("Should handle null glucose reading")
    void testGetDashboardSummaryWithNullGlucose() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(null);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertNull(response.getLatestGlucoseReading());
        assertNotNull(response.getLatestDoseEvent());
    }

    @Test
    @DisplayName("Should handle null dose event")
    void testGetDashboardSummaryWithNullDose() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(null);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getLatestGlucoseReading());
        assertNull(response.getLatestDoseEvent());
    }

    @Test
    @DisplayName("Should handle null storage reading")
    void testGetDashboardSummaryWithNullStorage() {
        // Arrange
        doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, testPatientId);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(null);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertNull(response.getLatestStorageReading());
        assertNotNull(response.getLatestInventoryReading());
    }

    @Test
    @DisplayName("Should handle null inventory reading")
    void testGetDashboardSummaryWithNullInventory() {
        // Arrange
        when(authorizationService.authorize(Permission.READ_DASHBOARD, testPatientId))
                .thenReturn(null);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(null);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertNull(response.getLatestInventoryReading());
        assertNotNull(response.getLatestGlucoseReading());
    }

    @Test
    @DisplayName("Should handle empty alerts list")
    void testGetDashboardSummaryWithEmptyAlerts() {
        // Arrange
        when(authorizationService.authorize(Permission.READ_DASHBOARD, testPatientId))
                .thenReturn(null);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(new ArrayList<>());

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getActiveAlerts().isEmpty());
    }

    // =====================================================
    // EDGE CASES AND MULTIPLE ALERTS
    // =====================================================

    @Test
    @DisplayName("Should handle dashboard with all null data")
    void testGetDashboardSummaryAllNull() {
        // Arrange
        when(authorizationService.authorize(Permission.READ_DASHBOARD, testPatientId))
                .thenReturn(null);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(null);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(null);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(null);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(null);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(Collections.emptyList());

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertNull(response.getLatestGlucoseReading());
        assertNull(response.getLatestDoseEvent());
        assertNull(response.getLatestStorageReading());
        assertNull(response.getLatestInventoryReading());
        assertTrue(response.getActiveAlerts().isEmpty());
    }

    @Test
    @DisplayName("Should handle dashboard with multiple critical alerts")
    void testGetDashboardSummaryMultipleCriticalAlerts() {
        // Arrange
        List<AlertResponse> criticalAlerts = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            AlertResponse alert = new AlertResponse();
            alert.setAlertId((long) i);
            alert.setAlertType("CRITICAL_ALERT_" + i);
            alert.setSeverity("CRITICAL");
            criticalAlerts.add(alert);
        }

        when(authorizationService.authorize(Permission.READ_DASHBOARD, testPatientId))
                .thenReturn(null);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(criticalAlerts);

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertEquals(5, response.getActiveAlerts().size());
    }

    @Test
    @DisplayName("Should work with different patient IDs")
    void testGetDashboardSummaryDifferentPatientIds() {
        // Arrange & Act
        for (long patientId = 1L; patientId <= 5L; patientId++) {
                doNothing().when(authorizationService).authorize(Permission.READ_DASHBOARD, patientId);
            when(glucoseServiceBridge.getLatestReading(patientId)).thenReturn(glucoseReading);
            when(doseServiceBridge.getLatestDose(patientId)).thenReturn(doseEvent);
            when(storageServiceBridge.getLatestStorage(patientId)).thenReturn(storageReading);
            when(inventoryServiceBridge.getLatestInventory(patientId)).thenReturn(inventoryReading);
            when(alertService.getLatestAlertsForPatient(patientId, 5)).thenReturn(alertList);

            DashboardSummaryResponse response = dashboardService.getDashboardSummary(patientId);

            // Assert
            assertNotNull(response);
            verify(authorizationService).authorize(Permission.READ_DASHBOARD, patientId);
        }
    }

    @Test
    @DisplayName("Should return response object with all fields set")
    void testGetDashboardSummaryResponseStructure() {
        // Arrange
        when(authorizationService.authorize(Permission.READ_DASHBOARD, testPatientId))
                .thenReturn(null);
        when(glucoseServiceBridge.getLatestReading(testPatientId)).thenReturn(glucoseReading);
        when(doseServiceBridge.getLatestDose(testPatientId)).thenReturn(doseEvent);
        when(storageServiceBridge.getLatestStorage(testPatientId)).thenReturn(storageReading);
        when(inventoryServiceBridge.getLatestInventory(testPatientId)).thenReturn(inventoryReading);
        when(alertService.getLatestAlertsForPatient(testPatientId, 5)).thenReturn(alertList);

        // Act
        DashboardSummaryResponse response = dashboardService.getDashboardSummary(testPatientId);

        // Assert
        assertNotNull(response);
        assertTrue(response.getClass().getDeclaredFields().length > 0);
    }
}
