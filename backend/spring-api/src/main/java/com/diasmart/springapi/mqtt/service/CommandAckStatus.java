package com.diasmart.springapi.mqtt.service;

import java.util.Locale;
import java.util.Set;

public enum CommandAckStatus {
    PENDING(0, false),
    PUBLISHED(1, false),
    RECEIVED(2, false),
    VALIDATED(3, false),
    STAGED(4, false),
    APPLYING(5, false),
    APPLIED(6, true),
    FAILED(100, true),
    REJECTED(100, true),
    ROLLED_BACK(100, true);

    private static final Set<String> TERMINAL_COMMAND_STATUSES = Set.of(
            "APPLIED",
            "FAILED",
            "REJECTED",
            "ROLLED_BACK",
            "EXPIRED"
    );

    private final int rank;
    private final boolean terminal;

    CommandAckStatus(int rank, boolean terminal) {
        this.rank = rank;
        this.terminal = terminal;
    }

    public static CommandAckStatus fromFirmware(String status) {
        if (status == null || status.isBlank()) {
            return FAILED;
        }

        String normalized = status.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        return switch (normalized) {
            case "PENDING" -> PENDING;
            case "PUBLISHED", "SENT" -> PUBLISHED;
            case "RECEIVED" -> RECEIVED;
            case "VALIDATED" -> VALIDATED;
            case "STAGED", "CREDENTIALS_STAGED" -> STAGED;
            case "APPLYING" -> APPLYING;
            case "APPLIED", "ACCEPTED" -> APPLIED;
            case "REJECTED" -> REJECTED;
            case "ROLLED_BACK" -> ROLLED_BACK;
            default -> FAILED;
        };
    }

    public String commandStatus() {
        return "REJECTED".equals(name()) ? "FAILED" : name();
    }

    public boolean canTransitionFrom(String currentStatus) {
        if (currentStatus == null || currentStatus.isBlank()) {
            return true;
        }

        String current = currentStatus.trim().toUpperCase(Locale.ROOT);
        if ("EXPIRED".equals(current)) {
            return false;
        }

        if (TERMINAL_COMMAND_STATUSES.contains(current)) {
            return current.equals(commandStatus());
        }

        CommandAckStatus currentAckStatus;
        try {
            currentAckStatus = fromFirmware(current);
        } catch (RuntimeException ex) {
            return true;
        }

        if (terminal) {
            return true;
        }

        return rank >= currentAckStatus.rank;
    }

    public boolean terminal() {
        return terminal;
    }
}
