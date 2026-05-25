package com.diasmart.springapi.analytics.dto;

import java.time.LocalTime;
import java.time.OffsetDateTime;

public class AdherenceEntry {

    private Long scheduleId;
    private String scheduleLabel;
    private LocalTime scheduledTime;
    private String status; // ON_TIME | LATE | MISSED | UNSCHEDULED
    private Long doseEventId;
    private OffsetDateTime injectedAt;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Long getDoseEventId() {
        return doseEventId;
    }

    public void setDoseEventId(Long doseEventId) {
        this.doseEventId = doseEventId;
    }

    public OffsetDateTime getInjectedAt() {
        return injectedAt;
    }

    public void setInjectedAt(OffsetDateTime injectedAt) {
        this.injectedAt = injectedAt;
    }
}
