package com.diasmart.springapi.devices.service;

import com.diasmart.springapi.devices.config.DeviceActivationProperties;
import com.diasmart.springapi.devices.entity.DeviceActivationAttempt;
import com.diasmart.springapi.devices.entity.DeviceActivationFailureCategory;
import com.diasmart.springapi.devices.repository.DeviceActivationAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Service
public class DeviceActivationAttemptService {

    private final DeviceActivationAttemptRepository attemptRepository;
    private final DeviceActivationProperties properties;

    public DeviceActivationAttemptService(
            DeviceActivationAttemptRepository attemptRepository,
            DeviceActivationProperties properties) {
        this.attemptRepository = attemptRepository;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public boolean isRateLimited(Long userId, String ipAddress, OffsetDateTime now) {
        OffsetDateTime windowStart = now.minusMinutes(properties.getWindowMinutes());

        if (userId != null) {
            OffsetDateTime userStart = latestUserSuccessStart(userId, windowStart);
            long userFailures = attemptRepository.countByUserIdAndSuccessFalseAndAttemptedAtAfter(
                    userId,
                    userStart);

            if (userFailures >= properties.getMaxFailuresPerUser()) {
                return true;
            }
        }

        if (ipAddress != null) {
            OffsetDateTime ipStart = latestIpSuccessStart(ipAddress, windowStart);
            long ipFailures = attemptRepository.countByIpAddressAndSuccessFalseAndAttemptedAtAfter(
                    ipAddress,
                    ipStart);

            return ipFailures >= properties.getMaxFailuresPerIp();
        }

        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(
            Long userId,
            Long patientId,
            Long kitId,
            String ipAddress,
            DeviceActivationFailureCategory failureCategory,
            String requestFingerprint,
            OffsetDateTime attemptedAt,
            OffsetDateTime blockedUntil) {
        DeviceActivationAttempt attempt = new DeviceActivationAttempt();
        attempt.setUserId(userId);
        attempt.setPatientId(patientId);
        attempt.setKitId(kitId);
        attempt.setIpAddress(ipAddress);
        attempt.setSuccess(false);
        attempt.setFailureCategory(failureCategory);
        attempt.setRequestFingerprint(requestFingerprint);
        attempt.setAttemptedAt(defaultNow(attemptedAt));
        attempt.setBlockedUntil(blockedUntil);

        attemptRepository.save(attempt);
    }

    @Transactional
    public void recordSuccess(
            Long userId,
            Long patientId,
            Long kitId,
            String ipAddress,
            String requestFingerprint,
            OffsetDateTime attemptedAt) {
        DeviceActivationAttempt attempt = new DeviceActivationAttempt();
        attempt.setUserId(userId);
        attempt.setPatientId(patientId);
        attempt.setKitId(kitId);
        attempt.setIpAddress(ipAddress);
        attempt.setSuccess(true);
        attempt.setRequestFingerprint(requestFingerprint);
        attempt.setAttemptedAt(defaultNow(attemptedAt));

        attemptRepository.save(attempt);
    }

    public OffsetDateTime blockedUntil(OffsetDateTime now) {
        return now.plusMinutes(properties.getWindowMinutes());
    }

    private OffsetDateTime latestUserSuccessStart(Long userId, OffsetDateTime windowStart) {
        return attemptRepository
                .findTopByUserIdAndSuccessTrueOrderByAttemptedAtDesc(userId)
                .map(DeviceActivationAttempt::getAttemptedAt)
                .filter(successAt -> successAt.isAfter(windowStart))
                .orElse(windowStart);
    }

    private OffsetDateTime latestIpSuccessStart(String ipAddress, OffsetDateTime windowStart) {
        return attemptRepository
                .findTopByIpAddressAndSuccessTrueOrderByAttemptedAtDesc(ipAddress)
                .map(DeviceActivationAttempt::getAttemptedAt)
                .filter(successAt -> successAt.isAfter(windowStart))
                .orElse(windowStart);
    }

    private OffsetDateTime defaultNow(OffsetDateTime attemptedAt) {
        return attemptedAt == null ? OffsetDateTime.now(ZoneOffset.UTC) : attemptedAt;
    }
}
