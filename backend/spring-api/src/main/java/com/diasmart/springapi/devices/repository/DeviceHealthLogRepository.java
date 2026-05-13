package com.diasmart.springapi.devices.repository;

import com.diasmart.springapi.devices.entity.DeviceHealthLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceHealthLogRepository
        extends JpaRepository<DeviceHealthLog, Long> {

    DeviceHealthLog
    findTopByDeviceIdOrderByMeasuredAtDesc(
            Long deviceId
    );
}
