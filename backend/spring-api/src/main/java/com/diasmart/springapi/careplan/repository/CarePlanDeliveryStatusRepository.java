package com.diasmart.springapi.careplan.repository;

import com.diasmart.springapi.careplan.entity.CarePlanDeliveryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CarePlanDeliveryStatusRepository extends JpaRepository<CarePlanDeliveryStatus, Long> {

    Optional<CarePlanDeliveryStatus> findTopBySnapshotIdAndOuterDeviceIdOrderByCreatedAtDesc(Long snapshotId, Long outerDeviceId);
}
