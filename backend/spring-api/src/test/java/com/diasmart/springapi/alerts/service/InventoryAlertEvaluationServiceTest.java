package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.inventory.entity.InventoryReading;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryAlertEvaluationService Tests")
class InventoryAlertEvaluationServiceTest {

    @Mock
    private AlertFactoryService alertFactoryService;

    @InjectMocks
    private InventoryAlertEvaluationService inventoryAlertEvaluationService;

    private InventoryReading testReading;

    @BeforeEach
    void setUp() {
        testReading = new InventoryReading();
        testReading.setInventoryReadingId(1L);
        testReading.setPatientId(1L);
        testReading.setEstimatedRemainingPercent(50.0);
        testReading.setCreatedAt(OffsetDateTime.now());
    }

    // =====================================================
    // NORMAL INVENTORY LEVEL TESTS (>20%)
    // =====================================================

    @Test
    @DisplayName("Should not create alert when inventory is above warning threshold (>20%)")
    void testNormalInventoryNoAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(50.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not create alert when inventory is at 100%")
    void testFullInventoryNoAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(100.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not create alert when inventory is just above warning threshold (20.1%)")
    void testJustAboveWarningThresholdNoAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(20.1);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    // =====================================================
    // LOW INVENTORY ALERT TESTS (10% < x <= 20%)
    // =====================================================

    @Test
    @DisplayName("Should create LOW_INVENTORY alert when inventory is between critical and warning thresholds")
    void testLowInventoryAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(15.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "LOW_INVENTORY",
                "MEDIUM",
                "Low insulin inventory",
                contains("15")
        );
    }

    @Test
    @DisplayName("Should create alert at exactly warning threshold (20%)")
    void testExactWarningThresholdAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(20.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "LOW_INVENTORY",
                "MEDIUM",
                "Low insulin inventory",
                contains("20")
        );
    }

    @Test
    @DisplayName("Should create alert just above critical threshold (10.1%)")
    void testJustAboveCriticalThresholdAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(10.1);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "LOW_INVENTORY",
                "MEDIUM",
                anyString(),
                anyString()
        );
    }

    // =====================================================
    // CRITICAL INVENTORY ALERT TESTS (<= 10%)
    // =====================================================

    @Test
    @DisplayName("Should create CRITICAL_INVENTORY alert when inventory is at or below critical threshold (<=10%)")
    void testCriticalInventoryAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(5.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "CRITICAL_INVENTORY",
                "CRITICAL",
                "Critical insulin inventory level",
                contains("5")
        );
    }

    @Test
    @DisplayName("Should create alert at exactly critical threshold (10%)")
    void testExactCriticalThresholdAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(10.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "CRITICAL_INVENTORY",
                "CRITICAL",
                "Critical insulin inventory level",
                contains("10")
        );
    }

    @Test
    @DisplayName("Should create alert when inventory is critically low (1%)")
    void testVeryCriticalInventoryAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(1.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "CRITICAL_INVENTORY",
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should create alert when inventory is empty (0%)")
    void testEmptyInventoryAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(0.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "CRITICAL_INVENTORY",
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    // =====================================================
    // CRITICAL ALERT EARLY RETURN TEST
    // =====================================================

    @Test
    @DisplayName("Should not create LOW_INVENTORY alert if CRITICAL_INVENTORY alert is triggered")
    void testCriticalAlertPreventsLowAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(5.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        // Verify only CRITICAL_INVENTORY alert is created
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "CRITICAL_INVENTORY",
                "CRITICAL",
                anyString(),
                anyString()
        );
        // Verify LOW_INVENTORY alert is NOT created
        verify(alertFactoryService, never()).createAlert(
                1L,
                "LOW_INVENTORY",
                anyString(),
                anyString(),
                anyString()
        );
    }

    // =====================================================
    // NULL AND EDGE CASE TESTS
    // =====================================================

    @Test
    @DisplayName("Should handle null reading gracefully")
    void testNullReadingNoAlert() {
        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(null);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle null estimated remaining percent gracefully")
    void testNullRemainingPercentNoAlert() {
        // Arrange
        testReading.setEstimatedRemainingPercent(null);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should include inventory percentage in low inventory alert message")
    void testLowInventoryAlertMessage() {
        // Arrange
        double inventoryPercent = 12.5;
        testReading.setEstimatedRemainingPercent(inventoryPercent);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                contains("12.5")
        );
    }

    @Test
    @DisplayName("Should include inventory percentage in critical inventory alert message")
    void testCriticalInventoryAlertMessage() {
        // Arrange
        double inventoryPercent = 8.3;
        testReading.setEstimatedRemainingPercent(inventoryPercent);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                contains("8.3")
        );
    }

    @Test
    @DisplayName("Should use correct patient ID in alert")
    void testAlertIncludesCorrectPatientId() {
        // Arrange
        testReading.setPatientId(99L);
        testReading.setEstimatedRemainingPercent(5.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                99L,
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should set CRITICAL severity for critical inventory alerts")
    void testCriticalAlertSeverity() {
        // Arrange
        testReading.setEstimatedRemainingPercent(3.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                anyLong(),
                anyString(),
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should set MEDIUM severity for low inventory alerts")
    void testLowAlertSeverity() {
        // Arrange
        testReading.setEstimatedRemainingPercent(15.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                anyLong(),
                anyString(),
                "MEDIUM",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should handle inventory with decimal precision")
    void testInventoryWithDecimalPrecision() {
        // Arrange
        testReading.setEstimatedRemainingPercent(20.00001);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle inventory greater than 100%")
    void testInventoryGreaterThan100Percent() {
        // Arrange
        testReading.setEstimatedRemainingPercent(105.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle negative inventory percentage")
    void testNegativeInventoryPercentage() {
        // Arrange
        testReading.setEstimatedRemainingPercent(-5.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        // Should still create CRITICAL_INVENTORY alert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "CRITICAL_INVENTORY",
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should correctly identify boundary between LOW and CRITICAL at 10%")
    void testBoundaryBetweenLowAndCritical() {
        // Arrange - exactly at critical threshold
        testReading.setEstimatedRemainingPercent(10.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert - should be CRITICAL, not LOW
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "CRITICAL_INVENTORY",
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should correctly identify boundary between NORMAL and LOW at 20%")
    void testBoundaryBetweenNormalAndLow() {
        // Arrange - exactly at warning threshold
        testReading.setEstimatedRemainingPercent(20.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert - should be LOW, not NORMAL
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "LOW_INVENTORY",
                "MEDIUM",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should handle very high inventory percentage")
    void testVeryHighInventoryPercentage() {
        // Arrange
        testReading.setEstimatedRemainingPercent(999.0);

        // Act
        inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle inventory reading with different patient IDs")
    void testMultiplePatientsInventoryAlerts() {
        // Arrange
        for (long patientId = 1L; patientId <= 5L; patientId++) {
            testReading.setPatientId(patientId);
            testReading.setEstimatedRemainingPercent(5.0);

            // Act
            inventoryAlertEvaluationService.evaluateInventoryAlerts(testReading);

            // Assert
            verify(alertFactoryService).createAlert(
                    patientId,
                    "CRITICAL_INVENTORY",
                    "CRITICAL",
                    anyString(),
                    anyString()
            );
        }
    }
}
