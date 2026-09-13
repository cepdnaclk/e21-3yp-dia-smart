package com.diasmart.springapi.alerts.service;

import com.diasmart.springapi.alerts.entity.Alert;
import com.diasmart.springapi.users.entity.UserDeviceToken;
import com.diasmart.springapi.users.repository.UserDeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FcmPushNotificationServiceTest {

    @Mock
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @InjectMocks
    private FcmPushNotificationService fcmPushNotificationService;

    @Test
    void sendPushNotificationForAlertShouldDoNothingWhenNullUserOrAlert() {
        fcmPushNotificationService.sendPushNotificationForAlert(null, new Alert());
        fcmPushNotificationService.sendPushNotificationForAlert(1L, null);

        verifyNoInteractions(userDeviceTokenRepository);
    }

    @Test
    void sendPushNotificationForAlertShouldReturnEarlyWhenNoTokens() {
        when(userDeviceTokenRepository.findByUserId(10L)).thenReturn(Collections.emptyList());

        Alert alert = new Alert();
        alert.setAlertId(100L);
        alert.setTitle("High Temperature");
        alert.setMessage("Storage temp exceeded threshold");

        fcmPushNotificationService.sendPushNotificationForAlert(10L, alert);

        verify(userDeviceTokenRepository).findByUserId(10L);
    }

    @Test
    void sendPushNotificationForAlertShouldHandleFirebaseNotInitializedGracefully() {
        UserDeviceToken token = new UserDeviceToken(10L, "token-abc-123", "android");
        when(userDeviceTokenRepository.findByUserId(10L)).thenReturn(List.of(token));

        Alert alert = new Alert();
        alert.setAlertId(100L);
        alert.setTitle("High Temperature");
        alert.setMessage("Storage temp exceeded threshold");

        // FirebaseMessaging.getInstance() will throw IllegalStateException if not initialized.
        // The service should catch this gracefully without throwing an exception.
        fcmPushNotificationService.sendPushNotificationForAlert(10L, alert);

        verify(userDeviceTokenRepository).findByUserId(10L);
    }
}
