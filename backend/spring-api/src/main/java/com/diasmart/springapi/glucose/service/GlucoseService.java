package com.diasmart.springapi.glucose.service;

// Import DTO used to send glucose data to frontend
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponseDto;

// Used for paginated responses
import org.springframework.data.domain.Page;

// Used for pagination request details like page and size
import org.springframework.data.domain.Pageable;


// Service interface for glucose-related business logic (Any glucose service must provide these methods)
public interface GlucoseService {

    // Fetch paginated glucose readings of a patient
    Page<GlucoseReadingResponseDto> getPatientGlucoseReadings(

            // ID of the patient
            Long patientId,

            // Pagination information
            Pageable pageable
    );
}