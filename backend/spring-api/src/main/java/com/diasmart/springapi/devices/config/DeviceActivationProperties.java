package com.diasmart.springapi.devices.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "diasmart.device-activation")
public class DeviceActivationProperties {

    private int maxFailuresPerUser = 5;
    private int maxFailuresPerIp = 10;
    private int windowMinutes = 15;
    private boolean trustForwardedHeaders = false;

    public int getMaxFailuresPerUser() {
        return maxFailuresPerUser;
    }

    public void setMaxFailuresPerUser(int maxFailuresPerUser) {
        this.maxFailuresPerUser = maxFailuresPerUser;
    }

    public int getMaxFailuresPerIp() {
        return maxFailuresPerIp;
    }

    public void setMaxFailuresPerIp(int maxFailuresPerIp) {
        this.maxFailuresPerIp = maxFailuresPerIp;
    }

    public int getWindowMinutes() {
        return windowMinutes;
    }

    public void setWindowMinutes(int windowMinutes) {
        this.windowMinutes = windowMinutes;
    }

    public boolean isTrustForwardedHeaders() {
        return trustForwardedHeaders;
    }

    public void setTrustForwardedHeaders(boolean trustForwardedHeaders) {
        this.trustForwardedHeaders = trustForwardedHeaders;
    }
}
