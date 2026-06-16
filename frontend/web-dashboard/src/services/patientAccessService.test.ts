import { describe, it, expect, vi, beforeEach } from "vitest";
import { patientAccessService } from "./patientAccessService";
import api from "./api";

vi.mock("./api", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("patientAccessService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("should call correct endpoint", async () => {
    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: [],
      },
    });

    await patientAccessService.getMyPatientAccess();

    expect(api.get).toHaveBeenCalledWith(
      "/patient-access/me"
    );
  });

  it("should return response data", async () => {
    const patientAccess = [
      {
        patientId: 5,
      },
    ];

    vi.mocked(api.get).mockResolvedValue({
      data: {
        data: patientAccess,
      },
    });

    const result =
      await patientAccessService.getMyPatientAccess();

    expect(result).toEqual(patientAccess);
  });
});