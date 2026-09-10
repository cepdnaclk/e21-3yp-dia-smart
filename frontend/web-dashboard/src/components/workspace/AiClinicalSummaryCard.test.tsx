import "@testing-library/jest-dom/vitest";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import AiClinicalSummaryCard from "./AiClinicalSummaryCard";
import { aiService, AiServiceError } from "../../services/aiService";
import type { AiClinicalSummaryResponse } from "../../types/ai";

vi.mock("../../services/aiService", () => ({
  aiService: {
    getAiClinicalSummary: vi.fn(),
  },
  AiServiceError: class extends Error {
    readonly errorCode?: string;
    readonly status?: number;
    constructor(message: string, errorCode?: string, status?: number) {
      super(message);
      this.name = "AiServiceError";
      this.errorCode = errorCode;
      this.status = status;
    }
  },
}));

describe("AiClinicalSummaryCard", () => {
  const mockPatientId = 42;

  const sampleSuccessResponse: AiClinicalSummaryResponse = {
    requestId: "11111111-2222-3333-4444-555555555555",
    periodFrom: "2026-09-01T00:00:00Z",
    periodTo: "2026-09-08T00:00:00Z",
    generatedAt: "2026-09-08T00:01:00Z",
    summary: "Patient demonstrates consistent glycemic metrics within target ranges.",
    observations: [
      {
        statement: "Mean glucose levels stabilized around target parameters.",
        evidenceReferences: ["glucose_reading:ref-001", "glucose_reading:ref-002"],
      },
      {
        statement: "Insulin doses were administered on schedule.",
        evidence_references: ["dose_event:ref-003"],
      },
    ],
    correlations: [
      {
        statement: "Evening physical activity is associated with lower nocturnal glucose fluctuations.",
        confidence: "HIGH",
        evidenceReferences: ["glucose_reading:ref-001", "dose_event:ref-003"],
      },
    ],
    uncertainties: [
      "Continuous sensor telemetry missing during exercise window.",
      "Self-reported snack logs have unverified carbohydrate quantities.",
    ],
    discussionPoints: [
      "Review target threshold adjustments during the next clinic visit.",
    ],
    safetyNotice:
      "This AI-generated summary is for clinical decision support only and does not provide medical diagnosis or treatment recommendations.",
    providerMetadata: {
      provider: "mock",
      model: "mock-model-v1",
      promptVersion: "clinical-summary-v1",
    },
  };

  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the card in its initial idle state without triggering an API request", () => {
    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    expect(screen.getByRole("heading", { name: /AI-Assisted Clinical Insight/i })).toBeInTheDocument();
    expect(
      screen.getByText(/Review patterns found in the selected Dia-Smart records/i)
    ).toBeInTheDocument();

    const generateButton = screen.getByRole("button", { name: /Generate clinical insight/i });
    expect(generateButton).toBeInTheDocument();
    expect(generateButton).toBeEnabled();

    expect(aiService.getAiClinicalSummary).not.toHaveBeenCalled();
    expect(screen.queryByText(/Generating clinical insight/i)).not.toBeInTheDocument();
  });

  it("fetches and renders structured summary when Generate Summary button is clicked", async () => {
    vi.mocked(aiService.getAiClinicalSummary).mockResolvedValue(sampleSuccessResponse);

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    const generateButton = screen.getByRole("button", { name: /Generate clinical insight/i });
    fireEvent.click(generateButton);

    expect(aiService.getAiClinicalSummary).toHaveBeenCalledTimes(1);
    expect(aiService.getAiClinicalSummary).toHaveBeenCalledWith(
      mockPatientId,
      expect.stringMatching(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/),
      expect.stringMatching(/^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}/),
      expect.any(AbortSignal)
    );

    // Verify loading state is triggered
    expect(screen.getByText(/Generating clinical insight.../i)).toBeInTheDocument();

    // Await success rendering
    await waitFor(() => {
      expect(screen.getByText("Summary")).toBeInTheDocument();
    });

    expect(screen.getByText(sampleSuccessResponse.summary)).toBeInTheDocument();

    // Verify observations
    expect(screen.getByText("Observations")).toBeInTheDocument();
    expect(
      screen.getByText("Mean glucose levels stabilized around target parameters.")
    ).toBeInTheDocument();
    expect(screen.getAllByText("glucose_reading:ref-001").length).toBeGreaterThanOrEqual(1);
    expect(screen.getAllByText("dose_event:ref-003").length).toBeGreaterThanOrEqual(1);

    // Verify correlations (Observed Associations)
    expect(screen.getByText("Observed Associations")).toBeInTheDocument();
    expect(
      screen.getByText(/Associations in recorded data do not establish causation/i)
    ).toBeInTheDocument();
    expect(
      screen.getByText("Evening physical activity is associated with lower nocturnal glucose fluctuations.")
    ).toBeInTheDocument();
    expect(screen.getByText("Confidence: HIGH")).toBeInTheDocument();

    // Verify uncertainties
    expect(screen.getByText("Limitations and Uncertainty")).toBeInTheDocument();
    expect(
      screen.getByText(/Continuous sensor telemetry missing during exercise window/i)
    ).toBeInTheDocument();

    // Verify discussion points
    expect(screen.getByText("Points for Clinical Review")).toBeInTheDocument();
    expect(
      screen.getByText(/Review target threshold adjustments during the next clinic visit/i)
    ).toBeInTheDocument();

    // Verify safety notice verbatim
    expect(screen.getByText(sampleSuccessResponse.safetyNotice)).toBeInTheDocument();

    // Verify provider metadata badge
    expect(screen.getByText("Provider: mock")).toBeInTheDocument();

    // Verify button text updates to "Refresh Summary"
    expect(screen.getByRole("button", { name: /Refresh clinical insight/i })).toBeInTheDocument();
  });

  it("omits optional sections when correlations and discussionPoints are empty", async () => {
    const minimalResponse: AiClinicalSummaryResponse = {
      ...sampleSuccessResponse,
      correlations: [],
      discussionPoints: [],
    };
    vi.mocked(aiService.getAiClinicalSummary).mockResolvedValue(minimalResponse);

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(screen.getByText("Summary")).toBeInTheDocument();
    });

    // Mandatory sections are present
    expect(screen.getByText("Observations")).toBeInTheDocument();
    expect(screen.getByText("Limitations and Uncertainty")).toBeInTheDocument();
    expect(screen.getByText(sampleSuccessResponse.safetyNotice)).toBeInTheDocument();

    // Optional sections are omitted
    expect(screen.queryByText("Observed Associations")).not.toBeInTheDocument();
    expect(screen.queryByText("Points for Clinical Review")).not.toBeInTheDocument();
  });

  it("handles AI_DISABLED error safely without breaking workspace", async () => {
    vi.mocked(aiService.getAiClinicalSummary).mockRejectedValue(
      new AiServiceError("AI subsystem is disabled.", "AI_DISABLED", 503)
    );

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(
        screen.getByText("AI-assisted clinical insight is currently unavailable.")
      ).toBeInTheDocument();
    });

    expect(screen.queryByRole("button", { name: /Retry/i })).not.toBeInTheDocument();
    expect(screen.queryByText("Summary")).not.toBeInTheDocument();
  });

  it("handles AI_INSUFFICIENT_DATA error with clear message", async () => {
    vi.mocked(aiService.getAiClinicalSummary).mockRejectedValue(
      new AiServiceError("Not enough readings.", "AI_INSUFFICIENT_DATA", 400)
    );

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(
        screen.getByText(
          "There is not enough information in the selected period to generate a clinical insight."
        )
      ).toBeInTheDocument();
    });
  });

  it("handles AI_GATEWAY_UNAVAILABLE and allows retry", async () => {
    vi.mocked(aiService.getAiClinicalSummary).mockRejectedValueOnce(
      new AiServiceError("Microservice down.", "AI_GATEWAY_UNAVAILABLE", 503)
    );

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(
        screen.getByText(
          "The AI-assisted insight service is temporarily unavailable. Please try again later."
        )
      ).toBeInTheDocument();
    });

    const retryButton = screen.getByRole("button", { name: /Retry/i });
    expect(retryButton).toBeInTheDocument();

    // Next attempt succeeds
    vi.mocked(aiService.getAiClinicalSummary).mockResolvedValueOnce(sampleSuccessResponse);
    fireEvent.click(retryButton);

    await waitFor(() => {
      expect(screen.getByText("Summary")).toBeInTheDocument();
    });
    expect(screen.getByText(sampleSuccessResponse.summary)).toBeInTheDocument();
  });

  it("handles AI_GATEWAY_TIMEOUT error gracefully", async () => {
    vi.mocked(aiService.getAiClinicalSummary).mockRejectedValue(
      new AiServiceError("Timeout occurred.", "AI_GATEWAY_TIMEOUT", 504)
    );

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(screen.getByText("The insight request took too long. Please try again.")).toBeInTheDocument();
    });
    expect(screen.getByRole("button", { name: /Retry/i })).toBeInTheDocument();
  });

  it("handles 403 Forbidden access denial gracefully", async () => {
    vi.mocked(aiService.getAiClinicalSummary).mockRejectedValue(
      new AiServiceError("Forbidden.", "ACCESS_DENIED", 403)
    );

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(
        screen.getByText("You do not have permission to view clinical insights for this patient.")
      ).toBeInTheDocument();
    });
  });

  it("renders potentially malicious script tags safely as plain text without executing XSS", async () => {
    const maliciousResponse: AiClinicalSummaryResponse = {
      ...sampleSuccessResponse,
      summary: "<script>window.__xss_executed__ = true;</script>Normal summary with script tag.",
      observations: [
        {
          statement: "<img src='x' onerror='window.__xss_executed__=true;' /> Observation with tag",
          evidenceReferences: ["<svg onload=alert(1)>"],
        },
      ],
    };
    vi.mocked(aiService.getAiClinicalSummary).mockResolvedValue(maliciousResponse);

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(screen.getByText("Summary")).toBeInTheDocument();
    });

    // Content is rendered literally as string text
    expect(
      screen.getByText("<script>window.__xss_executed__ = true;</script>Normal summary with script tag.")
    ).toBeInTheDocument();

    // Verify script element does NOT exist in DOM
    const scriptElements = document.querySelectorAll("script");
    const injectedScript = Array.from(scriptElements).find((s) =>
      s.textContent?.includes("__xss_executed__")
    );
    expect(injectedScript).toBeUndefined();
    expect((window as unknown as { __xss_executed__?: boolean }).__xss_executed__).toBeUndefined();
  });

  it("enforces defense-in-depth safety check when backend returns empty uncertainties", async () => {
    const invalidResponse: AiClinicalSummaryResponse = {
      ...sampleSuccessResponse,
      uncertainties: [], // Invalid per safety requirements
    };
    vi.mocked(aiService.getAiClinicalSummary).mockResolvedValue(invalidResponse);

    render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(
        screen.getByText("The generated insight could not be safely displayed. Please try again later.")
      ).toBeInTheDocument();
    });

    // Summary content must NOT be displayed
    expect(screen.queryByText("Observations")).not.toBeInTheDocument();
  });

  it("clears previous summary data when patientId prop changes", async () => {
    vi.mocked(aiService.getAiClinicalSummary).mockResolvedValue(sampleSuccessResponse);

    const { rerender } = render(<AiClinicalSummaryCard patientId={mockPatientId} />);

    fireEvent.click(screen.getByRole("button", { name: /Generate clinical insight/i }));

    await waitFor(() => {
      expect(screen.getByText(sampleSuccessResponse.summary)).toBeInTheDocument();
    });

    // Rerender with different patientId
    rerender(<AiClinicalSummaryCard patientId={999} />);

    // Stale summary must be cleared immediately
    expect(screen.queryByText(sampleSuccessResponse.summary)).not.toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Generate clinical insight/i })).toBeInTheDocument();
  });
});
