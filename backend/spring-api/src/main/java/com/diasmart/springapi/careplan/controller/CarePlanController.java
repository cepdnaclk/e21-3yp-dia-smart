package com.diasmart.springapi.careplan.controller;

import com.diasmart.springapi.careplan.dto.CarePlanResponse;
import com.diasmart.springapi.careplan.service.CarePlanService;
import com.diasmart.springapi.shared.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients/{patientId}/care-plans")
public class CarePlanController {

    private final CarePlanService carePlanService;

    public CarePlanController(CarePlanService carePlanService) {
        this.carePlanService = carePlanService;
    }

    @PostMapping("/generate-send")
    public ResponseEntity<ApiResponse<CarePlanResponse>> generateAndSend(@PathVariable Long patientId) {
        CarePlanResponse response = carePlanService.generateAndPublish(patientId);

        return ResponseEntity.ok(
                ApiResponse.success("Care Plan generated and publishing initiated", response)
        );
    }

    @GetMapping("/current")
    public ResponseEntity<ApiResponse<CarePlanResponse>> getCurrent(@PathVariable Long patientId) {
        CarePlanResponse response = carePlanService.getCurrent(patientId);

        return ResponseEntity.ok(
                ApiResponse.success("Current Care Plan retrieved successfully", response)
        );
    }

    @PostMapping("/current/send")
    public ResponseEntity<ApiResponse<CarePlanResponse>> resendCurrent(@PathVariable Long patientId) {
        CarePlanResponse response = carePlanService.resendCurrent(patientId);

        return ResponseEntity.ok(
                ApiResponse.success("Current Care Plan publishing initiated", response)
        );
    }
}
