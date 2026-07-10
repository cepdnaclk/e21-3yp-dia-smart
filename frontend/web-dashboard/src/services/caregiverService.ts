import api from "./api";
import type { CaregiverAssignedPatient } from "../types/caregiver";
import type { Alert } from "../types/alert";
import type { ScheduleAdherenceResponse } from "../types/analytics";

export const caregiverService = {
  async getAssignedPatients(): Promise<CaregiverAssignedPatient[]> {
    const response = await api.get("/relationships/my-patients");
    return response.data.data;
  },

  async getAlerts(status?: string): Promise<Alert[]> {
    const params = status && status !== "ALL" ? { status } : {};
    const response = await api.get("/alerts", { params });
    return response.data?.data?.content ?? [];
  },

  async getPatientTodayAdherence(patientId: number): Promise<ScheduleAdherenceResponse[]> {
    const response = await api.get(`/patients/${patientId}/schedule-adherence`);
    return response.data?.data ?? [];
  }
};
