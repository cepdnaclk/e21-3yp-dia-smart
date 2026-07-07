import { beforeEach, describe, expect, it, vi } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";

import DevicesPage from "./DevicesPage";
import { deviceService } from "../../services/deviceService";

vi.mock("../../services/deviceService", () => ({
  deviceService: {
    getPatientDevices: vi.fn(),
    connectDevice: vi.fn(),
    getDeviceDiagnostics: vi.fn(),
    disconnectDevice: vi.fn(),
  },
}));

describe("DevicesPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    localStorage.setItem("patientId", "42");
  });

  it("shows the setup wizard when no devices are assigned", async () => {
    vi.mocked(deviceService.getPatientDevices).mockResolvedValue([]);

    render(<DevicesPage />);

    await waitFor(() => {
      expect(
        screen.getByText(/No devices are connected to your account/i)
      ).toBeTruthy();
    });
  });
});
