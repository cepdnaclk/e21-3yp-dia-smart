#include "storage_service.h"

#include <stdio.h>
#include <string.h>

namespace {
constexpr const char* NVS_NAMESPACE = "dose_store";
constexpr const char* NVS_FORMAT_KEY = "fmt";
}

bool PenDoseStorageService::begin() {
    if (!initialized) {
        clearVolatileMirror();
        if (!preferences.begin(NVS_NAMESPACE, false)) {
            return false;
        }

        uint8_t storedFormat = preferences.getUChar(NVS_FORMAT_KEY, 0);
        if (storedFormat != PEN_DOSE_RECORD_FORMAT_VERSION) {
            preferences.putUChar(NVS_FORMAT_KEY, PEN_DOSE_RECORD_FORMAT_VERSION);
        }

        if (!loadAllRecords()) {
            return false;
        }

        initialized = true;
    }
    return true;
}

uint8_t PenDoseStorageService::capacity() const {
    return PEN_DOSE_RECORD_CAPACITY;
}

bool PenDoseStorageService::appendPending(const PersistentDoseRecord& record) {
    if (!initialized) {
        return false;
    }

    int slot = findEmptySlot();
    if (slot < 0) {
        return false;
    }

    PersistentDoseRecord pending = record;
    pending.status = DOSE_RECORD_PENDING;

    if (!persistRecord((uint8_t)slot, pending)) {
        return false;
    }

    records[slot] = pending;
    return true;
}

bool PenDoseStorageService::read(uint8_t index, PersistentDoseRecord* out) const {
    if (!initialized || out == nullptr || !isValidIndex(index)) {
        return false;
    }

    *out = records[index];
    return records[index].status != DOSE_RECORD_EMPTY;
}

bool PenDoseStorageService::updateStatus(uint8_t index, DoseRecordStatus status) {
    if (!initialized || !isValidIndex(index)) {
        return false;
    }

    if (records[index].status == DOSE_RECORD_EMPTY) {
        return false;
    }

    PersistentDoseRecord updated = records[index];
    updated.status = status;

    if (!persistRecord(index, updated)) {
        return false;
    }

    records[index] = updated;
    return true;
}

uint8_t PenDoseStorageService::countByStatus(DoseRecordStatus status) const {
    if (!initialized) {
        return 0;
    }

    uint8_t count = 0;
    for (uint8_t i = 0; i < PEN_DOSE_RECORD_CAPACITY; ++i) {
        if (records[i].status == status) {
            ++count;
        }
    }
    return count;
}

void PenDoseStorageService::clearVolatileMirror() {
    memset(records, 0, sizeof(records));
}

bool PenDoseStorageService::isValidIndex(uint8_t index) const {
    return index < PEN_DOSE_RECORD_CAPACITY;
}

int PenDoseStorageService::findEmptySlot() const {
    for (uint8_t i = 0; i < PEN_DOSE_RECORD_CAPACITY; ++i) {
        if (records[i].status == DOSE_RECORD_EMPTY) {
            return i;
        }
    }
    return -1;
}

bool PenDoseStorageService::loadAllRecords() {
    for (uint8_t i = 0; i < PEN_DOSE_RECORD_CAPACITY; ++i) {
        if (!loadRecord(i)) {
            return false;
        }
    }
    return true;
}

bool PenDoseStorageService::loadRecord(uint8_t index) {
    if (!isValidIndex(index)) {
        return false;
    }

    char key[5] = {};
    buildRecordKey(index, key, sizeof(key));

    PersistentDoseRecord record = {};
    size_t bytesRead = preferences.getBytes(key, &record, sizeof(record));

    if (bytesRead == 0) {
        records[index] = {};
        return true;
    }

    if (bytesRead != sizeof(record)) {
        records[index] = {};
        return true;
    }

    records[index] = record;
    return true;
}

bool PenDoseStorageService::persistRecord(uint8_t index, const PersistentDoseRecord& record) {
    if (!isValidIndex(index)) {
        return false;
    }

    char key[5] = {};
    buildRecordKey(index, key, sizeof(key));

    size_t bytesWritten = preferences.putBytes(key, &record, sizeof(record));
    return bytesWritten == sizeof(record);
}

void PenDoseStorageService::buildRecordKey(uint8_t index, char* key, uint8_t keyLen) const {
    if (key == nullptr || keyLen == 0) {
        return;
    }
    snprintf(key, keyLen, "r%02u", index);
}
