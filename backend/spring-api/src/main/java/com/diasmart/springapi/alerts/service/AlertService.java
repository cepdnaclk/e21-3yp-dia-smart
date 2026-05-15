package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.alerts.repository.AlertRepository;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Service
public class AlertService {

    private final AlertRepository alertRepository;

    private final CurrentUserService currentUserService;

    public AlertService(
            AlertRepository alertRepository,
            CurrentUserService currentUserService
    ) {
        this.alertRepository = alertRepository;
        this.currentUserService = currentUserService;
    }

    public Page<AlertResponse> getAlerts(
            Pageable pageable
    ) {

        AppUser currentUser =
                currentUserService.getCurrentUser();

        return alertRepository
                .findByPatientIdOrderByCreatedAtDesc(
                        currentUser.getId(),
                        pageable
                )
                .map(this::mapToResponse);
    }

    public AlertResponse getAlert(
            Long alertId
    ) {

        AppUser currentUser =
                currentUserService.getCurrentUser();

        Alert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Alert not found"
                        )
                );

        if (!alert.getPatientId().equals(
                currentUser.getId()
        )) {

            throw new IllegalArgumentException(
                    "You cannot access this alert"
            );
        }

        return mapToResponse(alert);
    }

    public AlertResponse acknowledgeAlert(
            Long alertId
    ) {

        AppUser currentUser =
                currentUserService.getCurrentUser();

        Alert alert = alertRepository
                .findById(alertId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Alert not found"
                        )
                );

        if (!alert.getPatientId().equals(
                currentUser.getId()
        )) {

            throw new IllegalArgumentException(
                    "You cannot acknowledge this alert"
            );
        }

        alert.setStatus("ACKNOWLEDGED");

        alert.setAcknowledgedAt(
                OffsetDateTime.now()
        );

        alert.setAcknowledgedBy(
                currentUser.getId()
        );

        Alert updatedAlert =
                alertRepository.save(alert);

        return mapToResponse(updatedAlert);
    }

    private AlertResponse mapToResponse(
            Alert alert
    ) {

        AlertResponse response =
                new AlertResponse();

        response.setAlertId(
                alert.getAlertId()
        );

        response.setAlertType(
                alert.getAlertType()
        );

        response.setSeverity(
                alert.getSeverity()
        );

        response.setTitle(
                alert.getTitle()
        );

        response.setMessage(
                alert.getMessage()
        );

        response.setStatus(
                alert.getStatus()
        );

        response.setCreatedAt(
                alert.getCreatedAt()
        );

        response.setAcknowledgedAt(
                alert.getAcknowledgedAt()
        );

        return response;
    }
}