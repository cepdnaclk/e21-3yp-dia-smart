import { describe, it, expect, vi, beforeEach } from "vitest";
import api from "./api";
import { doctorService } from "./doctorService";

vi.mock("./api", () => ({
  default: {
    get: vi.fn(),
    post: vi.fn(),
    patch: vi.fn()
  }
}));

describe("doctorService", () => {
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

    const result = await doctorService.getAssignedPatients();

    expect(api.get).toHaveBeenCalledWith("/relationships/my-patients");
    expect(result).toEqual(mockPatients);
  });

  it("should get patient alerts successfully", async () => {
    const mockAlerts = [
      { alertId: 101, title: "Hypoglycemia Alert", severity: "CRITICAL" }
    ];

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: {
          content: mockAlerts
        }
      }
    });

    const result = await doctorService.getAlerts("OPEN");

    expect(api.get).toHaveBeenCalledWith("/alerts", { params: { status: "OPEN" } });
    expect(result).toEqual(mockAlerts);
  });

  it("should acknowledge alert successfully", async () => {
    const mockResponse = { alertId: 101, status: "ACKNOWLEDGED" };

    vi.mocked(api.patch).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await doctorService.acknowledgeAlert(101);

    expect(api.patch).toHaveBeenCalledWith("/alerts/101/acknowledge");
    expect(result).toEqual(mockResponse);
  });

  it("should resolve alert successfully", async () => {
    const mockResponse = { alertId: 101, status: "RESOLVED" };

    vi.mocked(api.patch).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await doctorService.resolveAlert(101, "Contacted patient");

    expect(api.patch).toHaveBeenCalledWith("/alerts/101/resolve", {
      resolutionNote: "Contacted patient"
    });
    expect(result).toEqual(mockResponse);
  });

  it("should get adherence analytics successfully", async () => {
    const mockResponse = {
      patientId: 1,
      totalScheduled: 10,
      adherenceRate: 0.9
    };

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await doctorService.getAdherenceAnalytics(1, "2026-05-01", "2026-05-07");

    expect(api.get).toHaveBeenCalledWith("/analytics/adherence", {
      params: { patientId: 1, startDate: "2026-05-01", endDate: "2026-05-07" }
    });
    expect(result).toEqual(mockResponse);
  });

  it("should assign patient successfully", async () => {
    const mockRequest = {
      userId: 5,
      patientId: 1,
      accessRole: "DOCTOR",
      relationshipLabel: "Primary Physician",
      canView: true,
      canAcknowledgeAlerts: true,
      canEditPrescriptions: true
    };
    const mockResponse = { accessId: 10, patientId: 1, userId: 5 };

    vi.mocked(api.post).mockResolvedValue({
      data: {
        data: mockResponse
      }
    });

    const result = await doctorService.assignPatient(mockRequest);

    expect(api.post).toHaveBeenCalledWith("/patient-access", mockRequest);
    expect(result).toEqual(mockResponse);
  });
});
