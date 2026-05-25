package com.diasmart.springapi.analytics.dto;

import java.time.LocalDate;
import java.util.List;

public class AdherenceAnalyticsResponse {

    private Long patientId;
    private LocalDate startDate;
    private LocalDate endDate;
    private int totalScheduled;
    private int onTime;
    private int late;
    private int missed;
    private int unscheduled;
    private double adherenceRate;
    private List<DailyAdherenceBreakdown> dailyBreakdown;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public int getTotalScheduled() {
        return totalScheduled;
    }

    public void setTotalScheduled(int totalScheduled) {
        this.totalScheduled = totalScheduled;
    }

    public int getOnTime() {
        return onTime;
    }

    public void setOnTime(int onTime) {
        this.onTime = onTime;
    }

    public int getLate() {
        return late;
    }

    public void setLate(int late) {
        this.late = late;
    }

    public int getMissed() {
        return missed;
    }

    public void setMissed(int missed) {
        this.missed = missed;
    }

    public int getUnscheduled() {
        return unscheduled;
    }

    public void setUnscheduled(int unscheduled) {
        this.unscheduled = unscheduled;
    }

    public double getAdherenceRate() {
        return adherenceRate;
    }

    public void setAdherenceRate(double adherenceRate) {
        this.adherenceRate = adherenceRate;
    }

    public List<DailyAdherenceBreakdown> getDailyBreakdown() {
        return dailyBreakdown;
    }

    public void setDailyBreakdown(List<DailyAdherenceBreakdown> dailyBreakdown) {
        this.dailyBreakdown = dailyBreakdown;
    }
}
