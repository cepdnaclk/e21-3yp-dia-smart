package com.diasmart.springapi.glucose.repository;

// Import the GlucoseReading entity class
import com.diasmart.springapi.glucose.entity.GlucoseReading;

// Provides built-in CRUD database operations
import org.springframework.data.jpa.repository.JpaRepository;

// Used for pagination support
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;


// Repository interface for glucose_readings table
public interface GlucoseReadingRepository
        extends JpaRepository<GlucoseReading, Long> {

        // Fetch glucose readings of a patient ordered by latest first
        // Pageable automatically supports page, size, sorting, etc.
        Page<GlucoseReading> findByPatientIdOrderByMeasuredAtDesc(

                // Patient whose glucose history is requested
                Long patientId,

                // Pagination information
                Pageable pageable
        );

}