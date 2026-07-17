package com.diasmart.springapi.deviceevents.repository;

import com.diasmart.springapi.deviceevents.entity.DeviceTelemetryEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTelemetryEventRepository extends JpaRepository<DeviceTelemetryEvent, Long> {

    boolean existsByEventId(String eventId);
}
