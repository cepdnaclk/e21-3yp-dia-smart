package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.users.entity.UserDeviceToken;
import com.diasmart.springapi.users.repository.UserDeviceTokenRepository;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FcmPushNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FcmPushNotificationService.class);

    private final UserDeviceTokenRepository userDeviceTokenRepository;

    public FcmPushNotificationService(UserDeviceTokenRepository userDeviceTokenRepository) {
        this.userDeviceTokenRepository = userDeviceTokenRepository;
    }

    public void sendPushNotificationForAlert(Long userId, Alert alert) {
        if (userId == null || alert == null) {
            return;
        }

        List<UserDeviceToken> tokens = userDeviceTokenRepository.findByUserId(userId);
        if (tokens.isEmpty()) {
            logger.debug("No registered push tokens found for user ID {}", userId);
            return;
        }

        List<String> tokenStrings = tokens.stream()
                .map(UserDeviceToken::getDeviceToken)
                .collect(Collectors.toList());

        try {
            Notification notification = Notification.builder()
                    .setTitle(alert.getTitle() != null ? alert.getTitle() : "New Alert")
                    .setBody(alert.getMessage() != null ? alert.getMessage() : "You have a new Dia-Smart notification.")
                    .build();

            MulticastMessage message = MulticastMessage.builder()
                    .addAllTokens(tokenStrings)
                    .setNotification(notification)
                    .putData("alertId", alert.getAlertId() != null ? String.valueOf(alert.getAlertId()) : "")
                    .putData("alertType", alert.getAlertType() != null ? alert.getAlertType() : "")
                    .putData("severity", alert.getSeverity() != null ? alert.getSeverity() : "")
                    .putData("patientId", alert.getPatientId() != null ? String.valueOf(alert.getPatientId()) : "")
                    .build();

            FirebaseMessaging.getInstance().sendEachForMulticast(message);
            logger.info("Push notification sent to {} device(s) for user ID {}", tokenStrings.size(), userId);
        } catch (IllegalStateException e) {
            logger.warn("FirebaseApp is not initialized. Skipping push notification dispatch: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("Failed to send FCM push notification for alert ID {}: {}", alert.getAlertId(), e.getMessage());
        }
    }
}
