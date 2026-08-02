#pragma once

#include <stdint.h>

struct KeypadEvent {
    char key;
    uint32_t timestampMs;
};
