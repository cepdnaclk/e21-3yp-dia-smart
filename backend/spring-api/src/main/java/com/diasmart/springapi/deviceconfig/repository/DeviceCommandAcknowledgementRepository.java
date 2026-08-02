package com.diasmart.springapi.deviceconfig.repository;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommandAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceCommandAcknowledgementRepository extends JpaRepository<DeviceCommandAcknowledgement, Long> {

    boolean existsByAckDeduplicationKey(String ackDeduplicationKey);

    Optional<DeviceCommandAcknowledgement> findTopByCommandIdOrderByAcknowledgedAtDesc(Long commandId);
}
