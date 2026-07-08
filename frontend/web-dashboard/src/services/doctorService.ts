import api from "./api";
import type { DoctorAssignedPatient } from "../types/doctor";

export const doctorService = {
  async getAssignedPatients(): Promise<DoctorAssignedPatient[]> {
    const response = await api.get("/relationships/my-patients");
    return response.data.data;
  },
};
