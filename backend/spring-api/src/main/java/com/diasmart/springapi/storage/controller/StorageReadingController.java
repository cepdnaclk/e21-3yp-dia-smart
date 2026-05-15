package com.diasmart.springapi.storage.controller;

import com.diasmart.springapi.shared.dto.ApiResponse;
import com.diasmart.springapi.storage.dto.StorageReadingResponse;
import com.diasmart.springapi.storage.service.StorageReadingService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class StorageReadingController {

    private final StorageReadingService storageReadingService;

    public StorageReadingController(
            StorageReadingService storageReadingService
    ) {
        this.storageReadingService = storageReadingService;
    }

    @GetMapping("/{patientId}/storage-readings")
    public ResponseEntity<ApiResponse<Page<StorageReadingResponse>>>
    getStorageHistory(

            @PathVariable Long patientId,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {

        Page<StorageReadingResponse> response =
                storageReadingService.getStorageHistory(
                        patientId,
                        pageable
                );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Storage history retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/{patientId}/storage-readings/latest")
    public ResponseEntity<ApiResponse<StorageReadingResponse>>
    getLatestStorageReading(

            @PathVariable Long patientId
    ) {

        StorageReadingResponse response =
                storageReadingService
                        .getLatestStorageReading(
                                patientId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Latest storage reading retrieved successfully",
                        response
                )
        );
    }
}