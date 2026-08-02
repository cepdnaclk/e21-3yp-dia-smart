import api from "./api";
import type { Device, DeviceDiagnostics } from "../types/device";
import { getPatientId } from "../utils/patient";

export interface PatientDeviceActivationRequest {
  outerGatewayId: string;
  innerUnitId: string;
  penUnitId: string;
  glucoseMeterId: string;
}

const findDeviceMatch = (
  devices: Device[],
  reference: string
) => {
  const trimmedReference = reference.trim();

  return devices.find((device) => {
    const candidates = [
      device.deviceUid,
      device.serialNumber,
      device.deviceName,
      device.deviceId?.toString(),
    ];

    return candidates.some(
      (candidate) =>
        candidate &&
        candidate.toString().toLowerCase() ===
          trimmedReference.toLowerCase()
    );
  });
};

export const deviceService = {
  getPatientDevices: async (): Promise<Device[]> => {
    const patientId = getPatientId();
    const response = await api.get("/devices");
    const devices = response.data?.data ?? [];

    return devices.filter(
      (device: Device) =>
        String(device.patientId ?? "") ===
        String(patientId)
    );
  },

  connectDevice: async (
    deviceReference: string
  ): Promise<Device> => {
    const patientId = getPatientId();
    const response = await api.get("/devices");
    const devices = response.data?.data ?? [];
    const matchingDevice = findDeviceMatch(
      devices,
      deviceReference
    );

    if (!matchingDevice) {
      throw new Error(
        "No matching device was found. Please verify the device ID."
      );
    }

    if (
      matchingDevice.patientId &&
      String(matchingDevice.patientId) !== String(patientId)
    ) {
      throw new Error(
        "This device is already assigned to another patient. Please disconnect it from the existing account before connecting it to this account."
      );
    }

    if (
      matchingDevice.patientId &&
      String(matchingDevice.patientId) === String(patientId)
    ) {
      throw new Error(
        "This device is already connected to your account."
      );
    }

    await api.patch(`/devices/${matchingDevice.deviceId}/assign`, {
      patientId: Number(patientId),
    });

    return {
      ...matchingDevice,
      patientId: Number(patientId),
    };
  },

  getDeviceDiagnostics: async (
    deviceId: number
  ): Promise<DeviceDiagnostics> => {
    const response = await api.get(
      `/devices/${deviceId}/diagnostics`
    );

    return response.data?.data ?? {};
  },

  disconnectDevice: async (
    deviceId: number
  ): Promise<void> => {
    await api.delete(`/devices/${deviceId}/assign`);
  },

  activateDeviceKit: async (
    patientId: string | number,
    request: PatientDeviceActivationRequest
  ): Promise<void> => {
    await api.post(`/patients/${patientId}/devices/activate-kit`, request);
  },
};
