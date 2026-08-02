package com.diasmart.springapi.devices.repository;

import com.diasmart.springapi.devices.entity.DeviceKitDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface DeviceKitDeviceRepository extends JpaRepository<DeviceKitDevice, Long> {

    List<DeviceKitDevice> findByDeviceKitId(Long deviceKitId);

    List<DeviceKitDevice> findByDeviceKitIdIn(Collection<Long> deviceKitIds);

    List<DeviceKitDevice> findByDeviceIdIn(Collection<Long> deviceIds);

    Optional<DeviceKitDevice> findByDeviceId(Long deviceId);

    Optional<DeviceKitDevice> findByDeviceKitIdAndKitDeviceRole(Long deviceKitId, String kitDeviceRole);

    boolean existsByDeviceId(Long deviceId);

    boolean existsByDeviceKitIdAndKitDeviceRole(Long deviceKitId, String kitDeviceRole);
}
