package com.diasmart.springapi.raw_events.repository;

import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RawDeviceEventRepository
        extends JpaRepository<RawDeviceEvent, Long> {

    boolean existsByDeviceUidAndSourceEventId(
            String deviceUid,
            String sourceEventId
    );
}
