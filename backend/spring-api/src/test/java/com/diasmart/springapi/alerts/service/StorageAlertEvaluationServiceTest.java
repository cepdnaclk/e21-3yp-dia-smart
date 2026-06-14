package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.storage.entity.StorageReading;
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
@DisplayName("StorageAlertEvaluationService Tests")
class StorageAlertEvaluationServiceTest {

    @Mock
    private AlertFactoryService alertFactoryService;

    @InjectMocks
    private StorageAlertEvaluationService storageAlertEvaluationService;

    private StorageReading testReading;

    @BeforeEach
    void setUp() {
        testReading = new StorageReading();
        testReading.setStorageReadingId(1L);
        testReading.setPatientId(1L);
        testReading.setTemperatureC(5.0);
        testReading.setDoorState("CLOSED");
        testReading.setCreatedAt(OffsetDateTime.now());
    }

    // =====================================================
    // NORMAL TEMPERATURE RANGE TESTS (2°C - 8°C)
    // =====================================================

    @Test
    @DisplayName("Should not create alert when temperature is within safe range (2-8°C)")
    void testNormalTemperatureNoAlert() {
        // Arrange
        testReading.setTemperatureC(5.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not create alert at minimum safe temperature (2°C)")
    void testMinimumSafeTemperatureNoAlert() {
        // Arrange
        testReading.setTemperatureC(2.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not create alert at maximum safe temperature (8°C)")
    void testMaximumSafeTemperatureNoAlert() {
        // Arrange
        testReading.setTemperatureC(8.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not create alert when temperature is just above minimum (2.1°C)")
    void testJustAboveMinimumSafeTemperatureNoAlert() {
        // Arrange
        testReading.setTemperatureC(2.1);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should not create alert when temperature is just below maximum (7.9°C)")
    void testJustBelowMaximumSafeTemperatureNoAlert() {
        // Arrange
        testReading.setTemperatureC(7.9);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    // =====================================================
    // LOW TEMPERATURE ALERT TESTS
    // =====================================================

    @Test
    @DisplayName("Should create TEMP_LOW alert when temperature is below 2°C")
    void testLowTemperatureAlert() {
        // Arrange
        testReading.setTemperatureC(1.5);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_LOW",
                "CRITICAL",
                "Storage temperature too low",
                contains("1.5")
        );
    }

    @Test
    @DisplayName("Should create alert at just below minimum (1.9°C)")
    void testJustBelowMinimumAlert() {
        // Arrange
        testReading.setTemperatureC(1.9);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_LOW",
                "CRITICAL",
                "Storage temperature too low",
                contains("1.9")
        );
    }

    @Test
    @DisplayName("Should create alert at freezing temperature (0°C)")
    void testFreezingTemperatureAlert() {
        // Arrange
        testReading.setTemperatureC(0.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_LOW",
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should create alert at negative temperature (-5°C)")
    void testNegativeTemperatureAlert() {
        // Arrange
        testReading.setTemperatureC(-5.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_LOW",
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    // =====================================================
    // HIGH TEMPERATURE ALERT TESTS
    // =====================================================

    @Test
    @DisplayName("Should create TEMP_HIGH alert when temperature exceeds 8°C")
    void testHighTemperatureAlert() {
        // Arrange
        testReading.setTemperatureC(8.5);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_HIGH",
                "CRITICAL",
                "Storage temperature too high",
                contains("8.5")
        );
    }

    @Test
    @DisplayName("Should create alert at just above maximum (8.1°C)")
    void testJustAboveMaximumAlert() {
        // Arrange
        testReading.setTemperatureC(8.1);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_HIGH",
                "CRITICAL",
                "Storage temperature too high",
                contains("8.1")
        );
    }

    @Test
    @DisplayName("Should create alert at room temperature (20°C)")
    void testRoomTemperatureAlert() {
        // Arrange
        testReading.setTemperatureC(20.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_HIGH",
                "CRITICAL",
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should create alert at high temperature (30°C)")
    void testVeryHighTemperatureAlert() {
        // Arrange
        testReading.setTemperatureC(30.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_HIGH",
                "CRITICAL",
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
        storageAlertEvaluationService.evaluateStorageAlerts(null);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle null temperature gracefully")
    void testNullTemperatureNoAlert() {
        // Arrange
        testReading.setTemperatureC(null);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should include temperature value in low temperature alert message")
    void testLowTemperatureAlertMessage() {
        // Arrange
        double tempValue = -2.5;
        testReading.setTemperatureC(tempValue);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                contains("-2.5")
        );
    }

    @Test
    @DisplayName("Should include temperature value in high temperature alert message")
    void testHighTemperatureAlertMessage() {
        // Arrange
        double tempValue = 15.7;
        testReading.setTemperatureC(tempValue);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                anyLong(),
                anyString(),
                anyString(),
                anyString(),
                contains("15.7")
        );
    }

    @Test
    @DisplayName("Should use correct patient ID in alert")
    void testAlertIncludesCorrectPatientId() {
        // Arrange
        testReading.setPatientId(42L);
        testReading.setTemperatureC(10.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService).createAlert(
                42L,
                anyString(),
                anyString(),
                anyString(),
                anyString()
        );
    }

    @Test
    @DisplayName("Should set CRITICAL severity for low temperature alerts")
    void testLowTemperatureAlertSeverity() {
        // Arrange
        testReading.setTemperatureC(1.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

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
    @DisplayName("Should set CRITICAL severity for high temperature alerts")
    void testHighTemperatureAlertSeverity() {
        // Arrange
        testReading.setTemperatureC(25.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

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
    @DisplayName("Should handle temperature with decimal precision")
    void testTemperatureWithDecimalPrecision() {
        // Arrange
        testReading.setTemperatureC(7.95);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, never()).createAlert(anyLong(), anyString(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("Should handle extreme low temperature")
    void testExtremelyLowTemperature() {
        // Arrange
        testReading.setTemperatureC(-40.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_LOW",
                "CRITICAL",
                anyString(),
                contains("-40")
        );
    }

    @Test
    @DisplayName("Should handle extreme high temperature")
    void testExtremelyHighTemperature() {
        // Arrange
        testReading.setTemperatureC(50.0);

        // Act
        storageAlertEvaluationService.evaluateStorageAlerts(testReading);

        // Assert
        verify(alertFactoryService, times(1)).createAlert(
                1L,
                "TEMP_HIGH",
                "CRITICAL",
                anyString(),
                contains("50")
        );
    }
}
