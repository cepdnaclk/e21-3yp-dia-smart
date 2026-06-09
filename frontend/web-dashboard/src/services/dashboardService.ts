import api from "./api";
import type { DashboardData } from "../types/dashboard";

export const dashboardService = {
  async getDashboardData(): Promise<DashboardData> {
    const response = await api.get(
      "/patients/2/dashboard-summary"
    );

    const data = response.data.data;

    return {
      glucose:
        data.latestGlucoseReading
          ?.glucoseValueMgDl ?? 0,

      temperature:
        data.latestStorageReading
          ?.temperatureCelsius ?? 0,

      inventory:
        data.latestInventoryReading
          ?.remainingUnits ?? 0,

      lastDose:
        data.latestDoseEvent
          ?.doseUnits ?? 0,
    };
  },
};