package com.diasmart.springapi.deviceconfig.repository;

import com.diasmart.springapi.deviceconfig.entity.DeviceConfiguration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceConfigurationRepository extends JpaRepository<DeviceConfiguration, Long> {

    Optional<DeviceConfiguration> findByOuterDeviceId(Long outerDeviceId);

    boolean existsByOuterDeviceId(Long outerDeviceId);

    List<DeviceConfiguration> findByPatientIdInOrderByUpdatedAtDesc(Collection<Long> patientIds);

    Optional<DeviceConfiguration> findByConfigurationId(Long configurationId);
}
