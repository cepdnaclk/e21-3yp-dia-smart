package com.diasmart.springapi.admin.controller;

import com.diasmart.springapi.admin.dto.AuditLogResponse;
import com.diasmart.springapi.admin.service.AdminAuditService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/audit-logs")
public class AdminAuditController {

    private final AdminAuditService adminAuditService;

    public AdminAuditController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt",
                    direction = org.springframework.data.domain.Sort.Direction.DESC)
            Pageable pageable) {

        Page<AuditLogResponse> response = adminAuditService.getAuditLogs(pageable);

        return ResponseEntity.ok(
                ApiResponse.success("Audit logs retrieved successfully", response));
    }
}
