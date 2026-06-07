import { Profile } from "../types/profile";

export const profileService = {
  async getProfile(): Promise<Profile> {
    return {
      fullName: "John Silva",
      age: 68,
      gender: "Male",
      diabetesType: "Type 2",
      doctor: "Dr. Perera",
      caregiver: "Mary Silva",
      emergencyContact: "+94 77 123 4567",
    };
  },
};