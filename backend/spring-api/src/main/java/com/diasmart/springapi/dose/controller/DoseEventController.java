package com.diasmart.springapi.dose.controller;

import com.diasmart.springapi.dose.dto.CreateManualDoseEventRequest;
import com.diasmart.springapi.dose.dto.DoseEventResponse;
import com.diasmart.springapi.dose.service.DoseEventService;
import com.diasmart.springapi.shared.dto.ApiResponse;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class DoseEventController {

    private final DoseEventService doseEventService;

    public DoseEventController(
            DoseEventService doseEventService
    ) {
        this.doseEventService = doseEventService;
    }

    @GetMapping("/{patientId}/dose-events")
    public ResponseEntity<ApiResponse<Page<DoseEventResponse>>>
    getDoseEvents(

            @PathVariable Long patientId,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {

        Page<DoseEventResponse> response =
                doseEventService.getDoseEvents(
                        patientId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dose events retrieved successfully",
                        response
                )
        );
    }

    @PostMapping("/{patientId}/manual-dose-events")
    public ResponseEntity<ApiResponse<DoseEventResponse>>
    createManualDoseEvent(

            @PathVariable Long patientId,

            @Valid
            @RequestBody
            CreateManualDoseEventRequest request
    ) {

        DoseEventResponse response =
                doseEventService.createManualDoseEvent(
                        patientId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Manual dose event created successfully",
                        response
                )
        );
    }

    
}