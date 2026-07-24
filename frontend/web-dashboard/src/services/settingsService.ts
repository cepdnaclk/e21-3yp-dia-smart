import api from "./api";

export interface UserSettings {
  inventoryAlerts: boolean;
  temperatureAlerts: boolean;
  missedDoseAlerts: boolean;
  emailNotifications: boolean;
  smsNotifications: boolean;
  twoFactorAuth: boolean;
}

export const settingsService = {
  async getSettings(): Promise<UserSettings> {
    const response = await api.get("/users/settings");
    return response.data.data;
  },

  async updateSettings(settings: UserSettings): Promise<UserSettings> {
    const response = await api.put("/users/settings", settings);
    return response.data.data;
  },
};
