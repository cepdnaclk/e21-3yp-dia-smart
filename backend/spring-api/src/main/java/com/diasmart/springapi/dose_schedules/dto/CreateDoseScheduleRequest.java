package com.diasmart.springapi.dose_schedules.dto;

import java.math.BigDecimal;

public class CreateDoseScheduleRequest {

    private Long prescriptionId;

    private String scheduleLabel;

    private String scheduledTime;

    private BigDecimal doseUnits;

    private String daysOfWeek;

    private Integer allowedEarlyMinutes;

    private Integer allowedLateMinutes;

    public Long getPrescriptionId() {
        return prescriptionId;
    }

    public void setPrescriptionId(Long prescriptionId) {
        this.prescriptionId = prescriptionId;
    }

    public String getScheduleLabel() {
        return scheduleLabel;
    }

    public void setScheduleLabel(String scheduleLabel) {
        this.scheduleLabel = scheduleLabel;
    }

    public String getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(String scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public BigDecimal getDoseUnits() {
        return doseUnits;
    }

    public void setDoseUnits(BigDecimal doseUnits) {
        this.doseUnits = doseUnits;
    }

    public String getDaysOfWeek() {
        return daysOfWeek;
    }

    public void setDaysOfWeek(String daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    public Integer getAllowedEarlyMinutes() {
        return allowedEarlyMinutes;
    }

    public void setAllowedEarlyMinutes(Integer allowedEarlyMinutes) {
        this.allowedEarlyMinutes = allowedEarlyMinutes;
    }

    public Integer getAllowedLateMinutes() {
        return allowedLateMinutes;
    }

    public void setAllowedLateMinutes(Integer allowedLateMinutes) {
        this.allowedLateMinutes = allowedLateMinutes;
    }
}