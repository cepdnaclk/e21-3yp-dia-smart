export type DeviceStatus =
  | "ONLINE"
  | "OFFLINE"
  | "UNKNOWN";

export interface Device {
  id: number;
  name: string;
  status: DeviceStatus;
  serialNumber?: string;
  firmwareVersion?: string;
  lastSeenAt?: string;
}
