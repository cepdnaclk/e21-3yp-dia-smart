import api from "./api";
import { getPatientId } from "../utils/patient";

import type {
  AnalyticsData,
  DoseReading,
  GlucoseReading,
} from "../types/analytics";

export const analyticsService = {
  async getAnalytics(customPatientId?: number, startDate?: string, endDate?: string): Promise<AnalyticsData> {
    const patientId = customPatientId || getPatientId();

    const response = await api.get(
      "/analytics/adherence",
      {
        params: {
          patientId: Number(patientId),
          startDate: startDate || "2026-06-22",
          endDate: endDate || "2026-06-27",
        },
      }
    );

    const data = response.data.data;

    return {
      adherenceRate: data.adherenceRate,
      totalScheduled: data.totalScheduled,
      onTime: data.onTime,
      late: data.late,
      missed: data.missed,
      unscheduled: data.unscheduled,
    };
  },

  async getGlucoseHistory(customPatientId?: number, size = 200): Promise<GlucoseReading[]> {
    const patientId = customPatientId || getPatientId();

    const response = await api.get(
      `/patients/${patientId}/glucose-readings`,
      { params: { size } }
    );

    return response.data.data.content;
  },

  async getDoseHistory(customPatientId?: number, size = 200): Promise<DoseReading[]> {
    const patientId = customPatientId || getPatientId();

    const response = await api.get(
      `/patients/${patientId}/dose-events`,
      { params: { size } }
    );

    return response.data.data.content;
  },
};