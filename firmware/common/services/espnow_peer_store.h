#pragma once

#include <Preferences.h>
#include <stdint.h>
#include <string.h>

class EspNowPeerStore {
public:
    explicit EspNowPeerStore(const char* nvsNamespace)
        : nvsNamespace_(nvsNamespace) {
    }

    bool load(uint8_t peerMac[6]) const {
        Preferences preferences;
        if (!preferences.begin(nvsNamespace_, true)) {
            return false;
        }
        const bool valid = preferences.getBool("valid", false);
        const size_t length =
            preferences.getBytesLength("peer_mac");
        if (!valid || length != 6) {
            preferences.end();
            return false;
        }
        const size_t read =
            preferences.getBytes("peer_mac", peerMac, 6);
        preferences.end();
        return read == 6 && !isInvalid(peerMac);
    }

    bool save(const uint8_t peerMac[6]) {
        if (peerMac == nullptr || isInvalid(peerMac)) {
            return false;
        }

        Preferences preferences;
        if (!preferences.begin(nvsNamespace_, false)) {
            return false;
        }
        preferences.putBool("valid", false);
        const size_t written =
            preferences.putBytes("peer_mac", peerMac, 6);
        if (written == 6) {
            preferences.putBool("valid", true);
        }
        preferences.end();
        return written == 6;
    }

    bool clear() {
        Preferences preferences;
        if (!preferences.begin(nvsNamespace_, false)) {
            return false;
        }
        const bool cleared = preferences.clear();
        preferences.end();
        return cleared;
    }

private:
    const char* nvsNamespace_;

    static bool isInvalid(const uint8_t peerMac[6]) {
        bool allZero = true;
        bool allBroadcast = true;
        for (size_t i = 0; i < 6; ++i) {
            allZero = allZero && peerMac[i] == 0;
            allBroadcast = allBroadcast && peerMac[i] == 0xFF;
        }
        return allZero || allBroadcast;
    }
};
