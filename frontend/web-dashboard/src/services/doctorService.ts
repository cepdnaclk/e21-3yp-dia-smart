import api from "./api";
import type { DoctorAssignedPatient } from "../types/doctor";
import type { Alert } from "../types/alert";
import type { AdherenceAnalyticsResponse } from "../types/analytics";

export const doctorService = {
  async getAssignedPatients(): Promise<DoctorAssignedPatient[]> {
    const response = await api.get("/relationships/my-patients");
    return response.data.data;
  },

  async assignPatient(request: {
    userId: number;
    patientId: number;
    accessRole: string;
    relationshipLabel: string;
    canView: boolean;
    canAcknowledgeAlerts: boolean;
    canEditPrescriptions: boolean;
  }): Promise<any> {
    const response = await api.post("/patient-access", request);
    return response.data?.data;
  },

  async getAlerts(status?: string): Promise<Alert[]> {
    const params = status && status !== "ALL" ? { status } : {};
    const response = await api.get("/alerts", { params });
    // In spring-api, AlertController.getAlerts returns a Page<AlertResponse> wrapped in ApiResponse
    return response.data?.data?.content ?? [];
  },

  async acknowledgeAlert(alertId: number): Promise<Alert> {
    const response = await api.patch(`/alerts/${alertId}/acknowledge`);
    return response.data?.data;
  },

  async resolveAlert(alertId: number, note?: string): Promise<Alert> {
    const response = await api.patch(`/alerts/${alertId}/resolve`, {
      resolutionNote: note || ""
    });
    return response.data?.data;
  },

  async getAdherenceAnalytics(
    patientId: number,
    startDate: string,
    endDate: string
  ): Promise<AdherenceAnalyticsResponse> {
    const response = await api.get("/analytics/adherence", {
      params: { patientId, startDate, endDate }
    });
    return response.data?.data;
  }
};
