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
            temperatureC: 5,
          },
          latestInventoryReading: {
            weightG: 80,
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
      glucoseMeasuredAt: undefined,
      temperature: 5,
      temperatureStatus: undefined,
      temperatureMeasuredAt: undefined,
      inventory: 80,
      inventoryStatus: undefined,
      inventoryMeasuredAt: undefined,
      estimatedRemainingPercent: undefined,
      lastDose: 8,
      lastDoseInjectedAt: undefined,
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
      glucoseMeasuredAt: undefined,
      temperature: 0,
      temperatureStatus: undefined,
      temperatureMeasuredAt: undefined,
      inventory: 0,
      inventoryStatus: undefined,
      inventoryMeasuredAt: undefined,
      estimatedRemainingPercent: undefined,
      lastDose: 0,
      lastDoseInjectedAt: undefined,
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