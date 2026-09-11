package com.diasmart.springapi.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import java.time.Duration;

@ConfigurationProperties(prefix = "diasmart.ai")
public class AiProperties {
    private boolean enabled = false;
    private String gatewayUrl = "http://127.0.0.1:8000";
    private String internalServiceToken;
    private Duration connectTimeout = Duration.ofSeconds(3);
    private Duration readTimeout = Duration.ofSeconds(30);
    private int maxDateRangeDays = 31;
    private int maxAlerts = 100;
    private int maxSelectedEvents = 100;
    private String promptVersion = "clinical-summary-v1";
    private String expectedProvider = "mock";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getGatewayUrl() {
        return gatewayUrl;
    }

    public void setGatewayUrl(String gatewayUrl) {
        this.gatewayUrl = gatewayUrl;
    }

    public String getInternalServiceToken() {
        return internalServiceToken;
    }

    public void setInternalServiceToken(String internalServiceToken) {
        this.internalServiceToken = internalServiceToken;
    }

    public Duration getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public Duration getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getMaxDateRangeDays() {
        return maxDateRangeDays;
    }

    public void setMaxDateRangeDays(int maxDateRangeDays) {
        this.maxDateRangeDays = maxDateRangeDays;
    }

    public int getMaxAlerts() {
        return maxAlerts;
    }

    public void setMaxAlerts(int maxAlerts) {
        this.maxAlerts = maxAlerts;
    }

    public int getMaxSelectedEvents() {
        return maxSelectedEvents;
    }

    public void setMaxSelectedEvents(int maxSelectedEvents) {
        this.maxSelectedEvents = maxSelectedEvents;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getExpectedProvider() {
        return expectedProvider;
    }

    public void setExpectedProvider(String expectedProvider) {
        this.expectedProvider = expectedProvider;
    }
}
