import { describe, it, expect, vi, beforeEach } from "vitest";
import { aiService, AiServiceError } from "./aiService";
import api from "./api";
import type { AiClinicalSummaryResponse } from "../types/ai";

vi.mock("./api", () => ({
  default: {
    get: vi.fn(),
  },
}));

describe("aiService", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const validResponse: AiClinicalSummaryResponse = {
    requestId: "823ca2aa-cbca-495a-87e8-70e6f475d5ae",
    periodFrom: "2026-09-01T00:00:00Z",
    periodTo: "2026-09-08T00:00:00Z",
    generatedAt: "2026-09-08T00:01:00Z",
    summary: "Clinical overview for patient demonstrates steady glycemic balance.",
    observations: [
      {
        statement: "Glucose stability observed across waking hours.",
        evidenceReferences: ["glucose_reading:ref-001", "glucose_reading:ref-002"],
      },
    ],
    correlations: [
      {
        statement: "Morning walking routine correlates with reduced postprandial glucose spike.",
        confidence: "HIGH",
        evidenceReferences: ["glucose_reading:ref-001", "dose_event:ref-002"],
      },
    ],
    uncertainties: [
      "No continuous glucose telemetry available during late night periods.",
    ],
    discussionPoints: [
      "Review overnight target ranges with clinician during next consultation.",
    ],
    safetyNotice:
      "This AI-generated summary is for clinical decision support only and does not provide medical diagnosis or treatment recommendations.",
    providerMetadata: {
      provider: "mock",
      model: "mock-model-v1",
      promptVersion: "clinical-summary-v1",
    },
  };

  it("should successfully fetch clinical summary from Spring Boot endpoint", async () => {
    vi.mocked(api.get).mockResolvedValue({ data: validResponse });

    const patientId = 42;
    const from = "2026-09-01T00:00:00Z";
    const to = "2026-09-08T00:00:00Z";
    const controller = new AbortController();

    const result = await aiService.getAiClinicalSummary(patientId, from, to, controller.signal);

    expect(api.get).toHaveBeenCalledWith("/patients/42/ai-summary", {
      params: { from, to },
      signal: controller.signal,
    });
    expect(result).toEqual(validResponse);
    expect(result.requestId).toBe("823ca2aa-cbca-495a-87e8-70e6f475d5ae");
    expect(result.providerMetadata?.provider).toBe("mock");
  });

  it("should throw validation error when patientId is invalid", async () => {
    await expect(
      aiService.getAiClinicalSummary(0, "2026-09-01T00:00:00Z", "2026-09-08T00:00:00Z")
    ).rejects.toThrow("A valid patient ID is required.");

    expect(api.get).not.toHaveBeenCalled();
  });

  it("should throw validation error when period parameters are missing", async () => {
    await expect(aiService.getAiClinicalSummary(42, "", "2026-09-08T00:00:00Z")).rejects.toThrow(
      "Both 'from' and 'to' period parameters are required."
    );

    expect(api.get).not.toHaveBeenCalled();
  });

  it("should correctly map backend ErrorResponse with custom errorCode", async () => {
    vi.mocked(api.get).mockRejectedValue({
      response: {
        status: 503,
        data: {
          success: false,
          message: "AI subsystem is disabled.",
          errorCode: "AI_DISABLED",
        },
      },
    });

    try {
      await aiService.getAiClinicalSummary(42, "2026-09-01T00:00:00Z", "2026-09-08T00:00:00Z");
      expect.unreachable("Should have thrown AiServiceError");
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(AiServiceError);
      const aiErr = err as AiServiceError;
      expect(aiErr.errorCode).toBe("AI_DISABLED");
      expect(aiErr.status).toBe(503);
      expect(aiErr.message).toBe("AI subsystem is disabled.");
    }
  });

  it("should map 403 Forbidden to ACCESS_DENIED error", async () => {
    vi.mocked(api.get).mockRejectedValue({
      response: {
        status: 403,
        data: "Forbidden",
      },
    });

    try {
      await aiService.getAiClinicalSummary(42, "2026-09-01T00:00:00Z", "2026-09-08T00:00:00Z");
      expect.unreachable("Should have thrown");
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(AiServiceError);
      const aiErr = err as AiServiceError;
      expect(aiErr.errorCode).toBe("ACCESS_DENIED");
      expect(aiErr.status).toBe(403);
    }
  });

  it("should map 404 Not Found to NOT_FOUND error", async () => {
    vi.mocked(api.get).mockRejectedValue({
      response: {
        status: 404,
        data: "Not Found",
      },
    });

    try {
      await aiService.getAiClinicalSummary(42, "2026-09-01T00:00:00Z", "2026-09-08T00:00:00Z");
      expect.unreachable("Should have thrown");
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(AiServiceError);
      const aiErr = err as AiServiceError;
      expect(aiErr.errorCode).toBe("NOT_FOUND");
      expect(aiErr.status).toBe(404);
    }
  });

  it("should map network connection failure to NETWORK_ERROR", async () => {
    vi.mocked(api.get).mockRejectedValue({
      message: "Network Error",
    });

    try {
      await aiService.getAiClinicalSummary(42, "2026-09-01T00:00:00Z", "2026-09-08T00:00:00Z");
      expect.unreachable("Should have thrown");
    } catch (err: unknown) {
      expect(err).toBeInstanceOf(AiServiceError);
      const aiErr = err as AiServiceError;
      expect(aiErr.errorCode).toBe("UNKNOWN_ERROR");
    }
  });

  it("should propagate cancellation errors without wrapping", async () => {
    const cancelError = { name: "CanceledError", message: "canceled" };
    vi.mocked(api.get).mockRejectedValue(cancelError);

    await expect(
      aiService.getAiClinicalSummary(42, "2026-09-01T00:00:00Z", "2026-09-08T00:00:00Z")
    ).rejects.toEqual(cancelError);
  });
});
