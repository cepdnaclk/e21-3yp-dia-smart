package com.diasmart.springapi.inventory.service;

import com.diasmart.springapi.inventory.dto.InventoryReadingResponseDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// Inventory business operations
public interface InventoryService {

    // Fetch patient inventory history
    Page<InventoryReadingResponseDto>
    getPatientInventoryReadings(

            Long patientId,

            Pageable pageable
    );
}