package com.diasmart.springapi.inventory.service;

import com.diasmart.springapi.inventory.dto.InventoryReadingResponse;
import com.diasmart.springapi.inventory.entity.InventoryReading;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;
import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class InventoryReadingService {

    private final InventoryReadingRepository inventoryReadingRepository;

    private final AuthorizationService authorizationService;

    public InventoryReadingService(
            InventoryReadingRepository inventoryReadingRepository,
            AuthorizationService authorizationService
    ) {
        this.inventoryReadingRepository =
                inventoryReadingRepository;

        this.authorizationService =
                authorizationService;
    }

    public Page<InventoryReadingResponse>
    getInventoryHistory(
            Long patientId,
            Pageable pageable
    ) {

        authorizationService.authorize(
                Permission.READ_INVENTORY_HISTORY,
                patientId
        );

        return inventoryReadingRepository
                .findByPatientId(patientId, pageable)
                .map(this::mapToResponse);
    }

    public InventoryReadingResponse
    getLatestInventoryReading(
            Long patientId
    ) {

        authorizationService.authorize(
                Permission.READ_INVENTORY_HISTORY,
                patientId
        );

        InventoryReading reading =
                inventoryReadingRepository
                        .findTopByPatientIdOrderByMeasuredAtDesc(
                                patientId
                        );

        if (reading == null) {
            return null;
        }

        return mapToResponse(reading);
    }

    private InventoryReadingResponse mapToResponse(
            InventoryReading reading
    ) {

        InventoryReadingResponse response =
                new InventoryReadingResponse();

        response.setInventoryReadingId(
                reading.getInventoryReadingId()
        );

        response.setPenPresent(
                reading.getPenPresent()
        );

        response.setCartridgePresent(
                reading.getCartridgePresent()
        );

        response.setWeightG(
                reading.getWeightG()
        );

        response.setEstimatedUnitsRemaining(
                reading.getEstimatedUnitsRemaining()
        );

        response.setEstimatedRemainingPercent(
                reading.getEstimatedRemainingPercent()
        );

        response.setInventoryStatus(
                reading.getInventoryStatus()
        );

        response.setNotes(
                reading.getNotes()
        );

        response.setMeasuredAt(
                reading.getMeasuredAt()
        );

        response.setCreatedAt(
                reading.getCreatedAt()
        );

        return response;
    }
}