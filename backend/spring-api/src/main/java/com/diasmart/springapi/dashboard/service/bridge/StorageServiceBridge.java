package com.diasmart.springapi.dashboard.service.bridge;

import com.diasmart.springapi.storage.dto.StorageReadingResponse;
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;

import org.springframework.stereotype.Service;

@Service
public class StorageServiceBridge {

    private final StorageReadingRepository storageReadingRepository;

    public StorageServiceBridge(
            StorageReadingRepository storageReadingRepository
    ) {
        this.storageReadingRepository =
                storageReadingRepository;
    }

    public StorageReadingResponse
    getLatestStorage(
            Long patientId
    ) {

        StorageReading reading =
                storageReadingRepository
                        .findTopByPatientIdOrderByMeasuredAtDesc(
                                patientId
                        );

        if (reading == null) {
            return null;
        }

        StorageReadingResponse response =
                new StorageReadingResponse();

        response.setStorageReadingId(
                reading.getStorageReadingId()
        );

        response.setTemperatureC(
                reading.getTemperatureC()
        );

        response.setHumidityPercent(
                reading.getHumidityPercent()
        );

        response.setDoorState(
                reading.getDoorState()
        );

        response.setTemperatureStatus(
                reading.getTemperatureStatus()
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