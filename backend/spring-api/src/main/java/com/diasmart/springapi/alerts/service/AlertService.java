package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.relationships.service.PatientAccessService;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;

@Service
public class AlertService {

        private final AlertRepository alertRepository;
        private final CurrentUserService currentUserService;
        private final PatientAccessService patientAccessService;

        public AlertService(
                        AlertRepository alertRepository,
                        CurrentUserService currentUserService,
                        PatientAccessService patientAccessService) {
                this.alertRepository = alertRepository;
                this.currentUserService = currentUserService;
                this.patientAccessService = patientAccessService;
        }

        public Page<AlertResponse> getAlerts(Pageable pageable) {
                AppUser currentUser = currentUserService.getCurrentUser();

                /*
                 * ADMIN handling:
                 * Admin is a system-level role from app_users.role.
                 * Admin is not stored in user_patient_access.access_role.
                 */
                if (currentUser.getRole() == UserRole.ADMIN) {
                        return alertRepository
                                        .findAll(pageable)
                                        .map(this::mapToResponse);
                }

                /*
                 * Correct relationship logic:
                 * app_users.user_id -> user_patient_access -> patients.patient_id
                 */
                List<Long> viewablePatientIds = patientAccessService.getViewablePatientIdsForCurrentUser();

                if (viewablePatientIds.isEmpty()) {
                        return Page.empty(pageable);
                }

                return alertRepository
                                .findByPatientIdInOrderByCreatedAtDesc(
                                                viewablePatientIds,
                                                pageable)
                                .map(this::mapToResponse);
        }

        public AlertResponse getAlert(Long alertId) {
                Alert alert = alertRepository
                                .findById(alertId)
                                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

                patientAccessService.requireCanViewPatient(
                                alert.getPatientId());

                return mapToResponse(alert);
        }

        public AlertResponse acknowledgeAlert(Long alertId) {
                AppUser currentUser = currentUserService.getCurrentUser();

                Alert alert = alertRepository
                                .findById(alertId)
                                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

                patientAccessService.requireCanAcknowledgeAlerts(
                                alert.getPatientId());

                alert.setStatus("ACKNOWLEDGED");
                alert.setAcknowledgedAt(OffsetDateTime.now());
                alert.setAcknowledgedBy(currentUser.getUserId());

                Alert updatedAlert = alertRepository.save(alert);

                return mapToResponse(updatedAlert);
        }

        private AlertResponse mapToResponse(Alert alert) {
                AlertResponse response = new AlertResponse();

                response.setAlertId(alert.getAlertId());
                response.setAlertType(alert.getAlertType());
                response.setSeverity(alert.getSeverity());
                response.setTitle(alert.getTitle());
                response.setMessage(alert.getMessage());
                response.setStatus(alert.getStatus());
                response.setCreatedAt(alert.getCreatedAt());
                response.setAcknowledgedAt(alert.getAcknowledgedAt());

                return response;
        }
}