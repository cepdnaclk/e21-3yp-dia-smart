package com.diasmart.springapi.analytics.controller;

import com.diasmart.springapi.analytics.dto.AdherenceAnalyticsResponse;
import com.diasmart.springapi.analytics.service.AdherenceAnalyticsService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AdherenceAnalyticsService adherenceAnalyticsService;

    public AnalyticsController(AdherenceAnalyticsService adherenceAnalyticsService) {
        this.adherenceAnalyticsService = adherenceAnalyticsService;
    }

    @GetMapping("/adherence")
    public ResponseEntity<ApiResponse<AdherenceAnalyticsResponse>> getAdherenceAnalytics(
            @RequestParam Long patientId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        AdherenceAnalyticsResponse response =
                adherenceAnalyticsService.getAdherenceAnalytics(patientId, startDate, endDate);

        return ResponseEntity.ok(
                ApiResponse.success("Adherence analytics retrieved successfully", response));
    }
}
