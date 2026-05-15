package com.diasmart.springapi.inventory.controller;

import com.diasmart.springapi.inventory.dto.InventoryReadingResponse;
import com.diasmart.springapi.inventory.service.InventoryReadingService;
import com.diasmart.springapi.shared.dto.ApiResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/patients")
public class InventoryReadingController {

    private final InventoryReadingService inventoryReadingService;

    public InventoryReadingController(
            InventoryReadingService inventoryReadingService
    ) {
        this.inventoryReadingService =
                inventoryReadingService;
    }

    @GetMapping("/{patientId}/inventory-readings")
    public ResponseEntity<ApiResponse<Page<InventoryReadingResponse>>>
    getInventoryHistory(

            @PathVariable Long patientId,

            @PageableDefault(size = 20)
            Pageable pageable
    ) {

        Page<InventoryReadingResponse> response =
                inventoryReadingService
                        .getInventoryHistory(
                                patientId,
                                pageable
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Inventory history retrieved successfully",
                        response
                )
        );
    }

    @GetMapping("/{patientId}/inventory-readings/latest")
    public ResponseEntity<ApiResponse<InventoryReadingResponse>>
    getLatestInventoryReading(

            @PathVariable Long patientId
    ) {

        InventoryReadingResponse response =
                inventoryReadingService
                        .getLatestInventoryReading(
                                patientId
                        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        "Latest inventory reading retrieved successfully",
                        response
                )
        );
    }
}