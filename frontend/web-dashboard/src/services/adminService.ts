import api from "./api";
import type {
  AdminUserRecord,
  AdminCreateUserRequest,
  PatientAccessResponse,
  CreatePatientAccessRequest,
  PaginatedAuditLogsResponse
} from "../types/admin";

export const adminService = {
  getAllUsers: async (): Promise<AdminUserRecord[]> => {
    const response = await api.get("/admin/users");
    return response.data?.data ?? [];
  },

  createUser: async (request: AdminCreateUserRequest): Promise<AdminUserRecord> => {
    const response = await api.post("/admin/users", request);
    return response.data?.data;
  },

  updateUserStatus: async (userId: number, active: boolean): Promise<AdminUserRecord> => {
    const response = await api.patch(`/admin/users/${userId}/status`, { active });
    return response.data?.data;
  },

  getPatientAccessForUser: async (userId: number): Promise<PatientAccessResponse[]> => {
    const response = await api.get(`/patient-access/user/${userId}`);
    return response.data?.data ?? [];
  },

  createPatientAccess: async (request: CreatePatientAccessRequest): Promise<PatientAccessResponse> => {
    const response = await api.post("/patient-access", request);
    return response.data?.data;
  },

  revokePatientAccess: async (accessId: number): Promise<PatientAccessResponse> => {
    const response = await api.patch(`/patient-access/${accessId}/revoke`);
    return response.data?.data;
  },

  getAuditLogs: async (page: number = 0, size: number = 20): Promise<PaginatedAuditLogsResponse> => {
    const response = await api.get("/admin/audit-logs", {
      params: { page, size }
    });
    return response.data?.data;
  }
};
