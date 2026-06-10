import api from "./api";

import type { PatientAccess } from "../types/patientAccess";

export const patientAccessService = {
  async getMyPatientAccess(): Promise<
    PatientAccess[]
  > {
    const response =
      await api.get(
        "/patient-access/me"
      );

    return response.data.data;
  },
};