#include "storage_service.h"

#include <string.h>

bool PenDoseStorageService::begin() {
    if (!initialized) {
        clearVolatileMirror();
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

    records[slot] = record;
    records[slot].status = DOSE_RECORD_PENDING;
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

    records[index].status = status;
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
