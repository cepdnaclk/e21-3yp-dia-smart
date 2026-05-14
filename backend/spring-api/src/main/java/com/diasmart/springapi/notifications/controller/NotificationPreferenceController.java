package com.diasmart.springapi.notifications.controller;

import com.diasmart.springapi.common.responses.ApiResponse;
import com.diasmart.springapi.notifications.dto.NotificationPreferencesResponseDTO;
import com.diasmart.springapi.notifications.dto.UpdateBuzzerPreferenceRequestDTO;
import com.diasmart.springapi.notifications.dto.UpdateNotificationPreferencesRequestDTO;
import com.diasmart.springapi.notifications.dto.UpdateReminderPreferencesRequestDTO;
import com.diasmart.springapi.notifications.service.NotificationPreferenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private static final Long PROTOTYPE_USER_ID = 1L;

    private final NotificationPreferenceService service;

    public NotificationPreferenceController(
            NotificationPreferenceService service
    ) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<NotificationPreferencesResponseDTO>>
    getPreferences(
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long headerUserId,
            @RequestParam(
                    value = "userId",
                    required = false
            ) Long queryUserId
    ) {
        NotificationPreferencesResponseDTO response =
                service.getPreferences(
                        resolveUserId(headerUserId, queryUserId)
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification preferences retrieved successfully",
                        response
                )
        );
    }

    @PatchMapping
    public ResponseEntity<ApiResponse<NotificationPreferencesResponseDTO>>
    updatePreferences(
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long headerUserId,
            @RequestParam(
                    value = "userId",
                    required = false
            ) Long queryUserId,
            @RequestBody UpdateNotificationPreferencesRequestDTO dto
    ) {
        NotificationPreferencesResponseDTO response =
                service.updatePreferences(
                        resolveUserId(headerUserId, queryUserId),
                        dto
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Notification preferences updated successfully",
                        response
                )
        );
    }

    @PatchMapping("/reminders")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponseDTO>>
    updateReminderPreferences(
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long headerUserId,
            @RequestParam(
                    value = "userId",
                    required = false
            ) Long queryUserId,
            @Valid @RequestBody UpdateReminderPreferencesRequestDTO dto
    ) {
        NotificationPreferencesResponseDTO response =
                service.updateReminderPreferences(
                        resolveUserId(headerUserId, queryUserId),
                        dto
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Reminder preferences updated successfully",
                        response
                )
        );
    }

    @PatchMapping("/buzzer")
    public ResponseEntity<ApiResponse<NotificationPreferencesResponseDTO>>
    updateBuzzerPreference(
            @RequestHeader(
                    value = "X-User-Id",
                    required = false
            ) Long headerUserId,
            @RequestParam(
                    value = "userId",
                    required = false
            ) Long queryUserId,
            @RequestBody UpdateBuzzerPreferenceRequestDTO dto
    ) {
        NotificationPreferencesResponseDTO response =
                service.updateBuzzerPreference(
                        resolveUserId(headerUserId, queryUserId),
                        dto
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Buzzer preference updated successfully",
                        response
                )
        );
    }

    private Long resolveUserId(
            Long headerUserId,
            Long queryUserId
    ) {
        if (headerUserId != null) {
            return headerUserId;
        }

        if (queryUserId != null) {
            return queryUserId;
        }

        Authentication authentication =
                SecurityContextHolder
                        .getContext()
                        .getAuthentication();

        if (authentication != null) {
            try {
                return Long.valueOf(authentication.getName());
            } catch (NumberFormatException ignored) {
                return PROTOTYPE_USER_ID;
            }
        }

        return PROTOTYPE_USER_ID;
    }
}
