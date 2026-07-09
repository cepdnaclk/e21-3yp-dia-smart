package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

        @Mock
        private AlertRepository alertRepository;

        @Mock
        private CurrentUserService currentUserService;

        @Mock
        private PatientAccessService patientAccessService;

        @InjectMocks
        private AlertService alertService;

        @Test
        void getAlertShouldReturnAlert() {

                Alert alert = new Alert();
                alert.setAlertId(1L);
                alert.setPatientId(10L);

                when(alertRepository.findById(1L))
                                .thenReturn(Optional.of(alert));

                alertService.getAlert(1L);

                verify(patientAccessService)
                                .requireCanViewPatient(10L);
        }

        @Test
        void getAlertShouldThrowWhenMissing() {

                when(alertRepository.findById(1L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> alertService.getAlert(1L));
        }

        @Test
        void acknowledgeAlertShouldUpdateStatus() {

                AppUser user = new AppUser();
                user.setUserId(5L);

                Alert alert = new Alert();
                alert.setAlertId(1L);
                alert.setPatientId(10L);

                when(currentUserService.getCurrentUser())
                                .thenReturn(user);

                when(alertRepository.findById(1L))
                                .thenReturn(Optional.of(alert));

                when(alertRepository.save(any(Alert.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                alertService.acknowledgeAlert(1L);

                assertEquals(
                                "ACKNOWLEDGED",
                                alert.getStatus());

                verify(patientAccessService)
                                .requireCanAcknowledgeAlerts(10L);
        }

        @Test
        void resolveAlertShouldUpdateStatus() {

                AppUser user = new AppUser();
                user.setUserId(5L);

                Alert alert = new Alert();
                alert.setAlertId(1L);
                alert.setPatientId(10L);

                when(currentUserService.getCurrentUser())
                                .thenReturn(user);

                when(alertRepository.findById(1L))
                                .thenReturn(Optional.of(alert));

                when(alertRepository.save(any(Alert.class)))
                                .thenAnswer(inv -> inv.getArgument(0));

                alertService.resolveAlert(
                                1L,
                                "Issue fixed");

                assertEquals(
                                "RESOLVED",
                                alert.getStatus());

                assertEquals(
                                "Issue fixed",
                                alert.getResolutionNote());
        }

        @Test
        void resolveAlertShouldThrowWhenMissing() {

                AppUser user = new AppUser();

                when(currentUserService.getCurrentUser())
                                .thenReturn(user);

                when(alertRepository.findById(1L))
                                .thenReturn(Optional.empty());

                assertThrows(
                                IllegalArgumentException.class,
                                () -> alertService.resolveAlert(
                                                1L,
                                                "fixed"));
        }
}