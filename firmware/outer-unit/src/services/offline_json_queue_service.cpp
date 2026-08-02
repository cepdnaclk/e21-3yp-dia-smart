#include "offline_json_queue_service.h"

#include <LittleFS.h>
#include <stdio.h>

#include "config/app_config.h"

namespace {
const char* QUEUE_DIR = "/offline";
const char* META_PATH = "/offline/meta.txt";
}

OfflineJsonQueueService offlineJsonQueue;

bool OfflineJsonQueueService::begin() {
    if (!LittleFS.begin(true)) {
        Serial.println("[OfflineQueue] LittleFS mount failed");
        ready_ = false;
        return false;
    }

    if (!LittleFS.exists(QUEUE_DIR)) {
        LittleFS.mkdir(QUEUE_DIR);
    }

    if (!loadMeta()) {
        headId_ = 0;
        nextId_ = 0;
        count_ = 0;
        if (!saveMeta()) {
            ready_ = false;
            return false;
        }
    }

    ready_ = true;
    Serial.printf("[OfflineQueue] Ready count=%u max=%u\n",
                  count_,
                  OFFLINE_JSON_QUEUE_MAX_RECORDS);
    return true;
}

bool OfflineJsonQueueService::enqueue(const String& payload) {
    if (!ready_) {
        return false;
    }

    if (payload.length() == 0 || payload.length() >= OFFLINE_JSON_MAX_BYTES) {
        Serial.printf("[OfflineQueue] Reject payload len=%u\n", payload.length());
        return false;
    }

    while (count_ >= OFFLINE_JSON_QUEUE_MAX_RECORDS) {
        if (!dropOldest()) {
            return false;
        }
    }

    String path = pathFor(nextId_);
    File file = LittleFS.open(path, FILE_WRITE);
    if (!file) {
        Serial.printf("[OfflineQueue] Failed to open %s for write\n", path.c_str());
        return false;
    }

    size_t written = file.print(payload);
    file.close();
    if (written != payload.length()) {
        LittleFS.remove(path);
        Serial.printf("[OfflineQueue] Short write %u/%u\n", written, payload.length());
        return false;
    }

    nextId_++;
    count_++;
    if (!saveMeta()) {
        return false;
    }

    Serial.printf("[OfflineQueue] Saved payload. queued=%u\n", count_);
    return true;
}

bool OfflineJsonQueueService::peek(String& payload) {
    payload = "";
    if (!ready_ || count_ == 0) {
        return false;
    }

    String path = pathFor(headId_);
    File file = LittleFS.open(path, FILE_READ);
    if (!file) {
        Serial.printf("[OfflineQueue] Missing head %s, dropping metadata entry\n", path.c_str());
        return pop();
    }

    payload = file.readString();
    file.close();
    return payload.length() > 0;
}

bool OfflineJsonQueueService::pop() {
    if (!ready_ || count_ == 0) {
        return false;
    }

    LittleFS.remove(pathFor(headId_));
    headId_++;
    count_--;
    if (count_ == 0) {
        headId_ = nextId_;
    }
    return saveMeta();
}

uint16_t OfflineJsonQueueService::count() const {
    return count_;
}

bool OfflineJsonQueueService::ready() const {
    return ready_;
}

bool OfflineJsonQueueService::loadMeta() {
    File file = LittleFS.open(META_PATH, FILE_READ);
    if (!file) {
        return false;
    }

    String head = file.readStringUntil('\n');
    String next = file.readStringUntil('\n');
    String count = file.readStringUntil('\n');
    file.close();

    headId_ = (uint32_t)strtoul(head.c_str(), nullptr, 10);
    nextId_ = (uint32_t)strtoul(next.c_str(), nullptr, 10);
    count_ = (uint16_t)strtoul(count.c_str(), nullptr, 10);
    if (count_ > OFFLINE_JSON_QUEUE_MAX_RECORDS) {
        count_ = OFFLINE_JSON_QUEUE_MAX_RECORDS;
    }
    if (nextId_ < headId_) {
        nextId_ = headId_ + count_;
    }
    return true;
}

bool OfflineJsonQueueService::saveMeta() {
    File file = LittleFS.open(META_PATH, FILE_WRITE);
    if (!file) {
        Serial.println("[OfflineQueue] Failed to write metadata");
        return false;
    }

    file.printf("%lu\n%lu\n%u\n",
                (unsigned long)headId_,
                (unsigned long)nextId_,
                count_);
    file.close();
    return true;
}

String OfflineJsonQueueService::pathFor(uint32_t id) const {
    char path[32];
    snprintf(path, sizeof(path), "%s/%08lu.json", QUEUE_DIR, (unsigned long)id);
    return String(path);
}

bool OfflineJsonQueueService::dropOldest() {
    if (count_ == 0) {
        return true;
    }

    Serial.println("[OfflineQueue] Queue full, dropping oldest payload");
    return pop();
}
