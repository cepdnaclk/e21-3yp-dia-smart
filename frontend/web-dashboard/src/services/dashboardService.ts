//import { DashboardData } from "../types/dashboard";

export const dashboardService = {
  async getDashboardData(): Promise<DashboardData> {
    return {
      glucose: 118,
      temperature: 5.4,
      inventory: 41.8,
      lastDose: 12,
    };
  },
};