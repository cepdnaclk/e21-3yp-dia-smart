package com.diasmart.springapi.devices.service;

import com.diasmart.springapi.devices.config.DeviceActivationProperties;
import com.diasmart.springapi.devices.entity.DeviceActivationAttempt;
import com.diasmart.springapi.devices.entity.DeviceActivationFailureCategory;
import com.diasmart.springapi.devices.repository.DeviceActivationAttemptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeviceActivationAttemptServiceTest {

    @Mock
    private DeviceActivationAttemptRepository attemptRepository;

    private DeviceActivationAttemptService attemptService;
    private DeviceActivationProperties properties;

    @BeforeEach
    void setUp() {
        properties = new DeviceActivationProperties();
        properties.setMaxFailuresPerUser(5);
        properties.setMaxFailuresPerIp(10);
        properties.setWindowMinutes(15);
        attemptService = new DeviceActivationAttemptService(attemptRepository, properties);
    }

    @Test
    void recordFailureShouldPersistSafeFailureMetadata() {
        OffsetDateTime attemptedAt = OffsetDateTime.parse("2026-08-02T12:00:00Z");
        OffsetDateTime blockedUntil = OffsetDateTime.parse("2026-08-02T12:15:00Z");

        attemptService.recordFailure(
                7L,
                25L,
                77L,
                "203.0.113.10",
                DeviceActivationFailureCategory.INVALID_KIT,
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                attemptedAt,
                blockedUntil);

        ArgumentCaptor<DeviceActivationAttempt> captor =
                ArgumentCaptor.forClass(DeviceActivationAttempt.class);
        verify(attemptRepository).save(captor.capture());

        DeviceActivationAttempt attempt = captor.getValue();
        assertEquals(7L, attempt.getUserId());
        assertEquals(25L, attempt.getPatientId());
        assertEquals(77L, attempt.getKitId());
        assertEquals("203.0.113.10", attempt.getIpAddress());
        assertFalse(attempt.getSuccess());
        assertEquals(DeviceActivationFailureCategory.INVALID_KIT, attempt.getFailureCategory());
        assertEquals(64, attempt.getRequestFingerprint().length());
        assertFalse(attempt.getRequestFingerprint().contains("OUT-1"));
        assertEquals(attemptedAt, attempt.getAttemptedAt());
        assertEquals(blockedUntil, attempt.getBlockedUntil());
    }

    @Test
    void isRateLimitedShouldEnforceUserLimit() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-02T12:00:00Z");

        when(attemptRepository.findTopByUserIdAndSuccessTrueOrderByAttemptedAtDesc(7L))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByUserIdAndSuccessFalseAndAttemptedAtAfter(
                7L,
                now.minusMinutes(15)))
                .thenReturn(5L);

        assertTrue(attemptService.isRateLimited(7L, "203.0.113.10", now));
    }

    @Test
    void isRateLimitedShouldEnforceIpLimit() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-02T12:00:00Z");

        when(attemptRepository.findTopByUserIdAndSuccessTrueOrderByAttemptedAtDesc(7L))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByUserIdAndSuccessFalseAndAttemptedAtAfter(
                7L,
                now.minusMinutes(15)))
                .thenReturn(0L);
        when(attemptRepository.findTopByIpAddressAndSuccessTrueOrderByAttemptedAtDesc("203.0.113.10"))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByIpAddressAndSuccessFalseAndAttemptedAtAfter(
                "203.0.113.10",
                now.minusMinutes(15)))
                .thenReturn(10L);

        assertTrue(attemptService.isRateLimited(7L, "203.0.113.10", now));
    }

    @Test
    void isRateLimitedShouldIgnoreAttemptsOutsideWindow() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-02T12:00:00Z");

        when(attemptRepository.findTopByUserIdAndSuccessTrueOrderByAttemptedAtDesc(7L))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByUserIdAndSuccessFalseAndAttemptedAtAfter(
                7L,
                now.minusMinutes(15)))
                .thenReturn(0L);
        when(attemptRepository.findTopByIpAddressAndSuccessTrueOrderByAttemptedAtDesc("203.0.113.10"))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByIpAddressAndSuccessFalseAndAttemptedAtAfter(
                "203.0.113.10",
                now.minusMinutes(15)))
                .thenReturn(0L);

        assertFalse(attemptService.isRateLimited(7L, "203.0.113.10", now));
    }

    @Test
    void isRateLimitedShouldCountOnlyFailuresAfterLatestSuccess() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-02T12:00:00Z");
        OffsetDateTime successAt = OffsetDateTime.parse("2026-08-02T11:58:00Z");
        DeviceActivationAttempt success = new DeviceActivationAttempt();
        success.setAttemptedAt(successAt);

        when(attemptRepository.findTopByUserIdAndSuccessTrueOrderByAttemptedAtDesc(7L))
                .thenReturn(Optional.of(success));
        when(attemptRepository.countByUserIdAndSuccessFalseAndAttemptedAtAfter(7L, successAt))
                .thenReturn(0L);
        when(attemptRepository.findTopByIpAddressAndSuccessTrueOrderByAttemptedAtDesc("203.0.113.10"))
                .thenReturn(Optional.empty());
        when(attemptRepository.countByIpAddressAndSuccessFalseAndAttemptedAtAfter(
                "203.0.113.10",
                now.minusMinutes(15)))
                .thenReturn(0L);

        assertFalse(attemptService.isRateLimited(7L, "203.0.113.10", now));
        verify(attemptRepository).countByUserIdAndSuccessFalseAndAttemptedAtAfter(7L, successAt);
    }

    @Test
    void recordSuccessShouldPersistResetPointWithoutFailureCategory() {
        OffsetDateTime attemptedAt = OffsetDateTime.parse("2026-08-02T12:00:00Z");

        attemptService.recordSuccess(
                7L,
                25L,
                77L,
                "203.0.113.10",
                "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                attemptedAt);

        ArgumentCaptor<DeviceActivationAttempt> captor =
                ArgumentCaptor.forClass(DeviceActivationAttempt.class);
        verify(attemptRepository).save(captor.capture());

        DeviceActivationAttempt attempt = captor.getValue();
        assertTrue(attempt.getSuccess());
        assertNull(attempt.getFailureCategory());
        assertEquals(attemptedAt, attempt.getAttemptedAt());
    }

    @Test
    void blockedUntilShouldUseConfiguredWindow() {
        OffsetDateTime now = OffsetDateTime.parse("2026-08-02T12:00:00Z");

        assertEquals(
                OffsetDateTime.parse("2026-08-02T12:15:00Z"),
                attemptService.blockedUntil(now));
    }
}
