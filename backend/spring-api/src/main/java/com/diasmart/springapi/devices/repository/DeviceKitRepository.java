package com.diasmart.springapi.devices.repository;

import com.diasmart.springapi.devices.entity.DeviceKit;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceKitRepository extends JpaRepository<DeviceKit, Long> {

    Optional<DeviceKit> findByKitUid(String kitUid);

    boolean existsByKitUid(String kitUid);

    List<DeviceKit> findAllByOrderByCreatedAtDesc();

    List<DeviceKit> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
}
