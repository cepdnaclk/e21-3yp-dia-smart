package com.diasmart.springapi.glucose.repository;

import com.diasmart.springapi.glucose.entity.GlucoseReading;

import org.springframework.data.jpa.repository.JpaRepository;

public interface GlucoseReadingRepository
        extends JpaRepository<GlucoseReading, Long> {
}