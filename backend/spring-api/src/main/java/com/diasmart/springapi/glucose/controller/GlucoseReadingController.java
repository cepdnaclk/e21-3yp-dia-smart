package com.diasmart.springapi.glucose.controller;

import com.diasmart.springapi.glucose.dto.CreateManualGlucoseReadingRequest;
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponse;
import com.diasmart.springapi.glucose.service.GlucoseReadingService;
import com.diasmart.springapi.shared.dto.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class GlucoseReadingController {

    private final GlucoseReadingService glucoseReadingService;

    public GlucoseReadingController(
            GlucoseReadingService glucoseReadingService
    ) {
        this.glucoseReadingService = glucoseReadingService;
    }

    @GetMapping("/{patientId}/glucose-readings")
    public ResponseEntity<ApiResponse<Page<GlucoseReadingResponse>>>
    getPatientReadings(
            @PathVariable Long patientId,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {

        Page<GlucoseReadingResponse> response =
                glucoseReadingService.getPatientReadings(
                        patientId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Glucose readings retrieved successfully",
                        response
                )
        );
    }

    @PostMapping("/{patientId}/manual-glucose-readings")
    public ResponseEntity<ApiResponse<GlucoseReadingResponse>>
    createManualReading(

            @PathVariable Long patientId,

            @Valid
            @RequestBody
            CreateManualGlucoseReadingRequest request
    ) {
        System.out.println("POST CONTROLLER HIT");

        GlucoseReadingResponse response =
                glucoseReadingService.createManualReading(
                        patientId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Manual glucose reading created successfully",
                        response
                )
        );
    }

    @PostMapping("/test-post")
    public ResponseEntity<String> testPost() {

        System.out.println("TEST POST HIT");

        return ResponseEntity.ok("POST WORKS");
    }
}