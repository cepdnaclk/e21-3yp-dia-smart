#pragma once

#include <Arduino.h>
#include <stdint.h>

class OfflineJsonQueueService {
public:
    bool begin();
    bool enqueue(const String& payload);
    bool peek(String& payload);
    bool pop();
    uint16_t count() const;
    bool ready() const;

private:
    bool loadMeta();
    bool saveMeta();
    String pathFor(uint32_t id) const;
    bool dropOldest();

    bool ready_ = false;
    uint32_t headId_ = 0;
    uint32_t nextId_ = 0;
    uint16_t count_ = 0;
};

extern OfflineJsonQueueService offlineJsonQueue;
