package com.diasmart.springapi.mqtt.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class GlucoseDTO {

    private String deviceUid;

    private Integer valueMgDl;

    private String source;

    private String mealContext;

    private Integer sequenceNumber;

    private String measuredAt;

    private String notes;

    // =========================
    // GETTERS
    // =========================

    public String getDeviceUid() {
        return deviceUid;
    }

    public Integer getValueMgDl() {
        return valueMgDl;
    }

    public String getSource() {
        return source;
    }

    public String getMealContext() {
        return mealContext;
    }

    public Integer getSequenceNumber() {
        return sequenceNumber;
    }

    public String getMeasuredAt() {
        return measuredAt;
    }

    public String getNotes() {
        return notes;
    }

    // =========================
    // SETTERS
    // =========================

    public void setDeviceUid(String deviceUid) {
        this.deviceUid = deviceUid;
    }

    public void setValueMgDl(
            Integer valueMgDl
    ) {
        this.valueMgDl = valueMgDl;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public void setMealContext(String mealContext) {
        this.mealContext = mealContext;
    }

    public void setSequenceNumber(Integer sequenceNumber) {
        this.sequenceNumber = sequenceNumber;
    }

    public void setMeasuredAt(String measuredAt) {
        this.measuredAt = measuredAt;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
