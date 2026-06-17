import api from "./api";
import type {
  Alert,
  AlertsPageResponse,
} from "../types/alert";

export type AlertStatusFilter =
  | "ALL"
  | "OPEN"
  | "ACKNOWLEDGED"
  | "RESOLVED";

export const alertsService = {
  async getAlerts(
    page = 0,
    size = 20,
    status: AlertStatusFilter = "ALL"
  ): Promise<AlertsPageResponse> {
    const response = await api.get(
      "/alerts",
      {
        params: {
          page,
          size,
          ...(status !== "ALL"
            ? { status }
            : {}),
        },
      }
    );

    return response.data.data;
  },

  async acknowledgeAlert(
    alertId: number
  ): Promise<Alert> {
    const response = await api.patch(
      `/alerts/${alertId}/acknowledge`
    );

    return response.data.data;
  },

  async resolveAlert(
    alertId: number
  ): Promise<Alert> {
    const response = await api.patch(
      `/alerts/${alertId}/resolve`
    );

    return response.data.data;
  },
};
