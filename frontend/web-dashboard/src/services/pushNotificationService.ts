import api from "./api";

export interface RegisterDeviceTokenPayload {
  deviceToken: string;
  platform?: string;
}

export const pushNotificationService = {
  async registerDeviceToken(deviceToken: string, platform = "android"): Promise<void> {
    await api.post("/users/device-token", {
      deviceToken,
      platform,
    });
  },

  async unregisterDeviceToken(token: string): Promise<void> {
    await api.delete("/users/device-token", {
      params: { token },
    });
  },
};
