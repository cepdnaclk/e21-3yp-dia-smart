package com.diasmart.springapi.storage.service;

import com.diasmart.springapi.shared.enums.Permission;
import com.diasmart.springapi.shared.security.AuthorizationService;
import com.diasmart.springapi.storage.dto.StorageReadingResponse;
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class StorageReadingService {

    private final StorageReadingRepository storageReadingRepository;

    private final AuthorizationService authorizationService;

    public StorageReadingService(
            StorageReadingRepository storageReadingRepository,
            AuthorizationService authorizationService
    ) {
        this.storageReadingRepository = storageReadingRepository;
        this.authorizationService = authorizationService;
    }

    public Page<StorageReadingResponse> getStorageHistory(
            Long patientId,
            Pageable pageable
    ) {

        authorizationService.authorize(
                Permission.READ_STORAGE_HISTORY,
                patientId
        );

        return storageReadingRepository
                .findByPatientId(patientId, pageable)
                .map(this::mapToResponse);
    }

    public StorageReadingResponse getLatestStorageReading(
            Long patientId
    ) {

        authorizationService.authorize(
                Permission.READ_STORAGE_HISTORY,
                patientId
        );

        StorageReading reading =
                storageReadingRepository
                        .findTopByPatientIdOrderByMeasuredAtDesc(
                                patientId
                        );

        if (reading == null) {
            return null;
        }

        return mapToResponse(reading);
    }

    private StorageReadingResponse mapToResponse(
            StorageReading reading
    ) {

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

        response.setDoorOpenDurationSeconds(
                reading.getDoorOpenDurationSeconds()
        );

        response.setTemperatureStatus(
                reading.getTemperatureStatus()
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