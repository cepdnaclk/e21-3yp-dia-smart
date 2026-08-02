package com.diasmart.springapi.devices.repository;

import com.diasmart.springapi.devices.entity.DeviceActivationAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;
import java.util.Optional;

public interface DeviceActivationAttemptRepository extends JpaRepository<DeviceActivationAttempt, Long> {

    long countByUserIdAndSuccessFalseAndAttemptedAtAfter(Long userId, OffsetDateTime attemptedAt);

    long countByIpAddressAndSuccessFalseAndAttemptedAtAfter(String ipAddress, OffsetDateTime attemptedAt);

    Optional<DeviceActivationAttempt> findTopByUserIdAndSuccessTrueOrderByAttemptedAtDesc(Long userId);

    Optional<DeviceActivationAttempt> findTopByIpAddressAndSuccessTrueOrderByAttemptedAtDesc(String ipAddress);
}
