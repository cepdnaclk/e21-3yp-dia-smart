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