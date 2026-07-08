import api from "./api";
import type { CaregiverAssignedPatient } from "../types/caregiver";

export const caregiverService = {
  async getAssignedPatients(): Promise<CaregiverAssignedPatient[]> {
    const response = await api.get("/relationships/my-patients");
    return response.data.data;
  },
};
