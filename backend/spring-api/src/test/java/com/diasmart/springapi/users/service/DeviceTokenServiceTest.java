package com.diasmart.springapi.users.service;

import com.diasmart.springapi.users.dto.RegisterDeviceTokenRequest;
import com.diasmart.springapi.users.entity.UserDeviceToken;
import com.diasmart.springapi.users.repository.UserDeviceTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceTokenServiceTest {

    @Mock
    private UserDeviceTokenRepository userDeviceTokenRepository;

    @InjectMocks
    private DeviceTokenService deviceTokenService;

    @Test
    void registerTokenShouldCreateNewTokenWhenNotExists() {
        RegisterDeviceTokenRequest request = new RegisterDeviceTokenRequest("sample-token-123", "android");

        when(userDeviceTokenRepository.findByDeviceToken("sample-token-123"))
                .thenReturn(Optional.empty());
        when(userDeviceTokenRepository.save(any(UserDeviceToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserDeviceToken result = deviceTokenService.registerToken(1L, request);

        assertNotNull(result);
        assertEquals(1L, result.getUserId());
        assertEquals("sample-token-123", result.getDeviceToken());
        assertEquals("android", result.getPlatform());
    }

    @Test
    void registerTokenShouldUpdateExistingTokenOwner() {
        UserDeviceToken existing = new UserDeviceToken(2L, "sample-token-123", "android");
        RegisterDeviceTokenRequest request = new RegisterDeviceTokenRequest("sample-token-123", "android");

        when(userDeviceTokenRepository.findByDeviceToken("sample-token-123"))
                .thenReturn(Optional.of(existing));
        when(userDeviceTokenRepository.save(any(UserDeviceToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        UserDeviceToken result = deviceTokenService.registerToken(1L, request);

        assertEquals(1L, result.getUserId());
        assertEquals("sample-token-123", result.getDeviceToken());
    }

    @Test
    void unregisterTokenShouldCallRepositoryDelete() {
        deviceTokenService.unregisterToken(1L, "sample-token-123");
        verify(userDeviceTokenRepository).deleteByUserIdAndDeviceToken(1L, "sample-token-123");
    }
}
