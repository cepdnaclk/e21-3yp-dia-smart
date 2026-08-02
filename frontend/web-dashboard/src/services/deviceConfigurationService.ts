import axios from "axios";
import api from "./api";

// --- TYPES & INTERFACES ---

export interface DeviceConfigurationResponse {
  configurationId: number;
  outerDeviceId: number;
  patientId: number;
  innerDeviceId?: number;
  penDeviceId?: number;
  glucometerDeviceId?: number;
  wifiSsid: string;
  configurationStatus: "PENDING" | "PUBLISHED" | "FAILED" | string;
  outerUnitStatus: "PENDING" | "PUBLISHED" | "FAILED" | string;
  innerUnitStatus: "NOT_CONFIGURED" | "PENDING" | "CONNECTED" | "FAILED" | string;
  innerUnitIpAddress?: string;
  innerUnitMessage?: string;
  lastInnerUnitStatusAt?: string;
  configurationVersion: number;
  lastSyncedAt?: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateDeviceConfigurationRequest {
  outerDeviceId: number;
  wifiSsid: string;
  wifiPassword?: string;
  innerDeviceId?: number;
  penDeviceId?: number;
  glucometerDeviceId?: number;
}

export interface UpdateDeviceConfigurationRequest {
  wifiSsid?: string;
  wifiPassword?: string;
  innerDeviceId?: number;
  penDeviceId?: number;
  glucometerDeviceId?: number;
}

export interface LocalProvisionPayload {
  ssid: string;
  password?: string;
}

export interface LocalProvisionStatusResponse {
  status: "idle" | "connecting" | "success" | "error" | string;
  outerStatus?: "PENDING" | "CONNECTING" | "CONNECTED" | "FAILED" | string;
  innerStatus?: "PENDING" | "CONNECTING" | "CONNECTED" | "FAILED" | string;
  message?: string;
}

// --- LOCAL API CLIENT ---
// Isolated Axios instance specifically for local communication with the Outer unit's SoftAP.
// This client excludes default interceptors, authorization headers, or session cookies
// to ensure sensitive tokens are never sent to local hardware.
const localClient = axios.create({
  timeout: 10000,
});

// --- SERVICE IMPLEMENTATION ---

export const deviceConfigurationService = {
  // CLOUD APIs: Interact with the central Dia-Smart spring-api backend
  
  createConfiguration: async (
    dto: CreateDeviceConfigurationRequest
  ): Promise<DeviceConfigurationResponse> => {
    const response = await api.post("/patient/device-configurations", dto);
    return response.data?.data;
  },

  updateConfiguration: async (
    outerDeviceId: number,
    dto: UpdateDeviceConfigurationRequest
  ): Promise<DeviceConfigurationResponse> => {
    const response = await api.put(`/patient/device-configurations/${outerDeviceId}`, dto);
    return response.data?.data;
  },

  getConfigurationStatus: async (
    outerDeviceId: number
  ): Promise<DeviceConfigurationResponse> => {
    const response = await api.get(`/patient/device-configurations/${outerDeviceId}/status`);
    return response.data?.data;
  },

  // LOCAL APIs: Communicates directly with the Outer Unit's web service over its SoftAP (192.168.4.1)
  
  provisionLocalDevice: async (
    payload: LocalProvisionPayload
  ): Promise<void> => {
    await localClient.post("http://192.168.4.1/api/provision", payload, {
      headers: {
        "Content-Type": "application/json",
      },
    });
  },

  getLocalProvisionStatus: async (): Promise<LocalProvisionStatusResponse> => {
    const response = await localClient.get("http://192.168.4.1/api/provision/status");
    return response.data;
  },
};
