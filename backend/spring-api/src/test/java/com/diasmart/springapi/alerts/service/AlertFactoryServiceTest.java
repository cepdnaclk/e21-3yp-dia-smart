package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertFactoryServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @InjectMocks
    private AlertFactoryService alertFactoryService;

    @Test
    void shouldSkipAlertWhenSameTypeCreatedInsideGap() {

        Alert latestAlert = new Alert();
        latestAlert.setCreatedAt(
                OffsetDateTime.now()
                        .minusMinutes(5)
        );

        when(alertRepository
                .findTopByPatientIdAndAlertTypeOrderByCreatedAtDesc(
                        1L,
                        "TEMP_HIGH"
                ))
                .thenReturn(Optional.of(latestAlert));

        Optional<Alert> result =
                alertFactoryService.createAlertIfGapElapsed(
                        1L,
                        "TEMP_HIGH",
                        "CRITICAL",
                        "Storage temperature too high",
                        "Detected storage temperature above safe range",
                        Duration.ofMinutes(10)
                );

        assertTrue(result.isEmpty());
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void shouldCreateAlertWhenSameTypeCreatedOutsideGap() {

        Alert latestAlert = new Alert();
        latestAlert.setCreatedAt(
                OffsetDateTime.now()
                        .minusMinutes(11)
        );

        when(alertRepository
                .findTopByPatientIdAndAlertTypeOrderByCreatedAtDesc(
                        1L,
                        "TEMP_HIGH"
                ))
                .thenReturn(Optional.of(latestAlert));

        when(alertRepository.save(any(Alert.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        Optional<Alert> result =
                alertFactoryService.createAlertIfGapElapsed(
                        1L,
                        "TEMP_HIGH",
                        "CRITICAL",
                        "Storage temperature too high",
                        "Detected storage temperature above safe range",
                        Duration.ofMinutes(10)
                );

        assertTrue(result.isPresent());
        verify(alertRepository).save(any(Alert.class));
    }
}
