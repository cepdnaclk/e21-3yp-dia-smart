import api from "./api";
import { getPatientId } from "../utils/patient";
import type { Prescription } from "../types/prescription";

export const prescriptionsService = {
  async getPrescriptions(customPatientId?: number): Promise<Prescription[]> {
    const patientId = customPatientId || getPatientId();

    const response = await api.get(
      `/patients/${patientId}/prescriptions`
    );

    return response.data.data.content;
  },
};