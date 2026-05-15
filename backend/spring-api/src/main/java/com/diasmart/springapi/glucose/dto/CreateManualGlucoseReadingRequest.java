package com.diasmart.springapi.glucose.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

public class CreateManualGlucoseReadingRequest {

    @NotNull(message = "Glucose value is required")
    @Positive(message = "Glucose value must be positive")
    @Max(value = 1000, message = "Glucose value is unrealistically high")
    private Double glucoseValueMgDl;

    @NotNull(message = "Measurement time is required")
    private OffsetDateTime measuredAt;

    @Size(max = 100)
    private String mealContext;

    @Size(max = 500)
    private String notes;

    public Double getGlucoseValueMgDl() {
        return glucoseValueMgDl;
    }

    public void setGlucoseValueMgDl(Double glucoseValueMgDl) {
        this.glucoseValueMgDl = glucoseValueMgDl;
    }

    public OffsetDateTime getMeasuredAt() {
        return measuredAt;
    }

    public void setMeasuredAt(OffsetDateTime measuredAt) {
        this.measuredAt = measuredAt;
    }

    public String getMealContext() {
        return mealContext;
    }

    public void setMealContext(String mealContext) {
        this.mealContext = mealContext;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}