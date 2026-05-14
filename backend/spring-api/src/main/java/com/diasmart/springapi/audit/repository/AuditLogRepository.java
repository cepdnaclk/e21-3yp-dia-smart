package com.diasmart.springapi.audit.repository;

import com.diasmart.springapi.audit.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository
        extends JpaRepository<AuditLog, Long> {

    long countByActionTypeAndEntityTypeAndEntityId(
            String actionType,
            String entityType,
            Long entityId
    );
}
