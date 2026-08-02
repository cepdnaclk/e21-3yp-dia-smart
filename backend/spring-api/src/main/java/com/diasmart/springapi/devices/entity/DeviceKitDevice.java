package com.diasmart.springapi.devices.entity;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "device_kit_devices",
        uniqueConstraints = {
                @UniqueConstraint(name = "ux_device_kit_devices_device", columnNames = "device_id"),
                @UniqueConstraint(name = "ux_device_kit_devices_role", columnNames = {"device_kit_id", "kit_device_role"})
        }
)
public class DeviceKitDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "device_kit_device_id")
    private Long deviceKitDeviceId;

    @Column(name = "device_kit_id", nullable = false)
    private Long deviceKitId;

    @Column(name = "device_id", nullable = false)
    private Long deviceId;

    @Column(name = "kit_device_role", nullable = false)
    private String kitDeviceRole;

    @Column(name = "created_at")
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now(ZoneOffset.UTC);
        }
    }

    public Long getDeviceKitDeviceId() {
        return deviceKitDeviceId;
    }

    public void setDeviceKitDeviceId(Long deviceKitDeviceId) {
        this.deviceKitDeviceId = deviceKitDeviceId;
    }

    public Long getDeviceKitId() {
        return deviceKitId;
    }

    public void setDeviceKitId(Long deviceKitId) {
        this.deviceKitId = deviceKitId;
    }

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getKitDeviceRole() {
        return kitDeviceRole;
    }

    public void setKitDeviceRole(String kitDeviceRole) {
        this.kitDeviceRole = kitDeviceRole;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
