package com.diasmart.springapi.deviceconfig.repository;

import com.diasmart.springapi.deviceconfig.entity.DeviceCommand;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

public interface DeviceCommandRepository extends JpaRepository<DeviceCommand, Long> {

    Optional<DeviceCommand> findByCommandUid(String commandUid);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE DeviceCommand c
            SET c.commandStatus = 'SENT',
                c.lastAttemptAt = :now,
                c.lastError = null
            WHERE c.commandId = :commandId
              AND c.commandType = 'WIFI_CONFIGURATION'
              AND c.retryCount < :maxRetries
              AND (
                    c.commandStatus = 'PENDING'
                    OR (
                        c.commandStatus = 'FAILED'
                        AND (c.nextRetryAt IS NULL OR c.nextRetryAt <= :now)
                    )
                    OR (
                        c.commandStatus = 'SENT'
                        AND c.lastAttemptAt IS NOT NULL
                        AND c.lastAttemptAt <= :staleSentBefore
                    )
              )
            """)
    int claimRecoverableWifiCommand(
            @Param("commandId") Long commandId,
            @Param("now") OffsetDateTime now,
            @Param("staleSentBefore") OffsetDateTime staleSentBefore,
            @Param("maxRetries") int maxRetries
    );

    @Query("""
            SELECT c.commandId
            FROM DeviceCommand c
            WHERE c.commandType = 'WIFI_CONFIGURATION'
              AND c.retryCount < :maxRetries
              AND (
                    c.commandStatus = 'PENDING'
                    OR (
                        c.commandStatus = 'FAILED'
                        AND (c.nextRetryAt IS NULL OR c.nextRetryAt <= :now)
                    )
                    OR (
                        c.commandStatus = 'SENT'
                        AND c.lastAttemptAt IS NOT NULL
                        AND c.lastAttemptAt <= :staleSentBefore
                    )
              )
            ORDER BY c.createdAt ASC
            """)
    List<Long> findRecoverableWifiCommandIds(
            @Param("now") OffsetDateTime now,
            @Param("staleSentBefore") OffsetDateTime staleSentBefore,
            @Param("maxRetries") int maxRetries,
            Pageable pageable
    );

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("""
            UPDATE DeviceCommand c
            SET c.commandStatus = 'EXPIRED',
                c.lastError = 'COMMAND_SUPERSEDED',
                c.nextRetryAt = null
            WHERE c.commandType = 'WIFI_CONFIGURATION'
              AND c.deviceConfigurationId = :configurationId
              AND c.configurationVersion < :configurationVersion
              AND c.commandStatus IN ('PENDING', 'FAILED', 'SENT')
            """)
    int expireSupersededWifiCommands(
            @Param("configurationId") Long configurationId,
            @Param("configurationVersion") Integer configurationVersion
    );
}
