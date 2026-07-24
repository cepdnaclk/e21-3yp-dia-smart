import api from "./api";
import type { Profile } from "../types/profile";

export const profileService = {
  async getProfile(): Promise<Profile> {
    const response =
      await api.get("/users/me");

    return response.data.data;
  },

  async updateProfile(displayName: string, contactNumber: string): Promise<Profile> {
    const response = await api.patch("/users/me", { displayName, contactNumber });
    return response.data.data;
  },

  async updatePassword(currentPassword: string, newPassword: string): Promise<Profile> {
    const response = await api.patch("/users/me/password", { currentPassword, newPassword });
    return response.data.data;
  },
};