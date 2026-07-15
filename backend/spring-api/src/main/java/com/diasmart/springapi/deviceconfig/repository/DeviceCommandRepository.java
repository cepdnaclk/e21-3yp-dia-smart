package com.diasmart.springapi.deviceconfig.repository;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {
}
