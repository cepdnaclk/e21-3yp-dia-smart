import { describe, it, expect, vi, beforeEach } from "vitest";
import { dashboardService } from "./dashboardService";
import api from "./api";

vi.mock("./api", () => ({
  default: {
    get: vi.fn(),
  },
}));

vi.mock("../utils/patient", () => ({
  getPatientId: () => 12,
}));

describe("dashboardService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should map dashboard response", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: {
          latestGlucoseReading: {
            glucoseValueMgDl: 140,
          },
          latestStorageReading: {
            temperatureCelsius: 5,
          },
          latestInventoryReading: {
            remainingUnits: 80,
          },
          latestDoseEvent: {
            doseUnits: 8,
          },
        },
      },
    });

    const result =
      await dashboardService.getDashboardData();

    expect(result).toEqual({
      glucose: 140,
      temperature: 5,
      inventory: 80,
      lastDose: 8,
    });
  });

  it("should default missing values to zero", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: {},
      },
    });

    const result =
      await dashboardService.getDashboardData();

    expect(result).toEqual({
      glucose: 0,
      temperature: 0,
      inventory: 0,
      lastDose: 0,
    });
  });

  it("should use patient id in endpoint", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: {},
      },
    });

    await dashboardService.getDashboardData();

    expect(api.get).toHaveBeenCalledWith(
      "/patients/12/dashboard-summary"
    );
  });
});