package com.diasmart.springapi.admin.service;

import com.diasmart.springapi.admin.dto.AuditLogResponse;
import com.diasmart.springapi.audit.entity.AuditLog;
import com.diasmart.springapi.audit.repository.AuditLogRepository;
import com.diasmart.springapi.shared.enums.UserRole;
import com.diasmart.springapi.shared.security.CurrentUserService;
import com.diasmart.springapi.users.entity.AppUser;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuditService {

    private final AuditLogRepository auditLogRepository;
    private final CurrentUserService currentUserService;

    public AdminAuditService(
            AuditLogRepository auditLogRepository,
            CurrentUserService currentUserService) {
        this.auditLogRepository = auditLogRepository;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Page<AuditLogResponse> getAuditLogs(Pageable pageable) {
        requireAdmin();
        return auditLogRepository.findAll(pageable).map(this::mapToResponse);
    }

    private AuditLogResponse mapToResponse(AuditLog log) {
        AuditLogResponse response = new AuditLogResponse();
        response.setAuditLogId(log.getAuditLogId());
        response.setUserId(log.getUserId());
        response.setPatientId(log.getPatientId());
        response.setActionType(log.getActionType());
        response.setEntityType(log.getEntityType());
        response.setEntityId(log.getEntityId());
        response.setIpAddress(log.getIpAddress());
        response.setDetails(log.getDetails());
        response.setCreatedAt(log.getCreatedAt());
        return response;
    }

    private void requireAdmin() {
        AppUser currentUser = currentUserService.getCurrentUser();
        if (currentUser.getRole() != UserRole.ADMIN) {
            throw new AccessDeniedException("Only admins can access audit logs");
        }
    }
}
