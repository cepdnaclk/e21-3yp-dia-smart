import api from "./api";

export const profileService = {
  async getProfile() {
    const response =
      await api.get("/users/me");

    return response.data.data;
  },
};