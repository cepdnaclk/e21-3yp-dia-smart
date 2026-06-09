import api from "./api";

import type {
  AnalyticsData,
  GlucoseReading,
} from "../types/analytics";

export const analyticsService = {
  async getAnalytics(): Promise<AnalyticsData> {
    const response = await api.get(
      "/analytics/adherence",
      {
        params: {
          patientId: 2,
          startDate: "2026-05-01",
          endDate: "2026-06-09",
        },
      }
    );

    const data = response.data.data;

    return {
      adherenceRate:
        data.adherenceRate,

      totalScheduled:
        data.totalScheduled,

      onTime: data.onTime,

      late: data.late,

      missed: data.missed,

      unscheduled:
        data.unscheduled,
    };
  },

  async getGlucoseHistory(): Promise<
    GlucoseReading[]
  > {
    const response =
      await api.get(
        "/patients/2/glucose-readings"
      );

    return response.data.data.content;
  },
};