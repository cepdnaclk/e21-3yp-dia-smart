import api from "./api";
import { getPatientId } from "../utils/patient";

import type {
  AnalyticsData,
  DoseReading,
  GlucoseReading,
} from "../types/analytics";

export const analyticsService = {
  async getAnalytics(customPatientId?: number): Promise<AnalyticsData> {
    const patientId = customPatientId || getPatientId();

    const response = await api.get(
      "/analytics/adherence",
      {
        params: {
          patientId: Number(patientId),
          startDate: "2026-06-22",
          endDate: "2026-06-27",
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

  async getGlucoseHistory(customPatientId?: number): Promise<GlucoseReading[]> {
    const patientId = customPatientId || getPatientId();

    const response = await api.get(
      `/patients/${patientId}/glucose-readings`
    );

    return response.data.data.content;
  },

  async getDoseHistory(customPatientId?: number): Promise<DoseReading[]> {
    const patientId = customPatientId || getPatientId();

    const response = await api.get(
      `/patients/${patientId}/dose-events`
    );

    return response.data.data.content;
  },
};