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
            source = WifiCredentialSource::NVS_CURRENT;
            return true;
        }

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
            source = WifiCredentialSource::NONE;
            return false;
        }

        source = WifiCredentialSource::DEVELOPMENT_FALLBACK;
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
