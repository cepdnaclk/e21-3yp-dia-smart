import { describe, it, expect, vi, beforeEach } from "vitest";
import { pushNotificationService } from "./pushNotificationService";
import api from "./api";

vi.mock("./api", () => ({
  default: {
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

describe("pushNotificationService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should register device token via POST API", async () => {
    vi.mocked(api.post).mockResolvedValue({ data: { success: true } });

    await pushNotificationService.registerDeviceToken("token-123", "android");

    expect(api.post).toHaveBeenCalledWith("/users/device-token", {
      deviceToken: "token-123",
      platform: "android",
    });
  });

  it("should unregister device token via DELETE API", async () => {
    vi.mocked(api.delete).mockResolvedValue({ data: { success: true } });

    await pushNotificationService.unregisterDeviceToken("token-123");

    expect(api.delete).toHaveBeenCalledWith("/users/device-token", {
      params: { token: "token-123" },
    });
  });
});
