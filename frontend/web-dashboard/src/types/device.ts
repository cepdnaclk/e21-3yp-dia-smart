export type DeviceStatus =
  | "ONLINE"
  | "OFFLINE"
  | "UNKNOWN"
  | "LOW_BATTERY"
  | "DEACTIVATED";

export interface Device {
  deviceId: number;
  patientId?: number | null;
  deviceUid: string;
  deviceType?: string;
  deviceName?: string;
  serialNumber?: string;
  firmwareVersion?: string;
  hardwareVersion?: string;
  status?: DeviceStatus | string;
  online?: boolean;
  batteryPercent?: number | null;
  lastSeenAt?: string | null;
  active?: boolean;
  communicationType?: string;
  createdAt?: string;
  updatedAt?: string;
  notes?: string;
}

export interface DeviceDiagnostics {
  deviceId: number;
  deviceUid: string;
  deviceType?: string;
  deviceName?: string;
  status?: DeviceStatus | string;
  online?: boolean;
  firmwareVersion?: string;
  hardwareVersion?: string;
  lastMqttReceivedAt?: string | null;
  lastSeenAt?: string | null;
  batteryPercent?: number | null;
  latestHealthAt?: string | null;
  powerSource?: string;
  wifiRssiDbm?: number | null;
  bleRssiDbm?: number | null;
  freeHeapBytes?: number | null;
}
