package com.diasmart.springapi.devices.dto;

import java.util.List;

public class DeviceKitDTO {
    
    private java.time.LocalDate purchaseDate;
    private List<DeviceSummaryDTO> devices;

    public java.time.LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(java.time.LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public List<DeviceSummaryDTO> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceSummaryDTO> devices) {
        this.devices = devices;
    }
}
