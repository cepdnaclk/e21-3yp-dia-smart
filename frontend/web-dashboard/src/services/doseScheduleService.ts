import api from "./api";
import { getPatientId } from "../utils/patient";
import type { DoseSchedule, PaginatedDoseSchedulesResponse } from "../types/doseSchedule";

export const doseScheduleService = {
  async getDoseSchedules(customPatientId?: number, page: number = 0, size: number = 20): Promise<PaginatedDoseSchedulesResponse> {
    const patientId = customPatientId || getPatientId();
    const response = await api.get(`/patients/${patientId}/dose-schedules`, {
      params: { page, size }
    });
    return response.data?.data;
  },

  async createDoseSchedule(
    patientId: number,
    data: {
      prescriptionId: number;
      scheduleLabel: string;
      scheduledTime: string; // "HH:MM:SS" or "HH:MM"
      targetTime?: string;
      windowStart?: string;
      windowEnd?: string;
      doseUnits: number;
      daysOfWeek: string;
      allowedEarlyMinutes?: number;
      allowedLateMinutes?: number;
    }
  ): Promise<DoseSchedule> {
    const response = await api.post(`/patients/${patientId}/dose-schedules`, data);
    return response.data?.data;
  },

  async updateDoseSchedule(
    scheduleId: number,
    data: {
      scheduleLabel?: string;
      scheduledTime?: string;
      targetTime?: string;
      windowStart?: string;
      windowEnd?: string;
      doseUnits?: number;
      daysOfWeek?: string;
      allowedEarlyMinutes?: number;
      allowedLateMinutes?: number;
      active?: boolean;
    }
  ): Promise<DoseSchedule> {
    const response = await api.patch(`/dose-schedules/${scheduleId}`, data);
    return response.data?.data;
  },

  async deactivateDoseSchedule(scheduleId: number): Promise<void> {
    await api.delete(`/dose-schedules/${scheduleId}`);
  }
};
