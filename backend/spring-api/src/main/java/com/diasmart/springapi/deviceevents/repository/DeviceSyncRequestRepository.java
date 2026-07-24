package com.diasmart.springapi.deviceevents.repository;

import com.diasmart.springapi.deviceevents.entity.DeviceSyncRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceSyncRequestRepository extends JpaRepository<DeviceSyncRequest, Long> {
}
