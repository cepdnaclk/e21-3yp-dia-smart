package com.diasmart.springapi.deviceconfig.repository;

import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceConfigurationRepository extends JpaRepository<DeviceConfiguration, Long> {

    Optional<DeviceConfiguration> findByOuterDeviceId(Long outerDeviceId);

    boolean existsByOuterDeviceId(Long outerDeviceId);
}
