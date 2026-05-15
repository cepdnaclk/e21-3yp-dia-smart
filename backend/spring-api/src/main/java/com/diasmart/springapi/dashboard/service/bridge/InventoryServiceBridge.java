package com.diasmart.springapi.dashboard.service.bridge;

import com.diasmart.springapi.inventory.dto.InventoryReadingResponse;
import com.diasmart.springapi.inventory.entity.InventoryReading;
import com.diasmart.springapi.inventory.repository.InventoryReadingRepository;

import org.springframework.stereotype.Service;

@Service
public class InventoryServiceBridge {

    private final InventoryReadingRepository inventoryReadingRepository;

    public InventoryServiceBridge(
            InventoryReadingRepository inventoryReadingRepository
    ) {
        this.inventoryReadingRepository =
                inventoryReadingRepository;
    }

    public InventoryReadingResponse
    getLatestInventory(
            Long patientId
    ) {

        InventoryReading reading =
                inventoryReadingRepository
                        .findTopByPatientIdOrderByMeasuredAtDesc(
                                patientId
                        );

        if (reading == null) {
            return null;
        }

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

        response.setMeasuredAt(
                reading.getMeasuredAt()
        );

        response.setCreatedAt(
                reading.getCreatedAt()
        );

        return response;
    }
}