import api from "./api";

export interface PatientProfileResponse {
  patientId: number;
  fullName: string;
  dateOfBirth: string;
  gender: string;
  diabetesType: string;
  contactNumber: string;
  emergencyContactNumber: string;
}

export const patientsService = {
  async getPatients() {
    return [
      {
        id: 1,
        name: "John Silva",
        age: 68,
        glucose: 118,
        inventory: 42,
        status: "Stable",
      },
      {
        id: 2,
        name: "Nimal Perera",
        age: 72,
        glucose: 210,
        inventory: 15,
        status: "Critical",
      },
      {
        id: 3,
        name: "Kamala Fernando",
        age: 65,
        glucose: 132,
        inventory: 38,
        status: "Stable",
      },
    ];
  },

  async getPatientProfile(patientId: number): Promise<PatientProfileResponse> {
    const response = await api.get(`/patients/${patientId}`);
    return response.data.data;
  },
};