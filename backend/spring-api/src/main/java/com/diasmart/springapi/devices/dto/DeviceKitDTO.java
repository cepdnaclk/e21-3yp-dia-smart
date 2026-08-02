package com.diasmart.springapi.devices.dto;

import java.util.List;

public class DeviceKitDTO {

    private Long deviceKitId;
    private String kitUid;
    private Long buyerId;
    private java.time.LocalDate purchaseDate;
    private String status;
    private java.time.OffsetDateTime createdAt;
    private java.time.OffsetDateTime updatedAt;
    private List<DeviceSummaryDTO> devices;

    public Long getDeviceKitId() {
        return deviceKitId;
    }

    public void setDeviceKitId(Long deviceKitId) {
        this.deviceKitId = deviceKitId;
    }

    public String getKitUid() {
        return kitUid;
    }

    public void setKitUid(String kitUid) {
        this.kitUid = kitUid;
    }

    public Long getBuyerId() {
        return buyerId;
    }

    public void setBuyerId(Long buyerId) {
        this.buyerId = buyerId;
    }

    public java.time.LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(java.time.LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public java.time.OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<DeviceSummaryDTO> getDevices() {
        return devices;
    }

    public void setDevices(List<DeviceSummaryDTO> devices) {
        this.devices = devices;
    }
}
