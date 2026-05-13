package com.diasmart.springapi.storage.service.impl;

import com.diasmart.springapi.storage.dto.StorageReadingResponseDto;
import com.diasmart.springapi.storage.entity.StorageReading;
import com.diasmart.springapi.storage.repository.StorageReadingRepository;
import com.diasmart.springapi.storage.service.StorageService;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;


// Registers this class as Spring service
@Service

// Auto-generates constructor
@RequiredArgsConstructor
public class StorageServiceImpl
        implements StorageService {

    // Repository dependency
    private final StorageReadingRepository
            storageReadingRepository;


    // Fetch patient storage history
    @Override
    public Page<StorageReadingResponseDto>
    getPatientStorageReadings(

            Long patientId,

            Pageable pageable
    ) {

        return storageReadingRepository
                .findByPatientIdOrderByMeasuredAtDesc(
                        patientId,
                        pageable
                )
                .map(this::mapToDto);
    }


    // Convert entity into DTO
    private StorageReadingResponseDto
    mapToDto(

            StorageReading reading
    ) {

        return StorageReadingResponseDto
                .builder()

                .storageReadingId(
                        reading.getStorageReadingId()
                )

                .patientId(
                        reading.getPatientId()
                )

                .temperatureC(
                        reading.getTemperatureC()
                )

                .humidityPercent(
                        reading.getHumidityPercent()
                )

                .doorState(
                        reading.getDoorState()
                )

                .doorOpenDurationSeconds(
                        reading.getDoorOpenDurationSeconds()
                )

                .temperatureStatus(
                        reading.getTemperatureStatus()
                )

                .measuredAt(
                        reading.getMeasuredAt()
                )

                .build();
    }
}