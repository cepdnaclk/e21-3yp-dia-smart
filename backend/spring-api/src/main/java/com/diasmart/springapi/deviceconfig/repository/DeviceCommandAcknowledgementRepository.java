package com.diasmart.springapi.deviceconfig.repository;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommandAcknowledgement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceCommandAcknowledgementRepository extends JpaRepository<DeviceCommandAcknowledgement, Long> {
}
