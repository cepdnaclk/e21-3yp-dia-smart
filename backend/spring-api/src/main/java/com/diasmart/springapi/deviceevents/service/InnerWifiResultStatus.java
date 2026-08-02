package com.diasmart.springapi.deviceevents.service;

import java.util.Locale;

public enum InnerWifiResultStatus {
    STAGED(false),
    CONNECTING(false),
    CONNECTED(true),
    FAILED(true),
    ROLLED_BACK(true),
    RECOVERY_CHANNEL(false),
    WAITING_FOR_CONFIGURATION(false);

    private final boolean terminal;

    InnerWifiResultStatus(boolean terminal) {
        this.terminal = terminal;
    }

    public static InnerWifiResultStatus fromFirmware(String status) {
        if (status == null || status.isBlank()) {
            return FAILED;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "STAGED", "CREDENTIALS_STAGED" -> STAGED;
            case "CONNECTING", "CONNECTING_TO_ROUTER" -> CONNECTING;
            case "CONNECTED", "APPLIED" -> CONNECTED;
            case "ROLLED_BACK", "ROLLBACK_COMPLETE" -> ROLLED_BACK;
            case "RECOVERY_CHANNEL", "RECOVERY_CHANNEL_ACTIVE", "RECOVERY_MODE" -> RECOVERY_CHANNEL;
            case "WAITING_FOR_CONFIGURATION", "WAITING" -> WAITING_FOR_CONFIGURATION;
            default -> FAILED;
        };
    }

    public boolean terminal() {
        return terminal;
    }

    public boolean successful() {
        return this == CONNECTED;
    }

    public boolean failed() {
        return this == FAILED || this == ROLLED_BACK || this == RECOVERY_CHANNEL;
    }
}
