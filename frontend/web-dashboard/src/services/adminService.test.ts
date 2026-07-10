import { describe, it, expect, vi, beforeEach } from "vitest";
import { adminService } from "./adminService";
import api from "./api";
import { UserRole } from "../types/roles";

vi.mock("./api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
  },
}));

describe("adminService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should get all users successfully", async () => {
    const mockUsers = [
      {
        userId: 1,
        userUuid: "uuid-1",
        email: "admin@diasmart.com",
        role: UserRole.ADMIN,
        displayName: "Admin User",
        contactNumber: "123",
        active: true,
        createdAt: "2026-05-09T00:00:00Z",
        updatedAt: "2026-05-09T00:00:00Z"
      }
    ];

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: mockUsers
      }
    });

    const result = await adminService.getAllUsers();

    expect(api.get).toHaveBeenCalledWith("/admin/users");
    expect(result).toEqual(mockUsers);
  });

  it("should create user successfully", async () => {
    const request = {
      displayName: "New Patient",
      email: "patient@diasmart.com",
      password: "password123",
      role: UserRole.PATIENT,
      active: true
    };

    const mockResponse = {
      userId: 2,
      userUuid: "uuid-2",
      email: request.email,
      role: request.role,
      displayName: request.displayName,
      contactNumber: "",
      active: true,
      createdAt: "2026-05-09T00:00:00Z",
      updatedAt: "2026-05-09T00:00:00Z"
    };

    vi.mocked(api.post).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await adminService.createUser(request);

    expect(api.post).toHaveBeenCalledWith("/admin/users", request);
    expect(result).toEqual(mockResponse);
  });

  it("should update user status successfully", async () => {
    const mockResponse = {
      userId: 1,
      userUuid: "uuid-1",
      email: "admin@diasmart.com",
      role: UserRole.ADMIN,
      displayName: "Admin User",
      contactNumber: "123",
      active: false,
      createdAt: "2026-05-09T00:00:00Z",
      updatedAt: "2026-05-09T00:00:00Z"
    };

    vi.mocked(api.patch).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await adminService.updateUserStatus(1, false);

    expect(api.patch).toHaveBeenCalledWith("/admin/users/1/status", { active: false });
    expect(result).toEqual(mockResponse);
  });

  it("should get patient access for user successfully", async () => {
    const mockAccess = [
      {
        accessId: 10,
        userId: 1,
        patientId: 2,
        accessRole: "DOCTOR",
        canView: true,
        canAcknowledgeAlerts: true,
        canEditPrescriptions: false,
        status: "ACTIVE",
        createdAt: "2026-05-09T00:00:00Z"
      }
    ];

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: mockAccess
      }
    });

    const result = await adminService.getPatientAccessForUser(1);

    expect(api.get).toHaveBeenCalledWith("/patient-access/user/1");
    expect(result).toEqual(mockAccess);
  });

  it("should create patient access successfully", async () => {
    const request = {
      userId: 1,
      patientId: 2,
      accessRole: "DOCTOR" as const,
      relationshipLabel: "Primary Doctor",
      canView: true,
      canAcknowledgeAlerts: true,
      canEditPrescriptions: false
    };

    const mockResponse = {
      accessId: 10,
      ...request,
      status: "ACTIVE",
      createdAt: "2026-05-09T00:00:00Z"
    };

    vi.mocked(api.post).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await adminService.createPatientAccess(request);

    expect(api.post).toHaveBeenCalledWith("/patient-access", request);
    expect(result).toEqual(mockResponse);
  });

  it("should revoke patient access successfully", async () => {
    const mockResponse = {
      accessId: 10,
      userId: 1,
      patientId: 2,
      accessRole: "DOCTOR",
      status: "REVOKED",
      createdAt: "2026-05-09T00:00:00Z",
      revokedAt: "2026-05-09T01:00:00Z"
    };

    vi.mocked(api.patch).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await adminService.revokePatientAccess(10);

    expect(api.patch).toHaveBeenCalledWith("/patient-access/10/revoke");
    expect(result).toEqual(mockResponse);
  });
});
