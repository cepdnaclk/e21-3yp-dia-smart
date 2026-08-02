package com.diasmart.springapi.devices.entity;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "device_kits")
public class DeviceKit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_kit_id")
    private Long deviceKitId;

    @Column(name = "kit_uid", unique = true, nullable = false)
    private String kitUid;

    @Column(name = "buyer_id", nullable = false)
    private Long buyerId;

    @Column(name = "patient_id")
    private Long patientId;

    @Column(name = "purchase_date", nullable = false)
    private LocalDate purchaseDate;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "activated_at")
    private OffsetDateTime activatedAt;

    @PrePersist
    public void prePersist() {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (status == null) {
            status = "ACTIVE";
        }

        if (purchaseDate == null) {
            purchaseDate = LocalDate.now(ZoneOffset.UTC);
        }

        if (createdAt == null) {
            createdAt = now;
        }

        updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

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

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }
}
