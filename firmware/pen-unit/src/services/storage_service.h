#pragma once

#include <stdint.h>
#include "../models/persistent_dose_record.h"

class PenDoseStorageService {
public:
    bool begin();
    uint8_t capacity() const;
    bool appendPending(const PersistentDoseRecord& record);
    bool read(uint8_t index, PersistentDoseRecord* out) const;
    bool updateStatus(uint8_t index, DoseRecordStatus status);
    uint8_t countByStatus(DoseRecordStatus status) const;
    void clearVolatileMirror();

private:
    PersistentDoseRecord records[PEN_DOSE_RECORD_CAPACITY] = {};
    bool initialized = false;

    bool isValidIndex(uint8_t index) const;
    int findEmptySlot() const;
};
