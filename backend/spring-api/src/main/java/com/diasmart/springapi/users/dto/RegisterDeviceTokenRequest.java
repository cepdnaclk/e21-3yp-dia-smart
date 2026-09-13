package com.diasmart.springapi.users.dto;

import jakarta.validation.constraints.NotBlank;

public class RegisterDeviceTokenRequest {

    @NotBlank(message = "Device token is required")
    private String deviceToken;

    private String platform = "android";

    public RegisterDeviceTokenRequest() {
    }

    public RegisterDeviceTokenRequest(String deviceToken, String platform) {
        this.deviceToken = deviceToken;
        this.platform = platform;
    }

    public String getDeviceToken() {
        return deviceToken;
    }

    public void setDeviceToken(String deviceToken) {
        this.deviceToken = deviceToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}
