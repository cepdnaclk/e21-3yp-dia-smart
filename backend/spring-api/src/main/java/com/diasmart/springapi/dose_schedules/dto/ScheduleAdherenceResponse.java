package com.diasmart.springapi.dose_schedules.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public class ScheduleAdherenceResponse {

    private Long scheduleId;

    private String scheduleLabel;

    private LocalTime scheduledTime;

    private BigDecimal doseUnits;

    private String status;

    private OffsetDateTime injectedAt;

    private Long minutesOffset;

    /*
     * FUTURE EXTENSIONS
     *
     * private Double adherenceScore;
     * private Boolean correctionDoseUsed;
     * private Boolean emergencyInjection;
     * private String adherenceTrend;
     * private Integer missedDoseCount;
     * private Integer lateDoseCount;
     * private Integer weeklyAdherencePercent;
     */

    public Long getScheduleId() {
        return scheduleId;
    }

    public void setScheduleId(Long scheduleId) {
        this.scheduleId = scheduleId;
    }

    public String getScheduleLabel() {
        return scheduleLabel;
    }

    public void setScheduleLabel(String scheduleLabel) {
        this.scheduleLabel = scheduleLabel;
    }

    public LocalTime getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(LocalTime scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public BigDecimal getDoseUnits() {
        return doseUnits;
    }

    public void setDoseUnits(BigDecimal doseUnits) {
        this.doseUnits = doseUnits;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getInjectedAt() {
        return injectedAt;
    }

    public void setInjectedAt(OffsetDateTime injectedAt) {
        this.injectedAt = injectedAt;
    }

    public Long getMinutesOffset() {
        return minutesOffset;
    }

    public void setMinutesOffset(Long minutesOffset) {
        this.minutesOffset = minutesOffset;
    }
}