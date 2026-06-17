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
import java.util.Set;

@Service
public class AlertService {

        private static final Set<String> FILTERABLE_STATUSES =
                        Set.of(
                                        "OPEN",
                                        "ACKNOWLEDGED",
                                        "RESOLVED");

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

        public Page<AlertResponse> getAlerts(Pageable pageable, String status) {
                AppUser currentUser = currentUserService.getCurrentUser();
                String normalizedStatus = normalizeStatus(status);

                /*
                 * ADMIN handling:
                 * Admin is a system-level role from app_users.role.
                 * Admin is not stored in user_patient_access.access_role.
                 */
                if (currentUser.getRole() == UserRole.ADMIN) {
                        if (normalizedStatus != null) {
                                return alertRepository
                                                .findByStatusOrderByCreatedAtDesc(
                                                                normalizedStatus,
                                                                pageable)
                                                .map(this::mapToResponse);
                        }

                        return alertRepository
                                        .findAllByOrderByCreatedAtDesc(pageable)
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

                if (normalizedStatus != null) {
                        return alertRepository
                                        .findByPatientIdInAndStatusOrderByCreatedAtDesc(
                                                        viewablePatientIds,
                                                        normalizedStatus,
                                                        pageable)
                                        .map(this::mapToResponse);
                }

                return alertRepository
                                .findByPatientIdInOrderByCreatedAtDesc(
                                                viewablePatientIds,
                                                pageable)
                                .map(this::mapToResponse);
        }

        public List<AlertResponse> getLatestAlertsForPatient(Long patientId, int limit) {
                return alertRepository
                                .findByPatientIdOrderByCreatedAtDesc(
                                                patientId,
                                                org.springframework.data.domain.PageRequest.of(0, limit))
                                .map(this::mapToResponse)
                                .getContent();
        }

        private String normalizeStatus(String status) {
                if (status == null || status.isBlank()) {
                        return null;
                }

                String normalized = status.trim().toUpperCase();

                if ("ALL".equals(normalized)) {
                        return null;
                }

                if (!FILTERABLE_STATUSES.contains(normalized)) {
                        throw new IllegalArgumentException(
                                        "Unsupported alert status: " + status);
                }

                return normalized;
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
                //response.setAlertDomain(alert.getAlertDomain());
                response.setCreatedAt(alert.getCreatedAt());
                response.setAcknowledgedAt(alert.getAcknowledgedAt());
                response.setResolvedAt(alert.getResolvedAt());

                return response;
        }

        public AlertResponse resolveAlert(Long alertId, String resolutionNote) {
                AppUser currentUser = currentUserService.getCurrentUser();

                Alert alert = alertRepository
                                .findById(alertId)
                                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));

                patientAccessService.requireCanAcknowledgeAlerts(
                                alert.getPatientId());

                alert.setStatus("RESOLVED");
                alert.setResolvedAt(OffsetDateTime.now());
                alert.setResolvedBy(currentUser.getUserId());
                alert.setResolutionNote(resolutionNote);

                Alert updatedAlert = alertRepository.save(alert);

                return mapToResponse(updatedAlert);
        }
}
