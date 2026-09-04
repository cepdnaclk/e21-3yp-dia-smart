#pragma once

#include <Arduino.h>
#include <Preferences.h>

#include "../models/wifi_configuration.h"

class WifiCredentialStore {
public:
    explicit WifiCredentialStore(const char* nvsNamespace)
        : nvsNamespace_(nvsNamespace) {
    }

    bool loadCurrent(WifiConfiguration& configuration) const {
        return loadSlot("c", configuration);
    }

    bool loadPending(WifiConfiguration& configuration) const {
        return loadSlot("p", configuration);
    }

    bool loadPrevious(WifiConfiguration& configuration) const {
        return loadSlot("v", configuration);
    }

    bool saveCurrent(const WifiConfiguration& configuration) {
        return saveSlot("c", configuration);
    }

    bool stagePending(const WifiConfiguration& configuration) {
        if (!saveSlot("p", configuration)) {
            return false;
        }
        return setTransactionState(WifiTransactionState::STAGED);
    }

    bool promotePending() {
        WifiConfiguration pending = {};
        if (!loadPending(pending)) {
            return false;
        }

        if (!setTransactionState(WifiTransactionState::APPLYING)) {
            clearWifiConfiguration(pending);
            return false;
        }

        WifiConfiguration current = {};
        if (loadCurrent(current) && !saveSlot("v", current)) {
            clearWifiConfiguration(current);
            clearWifiConfiguration(pending);
            return false;
        }
        clearWifiConfiguration(current);

        if (!saveSlot("c", pending)) {
            clearWifiConfiguration(pending);
            return false;
        }
        clearWifiConfiguration(pending);

        if (!clearSlot("p")) {
            return false;
        }
        return setTransactionState(WifiTransactionState::APPLIED);
    }

    bool rollbackToPrevious() {
        WifiConfiguration previous = {};
        if (!loadPrevious(previous)) {
            return false;
        }

        if (!setTransactionState(WifiTransactionState::ROLLING_BACK)) {
            clearWifiConfiguration(previous);
            return false;
        }
        if (!saveSlot("c", previous)) {
            clearWifiConfiguration(previous);
            return false;
        }
        clearWifiConfiguration(previous);

        if (!clearSlot("p")) {
            return false;
        }
        return setTransactionState(WifiTransactionState::ROLLED_BACK);
    }

    bool clearPending() {
        if (!clearSlot("p")) {
            return false;
        }
        return setTransactionState(WifiTransactionState::IDLE);
    }

    WifiTransactionState transactionState() const {
        Preferences preferences;
        // Open read/write so a clean device creates the namespace once instead
        // of logging NVS_NOT_FOUND on every coordinator poll.
        if (!preferences.begin(nvsNamespace_, false)) {
            return WifiTransactionState::IDLE;
        }
        const uint8_t stored = preferences.getUChar(
            "txn_state",
            static_cast<uint8_t>(WifiTransactionState::IDLE));
        preferences.end();

        if (stored >
            static_cast<uint8_t>(WifiTransactionState::ROLLED_BACK)) {
            return WifiTransactionState::IDLE;
        }
        return static_cast<WifiTransactionState>(stored);
    }

    bool setTransactionState(WifiTransactionState state) {
        Preferences preferences;
        if (!preferences.begin(nvsNamespace_, false)) {
            return false;
        }
        const size_t written = preferences.putUChar(
            "txn_state",
            static_cast<uint8_t>(state));
        preferences.end();
        return written == sizeof(uint8_t);
    }

private:
    const char* nvsNamespace_;

    static void makeKey(
        char* output,
        size_t outputLength,
        const char* slot,
        const char* field
    ) {
        snprintf(output, outputLength, "%s_%s", slot, field);
    }

    bool loadSlot(
        const char* slot,
        WifiConfiguration& configuration
    ) const {
        clearWifiConfiguration(configuration);

        Preferences preferences;
        if (!preferences.begin(nvsNamespace_, true)) {
            return false;
        }

        char key[12];
        makeKey(key, sizeof(key), slot, "valid");
        if (!preferences.getBool(key, false)) {
            preferences.end();
            return false;
        }

        makeKey(key, sizeof(key), slot, "ssid");
        preferences.getString(
            key,
            configuration.ssid,
            sizeof(configuration.ssid));
        makeKey(key, sizeof(key), slot, "password");
        preferences.getString(
            key,
            configuration.password,
            sizeof(configuration.password));
        makeKey(key, sizeof(key), slot, "command");
        preferences.getString(
            key,
            configuration.commandId,
            sizeof(configuration.commandId));
        makeKey(key, sizeof(key), slot, "version");
        configuration.configurationVersion =
            preferences.getUInt(key, 0);
        makeKey(key, sizeof(key), slot, "checksum");
        configuration.checksum = preferences.getUInt(key, 0);
        makeKey(key, sizeof(key), slot, "open");
        configuration.openNetwork =
            preferences.getBool(key, false) ? 1 : 0;
        configuration.valid = 1;
        preferences.end();

        if (validateStoredWifiConfiguration(configuration) !=
            WifiValidationResult::VALID) {
            clearWifiConfiguration(configuration);
            return false;
        }
        return true;
    }

    bool saveSlot(
        const char* slot,
        const WifiConfiguration& configuration
    ) {
        if (validateStoredWifiConfiguration(configuration) !=
            WifiValidationResult::VALID) {
            return false;
        }

        Preferences preferences;
        if (!preferences.begin(nvsNamespace_, false)) {
            return false;
        }

        char key[12];
        makeKey(key, sizeof(key), slot, "valid");
        preferences.putBool(key, false);

        makeKey(key, sizeof(key), slot, "ssid");
        preferences.putString(key, configuration.ssid);
        makeKey(key, sizeof(key), slot, "password");
        preferences.putString(key, configuration.password);
        makeKey(key, sizeof(key), slot, "command");
        preferences.putString(key, configuration.commandId);
        makeKey(key, sizeof(key), slot, "version");
        preferences.putUInt(
            key,
            configuration.configurationVersion);
        makeKey(key, sizeof(key), slot, "checksum");
        preferences.putUInt(key, configuration.checksum);
        makeKey(key, sizeof(key), slot, "open");
        preferences.putBool(key, configuration.openNetwork != 0);
        makeKey(key, sizeof(key), slot, "valid");
        preferences.putBool(key, true);
        preferences.end();

        WifiConfiguration verified = {};
        const bool valid = loadSlot(slot, verified);
        clearWifiConfiguration(verified);
        return valid;
    }

    bool clearSlot(const char* slot) {
        Preferences preferences;
        if (!preferences.begin(nvsNamespace_, false)) {
            return false;
        }

        const char* fields[] = {
            "valid",
            "ssid",
            "password",
            "command",
            "version",
            "checksum",
            "open"
        };
        char key[12];
        for (const char* field : fields) {
            makeKey(key, sizeof(key), slot, field);
            preferences.remove(key);
        }
        preferences.end();
        return true;
    }
};
