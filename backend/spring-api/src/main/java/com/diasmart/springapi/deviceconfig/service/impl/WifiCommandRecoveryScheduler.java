package com.diasmart.springapi.deviceconfig.service.impl;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class WifiCommandRecoveryScheduler {

    private final WifiCommandStateService stateService;
    private final WifiConfigurationCommandPublisher publisher;

    public WifiCommandRecoveryScheduler(
            WifiCommandStateService stateService,
            WifiConfigurationCommandPublisher publisher) {
        this.stateService = stateService;
        this.publisher = publisher;
    }

    @Scheduled(fixedDelayString = "${diasmart.mqtt.wifi-command.recovery-fixed-delay-ms:30000}")
    public void recoverPendingWifiCommands() {
        for (Long commandId : stateService.findRecoverableWifiCommandIds()) {
            try {
                publisher.publishWifiCommand(commandId);
            } catch (RuntimeException ignored) {
                // The publisher stores a safe failure code and retry metadata.
            }
        }
    }
}
