package com.diasmart.springapi.deviceconfig.service.impl;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "diasmart.mqtt.wifi-command")
public class WifiCommandPublishProperties {

    private int maxRetries = 3;
    private long retryDelaySeconds = 5;
    private long sentStaleAfterSeconds = 60;
    private int recoveryBatchSize = 25;

    public int getMaxRetries() {
        return Math.max(1, maxRetries);
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public long getRetryDelaySeconds() {
        return Math.max(0, retryDelaySeconds);
    }

    public void setRetryDelaySeconds(long retryDelaySeconds) {
        this.retryDelaySeconds = retryDelaySeconds;
    }

    public long getSentStaleAfterSeconds() {
        return Math.max(1, sentStaleAfterSeconds);
    }

    public void setSentStaleAfterSeconds(long sentStaleAfterSeconds) {
        this.sentStaleAfterSeconds = sentStaleAfterSeconds;
    }

    public int getRecoveryBatchSize() {
        return Math.max(1, recoveryBatchSize);
    }

    public void setRecoveryBatchSize(int recoveryBatchSize) {
        this.recoveryBatchSize = recoveryBatchSize;
    }
}
