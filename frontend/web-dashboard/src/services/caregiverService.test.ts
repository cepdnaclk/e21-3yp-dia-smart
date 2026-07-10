import { describe, it, expect, vi, beforeEach } from "vitest";
import api from "./api";
import { caregiverService } from "./caregiverService";

vi.mock("./api", () => ({
  default: {
    get: vi.fn()
  }
}));

describe("caregiverService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should get assigned patients successfully", async () => {
    const mockPatients = [
      { patientId: 1, patientName: "Alice Smith" },
      { patientId: 2, patientName: "Bob Jones" }
    ];

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: mockPatients
      }
    });

    const result = await caregiverService.getAssignedPatients();

    expect(api.get).toHaveBeenCalledWith("/relationships/my-patients");
    expect(result).toEqual(mockPatients);
  });

  it("should get patient alerts successfully", async () => {
    const mockAlerts = [
      { alertId: 101, title: "Low Battery Warning", severity: "CRITICAL" }
    ];

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: {
          content: mockAlerts
        }
      }
    });

    const result = await caregiverService.getAlerts("OPEN");

    expect(api.get).toHaveBeenCalledWith("/alerts", { params: { status: "OPEN" } });
    expect(result).toEqual(mockAlerts);
  });

  it("should get patient today adherence successfully", async () => {
    const mockAdherence = [
      { scheduleId: 1, status: "MISSED", doseUnits: 5 }
    ];

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: mockAdherence
      }
    });

    const result = await caregiverService.getPatientTodayAdherence(1);

    expect(api.get).toHaveBeenCalledWith("/patients/1/schedule-adherence");
    expect(result).toEqual(mockAdherence);
  });
});
