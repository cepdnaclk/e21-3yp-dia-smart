package com.diasmart.springapi.patients.controller;

// Glucose response DTO
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponseDto;

// Glucose business logic service
import com.diasmart.springapi.glucose.service.GlucoseService;

// Dose response DTO
import com.diasmart.springapi.dose.dto.DoseEventResponseDto;

// Dose business logic service
import com.diasmart.springapi.dose.service.DoseService;

// Standard API response wrapper
import com.diasmart.springapi.shared.dto.ApiResponse;

// Auto-generates constructor for final fields
import lombok.RequiredArgsConstructor;

// Used for paginated responses
import org.springframework.data.domain.Page;

// Used for pagination request details
import org.springframework.data.domain.Pageable;

// Spring REST controller annotations
import org.springframework.web.bind.annotation.*;
//Inventory response DTO
import com.diasmart.springapi.inventory.dto.InventoryReadingResponseDto;
// Inventory business logic service
import com.diasmart.springapi.inventory.service.InventoryService;
// Storage response DTO
import com.diasmart.springapi.storage.dto.StorageReadingResponseDto;
// Storage business logic service
import com.diasmart.springapi.storage.service.StorageService;


// Marks this class as REST API controller
@RestController

// Base URL for all patient APIs
@RequestMapping("/api/v1/patients")

// Automatically creates constructor for services
@RequiredArgsConstructor
public class PatientTelemetryController {

    // Service handling glucose operations
    private final GlucoseService glucoseService;

    // Service handling dose operations
    private final DoseService doseService;

    // Service handling inventory operations
    private final InventoryService inventoryService;

    // Service handling storage operations
    private final StorageService storageService;

    // Endpoint to fetch patient glucose history
    @GetMapping("/{patientId}/glucose-readings")
    public ApiResponse<Page<GlucoseReadingResponseDto>>
    getPatientGlucoseReadings(

            // Patient ID from URL
            @PathVariable Long patientId,

            // Pagination info from request
            Pageable pageable
    ) {

        // Return standardized API response
        return ApiResponse.success(
                "Glucose readings fetched successfully",

                // Call service layer
                glucoseService.getPatientGlucoseReadings(
                        patientId,
                        pageable
                )
        );
    }


    // Endpoint to fetch patient dose history
    @GetMapping("/{patientId}/dose-events")
    public ApiResponse<Page<DoseEventResponseDto>>
    getPatientDoseEvents(

            // Patient ID from URL
            @PathVariable Long patientId,

            // Pagination info from request
            Pageable pageable
    ) {

        // Return standardized API response
        return ApiResponse.success(
                "Dose events fetched successfully",

                // Call service layer
                doseService.getPatientDoseEvents(
                        patientId,
                        pageable
                )
        );
    }

    // Fetch patient inventory history
    @GetMapping("/{patientId}/inventory-readings")
    public ApiResponse<Page<InventoryReadingResponseDto>>
    getPatientInventoryReadings(

        @PathVariable Long patientId,

        Pageable pageable
    ) {

    return ApiResponse.success(
            "Inventory readings fetched successfully",

            inventoryService
                    .getPatientInventoryReadings(
                            patientId,
                            pageable
                    )
    );
    }

    // Fetch patient storage telemetry history
    @GetMapping("/{patientId}/storage-readings")
    public ApiResponse<Page<StorageReadingResponseDto>>
    getPatientStorageReadings(

        @PathVariable Long patientId,

        Pageable pageable
    ) {

    return ApiResponse.success(
            "Storage readings fetched successfully",

            storageService
                    .getPatientStorageReadings(
                            patientId,
                            pageable
                    )
    );
    }
}