package com.diasmart.springapi.deviceconfig.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class CreateDeviceConfigurationRequestDTO {

    @NotNull(message = "Outer device ID is required")
    private Long outerDeviceId;

    @NotBlank(message = "WiFi SSID is required")
    @Size(max = 100, message = "WiFi SSID cannot exceed 100 characters")
    private String wifiSsid;

    @NotBlank(message = "WiFi password is required")
    @Size(min = 8, max = 63, message = "WiFi password must be between 8 and 63 characters")
    private String wifiPassword;

    private Long innerDeviceId;
    private Long penDeviceId;
    private Long glucometerDeviceId;

    public CreateDeviceConfigurationRequestDTO() {
    }

    public Long getOuterDeviceId() {
        return outerDeviceId;
    }

    public void setOuterDeviceId(Long outerDeviceId) {
        this.outerDeviceId = outerDeviceId;
    }

    public String getWifiSsid() {
        return wifiSsid;
    }

    public void setWifiSsid(String wifiSsid) {
        this.wifiSsid = wifiSsid;
    }

    public String getWifiPassword() {
        return wifiPassword;
    }

    public void setWifiPassword(String wifiPassword) {
        this.wifiPassword = wifiPassword;
    }

    public Long getInnerDeviceId() {
        return innerDeviceId;
    }

    public void setInnerDeviceId(Long innerDeviceId) {
        this.innerDeviceId = innerDeviceId;
    }

    public Long getPenDeviceId() {
        return penDeviceId;
    }

    public void setPenDeviceId(Long penDeviceId) {
        this.penDeviceId = penDeviceId;
    }

    public Long getGlucometerDeviceId() {
        return glucometerDeviceId;
    }

    public void setGlucometerDeviceId(Long glucometerDeviceId) {
        this.glucometerDeviceId = glucometerDeviceId;
    }
}
