package com.diasmart.springapi.glucose.dto;

import java.time.OffsetDateTime;

public class GlucoseReadingResponse {

    private Long glucoseReadingId;

    private Double glucoseValueMgDl;

    private OffsetDateTime measuredAt;

    private String source;

    private String mealContext;

    private String notes;

    private OffsetDateTime createdAt;

    public Long getGlucoseReadingId() {
        return glucoseReadingId;
    }

    public void setGlucoseReadingId(Long glucoseReadingId) {
        this.glucoseReadingId = glucoseReadingId;
    }

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

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
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

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}