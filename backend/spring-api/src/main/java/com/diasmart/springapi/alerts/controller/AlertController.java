package com.diasmart.springapi.alerts.controller;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.alerts.service.AlertService;
import com.diasmart.springapi.shared.dto.ApiResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/alerts")
public class AlertController {

    private final AlertService alertService;

    public AlertController(
            AlertService alertService
    ) {
        this.alertService = alertService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AlertResponse>>>
    getAlerts(

            @PageableDefault(size = 20)
            Pageable pageable
    ) {

        Page<AlertResponse> response =
                alertService.getAlerts(pageable);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Alerts retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/{alertId}")
    public ResponseEntity<ApiResponse<AlertResponse>>
    getAlert(

            @PathVariable Long alertId
    ) {

        AlertResponse response =
                alertService.getAlert(alertId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Alert retrieved successfully",
                        response
                )
        );
    }

    @PatchMapping("/{alertId}/acknowledge")
    public ResponseEntity<ApiResponse<AlertResponse>>
    acknowledgeAlert(

            @PathVariable Long alertId
    ) {

        AlertResponse response =
                alertService
                        .acknowledgeAlert(alertId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Alert acknowledged successfully",
                        response
                )
        );
    }
}