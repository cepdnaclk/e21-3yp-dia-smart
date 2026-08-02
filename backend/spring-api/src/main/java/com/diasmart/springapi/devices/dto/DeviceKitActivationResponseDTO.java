package com.diasmart.springapi.devices.dto;

import java.time.OffsetDateTime;

public class DeviceKitActivationResponseDTO {

    private Long patientId;
    private Long kitId;
    private String kitUid;
    private String activationStatus;
    private ActivationDevicesDTO devices;
    private OffsetDateTime activatedAt;

    public Long getPatientId() {
        return patientId;
    }

    public void setPatientId(Long patientId) {
        this.patientId = patientId;
    }

    public Long getKitId() {
        return kitId;
    }

    public void setKitId(Long kitId) {
        this.kitId = kitId;
    }

    public String getKitUid() {
        return kitUid;
    }

    public void setKitUid(String kitUid) {
        this.kitUid = kitUid;
    }

    public String getActivationStatus() {
        return activationStatus;
    }

    public void setActivationStatus(String activationStatus) {
        this.activationStatus = activationStatus;
    }

    public ActivationDevicesDTO getDevices() {
        return devices;
    }

    public void setDevices(ActivationDevicesDTO devices) {
        this.devices = devices;
    }

    public OffsetDateTime getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(OffsetDateTime activatedAt) {
        this.activatedAt = activatedAt;
    }

    public static class ActivationDevicesDTO {

        private Long outerDeviceId;
        private String outerDeviceUid;
        private Long innerDeviceId;
        private String innerDeviceUid;
        private Long penDeviceId;
        private String penDeviceUid;
        private Long glucometerDeviceId;
        private String glucometerDeviceUid;

        public Long getOuterDeviceId() {
            return outerDeviceId;
        }

        public void setOuterDeviceId(Long outerDeviceId) {
            this.outerDeviceId = outerDeviceId;
        }

        public String getOuterDeviceUid() {
            return outerDeviceUid;
        }

        public void setOuterDeviceUid(String outerDeviceUid) {
            this.outerDeviceUid = outerDeviceUid;
        }

        public Long getInnerDeviceId() {
            return innerDeviceId;
        }

        public void setInnerDeviceId(Long innerDeviceId) {
            this.innerDeviceId = innerDeviceId;
        }

        public String getInnerDeviceUid() {
            return innerDeviceUid;
        }

        public void setInnerDeviceUid(String innerDeviceUid) {
            this.innerDeviceUid = innerDeviceUid;
        }

        public Long getPenDeviceId() {
            return penDeviceId;
        }

        public void setPenDeviceId(Long penDeviceId) {
            this.penDeviceId = penDeviceId;
        }

        public String getPenDeviceUid() {
            return penDeviceUid;
        }

        public void setPenDeviceUid(String penDeviceUid) {
            this.penDeviceUid = penDeviceUid;
        }

        public Long getGlucometerDeviceId() {
            return glucometerDeviceId;
        }

        public void setGlucometerDeviceId(Long glucometerDeviceId) {
            this.glucometerDeviceId = glucometerDeviceId;
        }

        public String getGlucometerDeviceUid() {
            return glucometerDeviceUid;
        }

        public void setGlucometerDeviceUid(String glucometerDeviceUid) {
            this.glucometerDeviceUid = glucometerDeviceUid;
        }
    }
}
