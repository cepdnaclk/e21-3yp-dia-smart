import api from "./api";
import type { Alert } from "../types/alert";

export const alertsService = {
  async getAlerts(): Promise<Alert[]> {
    const response =
      await api.get("/alerts");

    return response.data.data.content;
  },
};