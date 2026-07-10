import api from "./api";
import { getPatientId } from "../utils/patient";
import type { Prescription } from "../types/prescription";

export const prescriptionsService = {
  async getPrescriptions(customPatientId?: number): Promise<Prescription[]> {
    const patientId = customPatientId || getPatientId();
    const response = await api.get(`/patients/${patientId}/prescriptions`);
    return response.data?.data?.content ?? [];
  },

  async createPrescription(
    patientId: number,
    data: { prescriptionName: string; startDate: string; endDate: string; notes?: string }
  ): Promise<Prescription> {
    const response = await api.post(`/patients/${patientId}/prescriptions`, data);
    return response.data?.data;
  },

  async updatePrescription(
    prescriptionId: number,
    data: { prescriptionName?: string; startDate?: string; endDate?: string; active?: boolean; notes?: string }
  ): Promise<Prescription> {
    const response = await api.patch(`/prescriptions/${prescriptionId}`, data);
    return response.data?.data;
  },

  async deactivatePrescription(prescriptionId: number): Promise<void> {
    await api.delete(`/prescriptions/${prescriptionId}`);
  }
};