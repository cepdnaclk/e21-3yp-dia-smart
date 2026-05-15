package com.diasmart.springapi.dashboard.dto;

import com.diasmart.springapi.alerts.dto.AlertResponse;
import com.diasmart.springapi.dose.dto.DoseEventResponse;
import com.diasmart.springapi.glucose.dto.GlucoseReadingResponse;
import com.diasmart.springapi.inventory.dto.InventoryReadingResponse;
import com.diasmart.springapi.storage.dto.StorageReadingResponse;

import java.util.List;

public class DashboardSummaryResponse {

    private GlucoseReadingResponse latestGlucoseReading;

    private DoseEventResponse latestDoseEvent;

    private StorageReadingResponse latestStorageReading;

    private InventoryReadingResponse latestInventoryReading;

    private List<AlertResponse> activeAlerts;

    public GlucoseReadingResponse getLatestGlucoseReading() {
        return latestGlucoseReading;
    }

    public void setLatestGlucoseReading(
            GlucoseReadingResponse latestGlucoseReading
    ) {
        this.latestGlucoseReading =
                latestGlucoseReading;
    }

    public DoseEventResponse getLatestDoseEvent() {
        return latestDoseEvent;
    }

    public void setLatestDoseEvent(
            DoseEventResponse latestDoseEvent
    ) {
        this.latestDoseEvent = latestDoseEvent;
    }

    public StorageReadingResponse getLatestStorageReading() {
        return latestStorageReading;
    }

    public void setLatestStorageReading(
            StorageReadingResponse latestStorageReading
    ) {
        this.latestStorageReading =
                latestStorageReading;
    }

    public InventoryReadingResponse getLatestInventoryReading() {
        return latestInventoryReading;
    }

    public void setLatestInventoryReading(
            InventoryReadingResponse latestInventoryReading
    ) {
        this.latestInventoryReading =
                latestInventoryReading;
    }

    public List<AlertResponse> getActiveAlerts() {
        return activeAlerts;
    }

    public void setActiveAlerts(
            List<AlertResponse> activeAlerts
    ) {
        this.activeAlerts = activeAlerts;
    }
}