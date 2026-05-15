package com.diasmart.springapi.dashboard.controller;

import com.diasmart.springapi.dashboard.dto.DashboardSummaryResponse;
import com.diasmart.springapi.dashboard.service.DashboardService;
import com.diasmart.springapi.shared.dto.ApiResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(
            DashboardService dashboardService
    ) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/{patientId}/dashboard-summary")
    public ResponseEntity<
            ApiResponse<DashboardSummaryResponse>
    >
    getDashboardSummary(

            @PathVariable Long patientId
    ) {

        DashboardSummaryResponse response =
                dashboardService
                        .getDashboardSummary(
                                patientId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dashboard summary retrieved successfully",
                        response
                )
        );
    }
}