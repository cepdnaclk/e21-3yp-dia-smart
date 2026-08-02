package com.diasmart.springapi.deviceconfig.service.impl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WifiProvisioningTimeoutScheduler {

    private final WifiCommandStateService stateService;

    public WifiProvisioningTimeoutScheduler(WifiCommandStateService stateService) {
        this.stateService = stateService;
    }

    @Scheduled(fixedDelayString = "${diasmart.device-provisioning.timeout-scan-fixed-delay-ms:60000}")
    public void markTimedOutProvisioningCommands() {
        for (Long commandId : stateService.findTimedOutProvisioningCommandIds()) {
            try {
                stateService.markProvisioningTimedOut(commandId);
            } catch (RuntimeException ignored) {
                // Timeout processing is retried by the next scheduler pass.
            }
        }
    }
}
