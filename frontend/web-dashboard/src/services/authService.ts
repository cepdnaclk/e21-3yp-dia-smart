// import api from "./api";
// import { UserRole } from "../types/roles";

// interface LoginResponse {
//   accessToken: string;
//   expiresInMs: number;

//   user: {
//     userId: number;
//     email: string;
//     role: UserRole;
//     displayName: string;
//   };
// }

// export const authService = {
//   async login(
//     email: string,
//     password: string
//   ): Promise<LoginResponse> {
//     const response = await api.post(
//       "/auth/login",
//       {
//         email,
//         password,
//       }
//     );

//     return response.data.data;
//   },
// };

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

export interface RegisterRequest {
  displayName: string;
  email: string;
  password: string;
  role: "PATIENT" | "CAREGIVER" | "DOCTOR";
  contactNumber?: string;
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

  async register(
    request: RegisterRequest
  ) {
    const response = await api.post(
      "/auth/register",
      request
    );

    return response.data.data;
  },
};