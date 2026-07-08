package com.diasmart.springapi.devices.repository;

import com.diasmart.springapi.devices.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceRepository
        extends JpaRepository<Device, Long> {

    Optional<Device> findByDeviceUid(String deviceUid);

    boolean existsByDeviceUid(String deviceUid);

    List<Device> findAllByOrderByDeviceIdAsc();

    Optional<Device> findByAwsThingName(String awsThingName);

    Optional<Device> findByMqttClientId(String mqttClientId);

    Optional<Device> findByMacAddress(String macAddress);

    Optional<Device> findBySerialNumber(String serialNumber);
}
