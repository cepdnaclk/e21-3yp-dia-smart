package com.diasmart.springapi.notifications.service;

import com.diasmart.springapi.notifications.dto.NotificationPreferencesResponseDTO;
import com.diasmart.springapi.notifications.dto.UpdateBuzzerPreferenceRequestDTO;
import com.diasmart.springapi.notifications.dto.UpdateNotificationPreferencesRequestDTO;
import com.diasmart.springapi.notifications.dto.UpdateReminderPreferencesRequestDTO;

public interface NotificationPreferenceService {

    NotificationPreferencesResponseDTO getPreferences(Long userId);

    NotificationPreferencesResponseDTO updatePreferences(
            Long userId,
            UpdateNotificationPreferencesRequestDTO dto
    );

    NotificationPreferencesResponseDTO updateReminderPreferences(
            Long userId,
            UpdateReminderPreferencesRequestDTO dto
    );

    NotificationPreferencesResponseDTO updateBuzzerPreference(
            Long userId,
            UpdateBuzzerPreferenceRequestDTO dto
    );
}
