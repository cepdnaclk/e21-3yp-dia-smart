package com.diasmart.springapi.inventory.service.impl;

import com.diasmart.springapi.inventory.dto.InventoryReadingResponseDto;
import com.diasmart.springapi.inventory.entity.InventoryReading;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.inventory.service.InventoryService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;


// Registers as Spring service
@Service

// Auto-generates constructor
@RequiredArgsConstructor
public class InventoryServiceImpl
        implements InventoryService {

    // Repository dependency
    private final InventoryReadingRepository
            inventoryReadingRepository;


    // Fetch inventory history
    @Override
    public Page<InventoryReadingResponseDto>
    getPatientInventoryReadings(

            Long patientId,

            Pageable pageable
    ) {

        return inventoryReadingRepository
                .findByPatientIdOrderByMeasuredAtDesc(
                        patientId,
                        pageable
                )
                .map(this::mapToDto);
    }


    // Convert entity to DTO
    private InventoryReadingResponseDto
    mapToDto(

            InventoryReading reading
    ) {

        return InventoryReadingResponseDto
                .builder()

                .inventoryReadingId(
                        reading.getInventoryReadingId()
                )

                .patientId(
                        reading.getPatientId()
                )

                .weightG(
                        reading.getWeightG()
                )

                .estimatedUnitsRemaining(
                        reading.getEstimatedUnitsRemaining()
                )

                .estimatedRemainingPercent(
                        reading.getEstimatedRemainingPercent()
                )

                .inventoryStatus(
                        reading.getInventoryStatus()
                )

                .measuredAt(
                        reading.getMeasuredAt()
                )

                .build();
    }
}