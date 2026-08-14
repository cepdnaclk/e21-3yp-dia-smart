import api from "./api";
import type { DashboardData } from "../types/dashboard";
import { getPatientId } from "../utils/patient";

export const dashboardService = {
  async getDashboardData(): Promise<DashboardData> {

    const patientId = getPatientId();
    const response = await api.get(
      `/patients/${patientId}/dashboard-summary`
    );

    const data = response.data.data;

    return {
      glucose:
        data.latestGlucoseReading
          ?.glucoseValueMgDl ?? 0,
      glucoseMeasuredAt:
        data.latestGlucoseReading
          ?.measuredAt,

      temperature:
        data.latestStorageReading
          ?.temperatureC ?? 0,
      temperatureStatus:
        data.latestStorageReading
          ?.temperatureStatus,
      temperatureMeasuredAt:
        data.latestStorageReading
          ?.measuredAt,

      inventory:
        data.latestInventoryReading
          ?.weightG ?? 0,
      inventoryStatus:
        data.latestInventoryReading
          ?.inventoryStatus,
      inventoryMeasuredAt:
        data.latestInventoryReading
          ?.measuredAt,
      estimatedRemainingPercent:
        data.latestInventoryReading
          ?.estimatedRemainingPercent,

      lastDose:
        data.latestDoseEvent
          ?.doseUnits ?? 0,
      lastDoseInjectedAt:
        data.latestDoseEvent
          ?.injectedAt,
    };
  },
};