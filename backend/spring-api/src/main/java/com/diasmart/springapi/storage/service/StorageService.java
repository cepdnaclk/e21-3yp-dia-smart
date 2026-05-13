package com.diasmart.springapi.storage.service;

import com.diasmart.springapi.storage.dto.StorageReadingResponseDto;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// Storage business operations
public interface StorageService {

    // Fetch patient storage history
    Page<StorageReadingResponseDto>
    getPatientStorageReadings(

            Long patientId,

            Pageable pageable
    );
}