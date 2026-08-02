import { describe, it, expect, vi, beforeEach } from "vitest";
import { authService } from "./authService";
import api from "./api";

vi.mock("./api", () => ({
  default: {
    post: vi.fn(),
  },
}));

describe("authService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should login successfully", async () => {
    const mockResponse = {
      data: {
        data: {
          accessToken: "jwt-token",
          expiresInMs: 1000,
          user: {
            userId: 1,
            email: "test@test.com",
            role: "PATIENT",
            displayName: "Test",
          },
        },
      },
    };

    vi.mocked(api.post).mockResolvedValue(mockResponse);

    const result = await authService.login(
      "test@test.com",
      "password"
    );

    expect(api.post).toHaveBeenCalledWith(
      "/auth/login",
      {
        email: "test@test.com",
        password: "password",
      }
    );

    expect(result.accessToken).toBe(
      "jwt-token"
    );
  });

  it("should register successfully", async () => {
    const request = {
      displayName: "Test",
      email: "test@test.com",
      password: "123456",
      role: "PATIENT" as const,
    };

    vi.mocked(api.post).mockResolvedValue({
      data: {
        data: {
          success: true,
        },
      },
    });

    await authService.register(request);

    expect(api.post).toHaveBeenCalledWith(
      "/auth/register",
      request
    );
  });
});