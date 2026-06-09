import api from "./api";
import { UserRole } from "../types/roles";

interface LoginResponse {
  accessToken: string;
  expiresInMs: number;

  user: {
    userId: number;
    email: string;
    role: UserRole;
    displayName: string;
  };
}

export const authService = {
  async login(
    email: string,
    password: string
  ): Promise<LoginResponse> {
    const response = await api.post(
      "/auth/login",
      {
        email,
        password,
      }
    );

    return response.data.data;
  },
};