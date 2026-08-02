package com.diasmart.springapi.deviceconfig.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "diasmart.device-provisioning")
public class DeviceProvisioningProperties {

    private long commandTimeoutMinutes = 5;
    private long timeoutScanFixedDelayMs = 60000;
    private int timeoutBatchSize = 50;

    public long getCommandTimeoutMinutes() {
        return Math.max(1, commandTimeoutMinutes);
    }

    public void setCommandTimeoutMinutes(long commandTimeoutMinutes) {
        this.commandTimeoutMinutes = commandTimeoutMinutes;
    }

    public long getTimeoutScanFixedDelayMs() {
        return Math.max(1000, timeoutScanFixedDelayMs);
    }

    public void setTimeoutScanFixedDelayMs(long timeoutScanFixedDelayMs) {
        this.timeoutScanFixedDelayMs = timeoutScanFixedDelayMs;
    }

    public int getTimeoutBatchSize() {
        return Math.max(1, timeoutBatchSize);
    }

    public void setTimeoutBatchSize(int timeoutBatchSize) {
        this.timeoutBatchSize = timeoutBatchSize;
    }
}
