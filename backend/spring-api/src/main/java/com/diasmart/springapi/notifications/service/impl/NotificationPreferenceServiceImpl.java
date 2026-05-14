package com.diasmart.springapi.notifications.service.impl;

import com.diasmart.springapi.audit.service.AuditService;
import com.diasmart.springapi.common.exceptions.ApiException;
import com.diasmart.springapi.notifications.dto.NotificationPreferencesResponseDTO;
import com.diasmart.springapi.notifications.dto.UpdateBuzzerPreferenceRequestDTO;
import com.diasmart.springapi.notifications.dto.UpdateNotificationPreferencesRequestDTO;
import com.diasmart.springapi.notifications.dto.UpdateReminderPreferencesRequestDTO;
import com.diasmart.springapi.notifications.entity.NotificationPreference;
import com.diasmart.springapi.notifications.repository.NotificationPreferenceRepository;
import com.diasmart.springapi.notifications.service.NotificationPreferenceService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class NotificationPreferenceServiceImpl
        implements NotificationPreferenceService {

    private final NotificationPreferenceRepository repository;
    private final AuditService auditService;

    public NotificationPreferenceServiceImpl(
            NotificationPreferenceRepository repository,
            AuditService auditService
    ) {
        this.repository = repository;
        this.auditService = auditService;
    }

    @Override
    public NotificationPreferencesResponseDTO getPreferences(
            Long userId
    ) {
        validateUserId(userId);

        return repository
                .findByUserId(userId)
                .map(this::mapToDTO)
                .orElseGet(() -> mapToDTO(defaultPreference(userId)));
    }

    @Override
    public NotificationPreferencesResponseDTO updatePreferences(
            Long userId,
            UpdateNotificationPreferencesRequestDTO dto
    ) {
        validateUserId(userId);

        NotificationPreference preference =
                repository
                        .findByUserId(userId)
                        .orElseGet(() -> defaultPreference(userId));

        Boolean remindersEnabled =
                firstNonNull(
                        dto.getRemindersEnabled(),
                        dto.getReminderEnabled()
                );
        Boolean pushEnabled =
                firstNonNull(
                        dto.getMobileNotificationsEnabled(),
                        dto.getPushEnabled()
                );

        if (remindersEnabled != null) {
            preference.setReminderEnabled(remindersEnabled);
        }

        if (dto.getBuzzerEnabled() != null) {
            preference.setBuzzerEnabled(dto.getBuzzerEnabled());
        }

        if (pushEnabled != null) {
            preference.setPushEnabled(pushEnabled);
        }

        if (dto.getSmsEnabled() != null) {
            preference.setSmsEnabled(dto.getSmsEnabled());
        }

        if (dto.getEmailEnabled() != null) {
            preference.setEmailEnabled(dto.getEmailEnabled());
        }

        if (dto.getCaregiverNotificationsEnabled() != null) {
            preference.setCaregiverNotificationsEnabled(
                    dto.getCaregiverNotificationsEnabled()
            );
        }

        NotificationPreference saved = repository.save(preference);
        logPreferenceUpdate(userId, "GENERAL");
        return mapToDTO(saved);
    }

    @Override
    public NotificationPreferencesResponseDTO updateReminderPreferences(
            Long userId,
            UpdateReminderPreferencesRequestDTO dto
    ) {
        validateUserId(userId);

        NotificationPreference preference =
                repository
                        .findByUserId(userId)
                        .orElseGet(() -> defaultPreference(userId));

        if (dto.getRemindersEnabled() != null) {
            preference.setReminderEnabled(dto.getRemindersEnabled());
        }

        Integer reminderMinutes =
                firstNonNull(
                        dto.getReminderLeadMinutes(),
                        dto.getReminderMinutesBefore()
                );

        if (reminderMinutes != null) {
            preference.setReminderMinutesBefore(reminderMinutes);
        }

        NotificationPreference saved = repository.save(preference);
        logPreferenceUpdate(userId, "REMINDERS");
        return mapToDTO(saved);
    }

    @Override
    public NotificationPreferencesResponseDTO updateBuzzerPreference(
            Long userId,
            UpdateBuzzerPreferenceRequestDTO dto
    ) {
        validateUserId(userId);

        if (dto.getBuzzerEnabled() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "buzzerEnabled is required"
            );
        }

        NotificationPreference preference =
                repository
                        .findByUserId(userId)
                        .orElseGet(() -> defaultPreference(userId));

        preference.setBuzzerEnabled(dto.getBuzzerEnabled());

        NotificationPreference saved = repository.save(preference);
        logPreferenceUpdate(userId, "BUZZER");
        return mapToDTO(saved);
    }

    private NotificationPreference defaultPreference(Long userId) {
        NotificationPreference preference =
                new NotificationPreference();
        preference.setUserId(userId);
        preference.setReminderEnabled(true);
        preference.setBuzzerEnabled(true);
        preference.setReminderMinutesBefore(15);
        preference.setPushEnabled(true);
        preference.setSmsEnabled(false);
        preference.setEmailEnabled(false);
        preference.setCaregiverNotificationsEnabled(true);
        return preference;
    }

    private NotificationPreferencesResponseDTO mapToDTO(
            NotificationPreference preference
    ) {
        NotificationPreferencesResponseDTO dto =
                new NotificationPreferencesResponseDTO();

        dto.setPreferenceId(preference.getPreferenceId());
        dto.setUserId(preference.getUserId());
        dto.setRemindersEnabled(
                defaultBoolean(preference.getReminderEnabled(), true)
        );
        dto.setBuzzerEnabled(
                defaultBoolean(preference.getBuzzerEnabled(), true)
        );
        dto.setReminderLeadMinutes(
                defaultInteger(
                        preference.getReminderMinutesBefore(),
                        15
                )
        );
        dto.setMobileNotificationsEnabled(
                defaultBoolean(preference.getPushEnabled(), true)
        );
        dto.setDashboardNotificationsEnabled(true);
        dto.setSmsEnabled(
                defaultBoolean(preference.getSmsEnabled(), false)
        );
        dto.setEmailEnabled(
                defaultBoolean(preference.getEmailEnabled(), false)
        );
        dto.setCaregiverNotificationsEnabled(
                defaultBoolean(
                        preference.getCaregiverNotificationsEnabled(),
                        true
                )
        );
        dto.setUpdatedAt(preference.getUpdatedAt());

        return dto;
    }

    private void logPreferenceUpdate(
            Long userId,
            String preferenceArea
    ) {
        Map<String, Object> details = new LinkedHashMap<>();
        details.put("preferenceArea", preferenceArea);

        auditService.record(
                userId,
                null,
                AuditService.NOTIFICATION_PREFERENCES_UPDATED,
                "NOTIFICATION_PREFERENCE",
                null,
                null,
                details
        );
    }

    private void validateUserId(Long userId) {
        if (userId == null || userId <= 0) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "A valid userId is required"
            );
        }
    }

    private Boolean defaultBoolean(
            Boolean value,
            boolean defaultValue
    ) {
        return value == null ? defaultValue : value;
    }

    private Integer defaultInteger(
            Integer value,
            int defaultValue
    ) {
        return value == null ? defaultValue : value;
    }

    private <T> T firstNonNull(T first, T second) {
        return first != null ? first : second;
    }
}
