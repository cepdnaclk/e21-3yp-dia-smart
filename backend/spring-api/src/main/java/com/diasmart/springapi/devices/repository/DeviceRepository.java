package com.diasmart.springapi.devices.repository;

import com.diasmart.springapi.devices.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DeviceRepository
        extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceUid(String deviceUid);
}
