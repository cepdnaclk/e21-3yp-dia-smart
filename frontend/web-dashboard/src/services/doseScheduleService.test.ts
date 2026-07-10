import { describe, it, expect, vi, beforeEach } from "vitest";
import api from "./api";
import { doseScheduleService } from "./doseScheduleService";

vi.mock("./api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn(),
    delete: vi.fn()
  }
}));

describe("doseScheduleService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should list dose schedules successfully", async () => {
    const mockResponse = {
      content: [{ scheduleId: 101, scheduleLabel: "Morning Bolus" }]
    };

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await doseScheduleService.getDoseSchedules(1);

    expect(api.get).toHaveBeenCalledWith("/patients/1/dose-schedules", {
      params: { page: 0, size: 20 }
    });
    expect(result).toEqual(mockResponse);
  });

  it("should create dose schedule successfully", async () => {
    const request = {
      prescriptionId: 1,
      scheduleLabel: "Breakfast Dose",
      scheduledTime: "08:00:00",
      doseUnits: 6,
      daysOfWeek: "1,2,3,4,5,6,7",
      allowedEarlyMinutes: 30,
      allowedLateMinutes: 30
    };

    const mockResponse = { scheduleId: 102, ...request };

    vi.mocked(api.post).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await doseScheduleService.createDoseSchedule(1, request);

    expect(api.post).toHaveBeenCalledWith("/patients/1/dose-schedules", request);
    expect(result).toEqual(mockResponse);
  });

  it("should update dose schedule successfully", async () => {
    const request = { scheduleLabel: "New Label", active: false };
    const mockResponse = { scheduleId: 102, scheduleLabel: "New Label", active: false };

    vi.mocked(api.patch).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await doseScheduleService.updateDoseSchedule(102, request);

    expect(api.patch).toHaveBeenCalledWith("/dose-schedules/102", request);
    expect(result).toEqual(mockResponse);
  });

  it("should deactivate dose schedule successfully", async () => {
    vi.mocked(api.delete).mockResolvedValue({
      data: {}
    });

    await doseScheduleService.deactivateDoseSchedule(102);

    expect(api.delete).toHaveBeenCalledWith("/dose-schedules/102");
  });
});
