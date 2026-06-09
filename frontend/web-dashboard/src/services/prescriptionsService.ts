import api from "./api";
import type { Prescription } from "../types/prescription";

export const prescriptionsService = {
  async getPrescriptions(): Promise<
    Prescription[]
  > {
    const response =
      await api.get(
        "/patients/2/prescriptions"
      );

    // One thing to note for later:

    // "/patients/2/prescriptions"

    // is currently hardcoded. Since you've identified that:

    // userId = 3
    // patientId = 2

    // you should eventually store patientId after login and use:

    // const patientId =
    //   localStorage.getItem("patientId");

    // api.get(
    //   `/patients/${patientId}/prescriptions`
    // );

    return response.data.data.content;
  },
};