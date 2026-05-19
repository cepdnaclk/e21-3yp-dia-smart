package com.diasmart.springapi.dose_schedules.controller;

import com.diasmart.springapi.dose_schedules.dto.DoseScheduleResponse;
import com.diasmart.springapi.dose_schedules.service.DoseScheduleService;
import com.diasmart.springapi.shared.dto.ApiResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.diasmart.springapi.dose_schedules.dto.CreateDoseScheduleRequest;
import com.diasmart.springapi.dose_schedules.dto.UpdateDoseScheduleRequest;

import java.util.List;

import com.diasmart.springapi.dose_schedules.dto.ScheduleAdherenceResponse;

@RestController
@RequestMapping("/api/v1")
public class DoseScheduleController {

    private final DoseScheduleService doseScheduleService;

    public DoseScheduleController(
            DoseScheduleService doseScheduleService
    ) {
        this.doseScheduleService = doseScheduleService;
    }

    @GetMapping("/patients/{patientId}/dose-schedules")
    public ResponseEntity<ApiResponse<Page<DoseScheduleResponse>>>
    getDoseSchedules(

            @PathVariable Long patientId,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {

        Page<DoseScheduleResponse> response =
                doseScheduleService.getDoseSchedules(
                        patientId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dose schedules retrieved successfully",
                        response
                )
        );
    }

    @PostMapping("/patients/{patientId}/dose-schedules")
        public ResponseEntity<ApiResponse<DoseScheduleResponse>>
        createDoseSchedule(

                @PathVariable Long patientId,

                @RequestBody
                CreateDoseScheduleRequest request
        ) {

        DoseScheduleResponse response =
                doseScheduleService.createDoseSchedule(
                        patientId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dose schedule created successfully",
                        response
                )
        );
        }
    @PatchMapping("/dose-schedules/{scheduleId}")
        public ResponseEntity<ApiResponse<DoseScheduleResponse>>
        updateDoseSchedule(

                @PathVariable Long scheduleId,

                @RequestBody
                UpdateDoseScheduleRequest request
        ) {

        DoseScheduleResponse response =
                doseScheduleService.updateDoseSchedule(
                        scheduleId,
                        request
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dose schedule updated successfully",
                        response
                )
        );
        }

        @DeleteMapping("/dose-schedules/{scheduleId}")
        public ResponseEntity<ApiResponse<Void>>
        deactivateDoseSchedule(

                @PathVariable Long scheduleId
        ) {

        doseScheduleService.deactivateDoseSchedule(
                scheduleId
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Dose schedule deactivated successfully"
                )
        );
        }

        @GetMapping("/patients/{patientId}/schedule-adherence")
        public ResponseEntity<
                ApiResponse<List<ScheduleAdherenceResponse>>
                >
        getTodayScheduleAdherence(

                @PathVariable Long patientId
        ) {

        List<ScheduleAdherenceResponse> response =
                doseScheduleService
                        .getTodayScheduleAdherence(
                                patientId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Schedule adherence retrieved successfully",
                        response
                )
        );
        }
}