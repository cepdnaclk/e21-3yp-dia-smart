import api from "./api";
import { getPatientId } from "../utils/patient";

import type {
  AnalyticsData,
  GlucoseReading,
} from "../types/analytics";

export const analyticsService = {
  async getAnalytics(): Promise<AnalyticsData> {
    const patientId =
      getPatientId();

    const response = await api.get(
      "/analytics/adherence",
      {
        params: {
          patientId:
            Number(patientId),

          startDate:
            "2026-05-01",

          endDate:
            "2027-06-09",
        },
      }
    );

    const data =
      response.data.data;

    return {
      adherenceRate:
        data.adherenceRate,

      totalScheduled:
        data.totalScheduled,

      onTime:
        data.onTime,

      late:
        data.late,

      missed:
        data.missed,

      unscheduled:
        data.unscheduled,
    };
  },

  async getGlucoseHistory(): Promise<
    GlucoseReading[]
  > {
    const patientId =
      getPatientId();

    const response =
      await api.get(
        `/patients/${patientId}/glucose-readings`
      );

    return response.data.data.content;
  },
};