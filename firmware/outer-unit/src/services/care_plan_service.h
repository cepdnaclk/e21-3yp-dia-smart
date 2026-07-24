#pragma once

#include <Arduino.h>

#include "models/care_plan.h"

bool setupCarePlanService();
CarePlanApplyResult applyCarePlanPayload(const uint8_t* payload, size_t length);
CarePlanView getCarePlanViewSnapshot();
void carePlanTick();
void carePlanSelectPreviousSchedule();
void carePlanSelectNextSchedule();
void carePlanFocusCurrentSchedule();
void carePlanMarkDoseTaken(float doseUnits);
