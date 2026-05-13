package com.diasmart.springapi.glucose.service.impl;

// DTO used to send safe glucose data to frontend
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponseDto;

// Entity representing glucose_readings table
import com.diasmart.springapi.glucose.entity.GlucoseReading;

// Repository used to access glucose data from database
import com.diasmart.springapi.glucose.repository.GlucoseReadingRepository;

// Service interface implemented by this class
import com.diasmart.springapi.glucose.service.GlucoseService;

// Auto-generates constructor for final fields
import lombok.RequiredArgsConstructor;

// Used for paginated responses
import org.springframework.data.domain.Page;

// Used for pagination request details
import org.springframework.data.domain.Pageable;

// Marks this class as a Spring service component
import org.springframework.stereotype.Service;


// Registers this class as a service bean in Spring
@Service

// Automatically creates constructor for dependency injection(Automatically generates constructor for: private final GlucoseReadingRepository)
@RequiredArgsConstructor

// Service implementation containing glucose business logic
public class GlucoseServiceImpl implements GlucoseService {

    // Repository used to fetch glucose data from PostgreSQL
    private final GlucoseReadingRepository glucoseReadingRepository;


    // Implementation of service method defined in interface
    @Override
    public Page<GlucoseReadingResponseDto> getPatientGlucoseReadings(

            // Patient whose glucose history is requested
            Long patientId,

            // Pagination details
            Pageable pageable
    ) {

        // Fetch glucose readings from DB and convert entities to DTOs
        return glucoseReadingRepository
                .findByPatientIdOrderByMeasuredAtDesc(patientId, pageable)
                .map(this::mapToDto);
    }


    // Converts GlucoseReading entity into response DTO
    private GlucoseReadingResponseDto mapToDto(

            // Entity object fetched from database
            GlucoseReading reading
    ) {

        // Build and return DTO object
        return GlucoseReadingResponseDto.builder()

                // Copy glucose reading ID
                .glucoseReadingId(reading.getGlucoseReadingId())

                // Copy patient ID
                .patientId(reading.getPatientId())

                // Copy glucose value
                .glucoseValueMgDl(reading.getGlucoseValueMgDl())

                // Copy measurement timestamp
                .measuredAt(reading.getMeasuredAt())

                // Finalize DTO object creation
                .build();
    }
}