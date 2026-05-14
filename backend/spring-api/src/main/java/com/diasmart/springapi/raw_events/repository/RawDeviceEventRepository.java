package com.diasmart.springapi.raw_events.repository;

import com.diasmart.springapi.raw_events.entity.RawDeviceEvent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RawDeviceEventRepository
        extends JpaRepository<RawDeviceEvent, Long> {

    boolean existsByDeviceUidAndSourceEventId(
            String deviceUid,
            String sourceEventId
    );

    @Query(value = """
            SELECT r.*
            FROM raw_device_events r
            LEFT JOIN device_health_logs h
                ON h.raw_event_id = r.raw_event_id
            WHERE r.device_id = :deviceId
               OR h.device_id = :deviceId
            ORDER BY r.received_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<RawDeviceEvent> findLatestForDeviceDiagnostics(
            @Param("deviceId") Long deviceId
    );

    @Query(value = """
            SELECT COUNT(DISTINCT r.raw_event_id)
            FROM raw_device_events r
            LEFT JOIN device_health_logs h
                ON h.raw_event_id = r.raw_event_id
            WHERE r.device_id = :deviceId
               OR h.device_id = :deviceId
            """, nativeQuery = true)
    long countEventsForDeviceDiagnostics(
            @Param("deviceId") Long deviceId
    );

    @Query(value = """
            SELECT COUNT(DISTINCT r.raw_event_id)
            FROM raw_device_events r
            LEFT JOIN device_health_logs h
                ON h.raw_event_id = r.raw_event_id
            WHERE (r.device_id = :deviceId OR h.device_id = :deviceId)
              AND COALESCE(CAST(r.payload ->> 'replayedEvent' AS boolean), false) = true
            """, nativeQuery = true)
    long countReplayedEventsForDeviceDiagnostics(
            @Param("deviceId") Long deviceId
    );
}
