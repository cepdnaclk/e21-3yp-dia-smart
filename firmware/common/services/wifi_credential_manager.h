#pragma once

#include "wifi_credential_store.h"

class WifiCredentialManager {
public:
    WifiCredentialManager(
        const char* nvsNamespace,
        const char* developmentSsid,
        const char* developmentPassword
    )
        : store_(nvsNamespace),
          developmentSsid_(developmentSsid),
          developmentPassword_(developmentPassword) {
    }

    bool loadActive(
        WifiConfiguration& configuration,
        WifiCredentialSource& source
    ) const {
        if (store_.loadCurrent(configuration)) {
            const bool legacyDevelopmentFallback =
                configuration.configurationVersion == 0 &&
                strcmp(configuration.commandId, "DEV-FALLBACK") == 0;
            if (!legacyDevelopmentFallback) {
                source = WifiCredentialSource::NVS_CURRENT;
                return true;
            }
            clearWifiConfiguration(configuration);
        }

        if (!loadDevelopmentFallback(configuration)) {
            source = WifiCredentialSource::NONE;
            return false;
        }
        source = WifiCredentialSource::DEVELOPMENT_FALLBACK;
        return true;
    }

    bool loadDevelopmentFallback(
        WifiConfiguration& configuration
    ) const {
        const bool openNetwork =
            developmentPassword_ == nullptr ||
            developmentPassword_[0] == '\0';
        const WifiValidationResult result =
            initializeWifiConfiguration(
                configuration,
                developmentSsid_,
                developmentPassword_ == nullptr
                    ? ""
                    : developmentPassword_,
                openNetwork,
                0,
                "DEV-FALLBACK");
        if (result != WifiValidationResult::VALID) {
            return false;
        }
        return true;
    }

    WifiCredentialStore& store() {
        return store_;
    }

private:
    mutable WifiCredentialStore store_;
    const char* developmentSsid_;
    const char* developmentPassword_;
};
