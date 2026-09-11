/**
 * TypeScript interfaces and types for the AI-Assisted Clinical Insight subsystem.
 * Strictly aligned with the Spring Boot public response:
 * com.diasmart.springapi.ai.dto.api.AiClinicalSummaryApiResponse
 */

export type AiConfidenceLevel = "HIGH" | "MEDIUM" | "LOW";

export interface AiObservation {
  statement: string;
  evidenceReferences?: string[];
  evidence_references?: string[];
}

export interface AiCorrelation {
  statement: string;
  confidence: AiConfidenceLevel | string;
  evidenceReferences?: string[];
  evidence_references?: string[];
}

export interface AiProviderMetadata {
  provider: string;
  model: string;
  promptVersion?: string;
  prompt_version?: string;
}

export interface AiClinicalSummaryResponse {
  requestId: string;
  periodFrom: string;
  periodTo: string;
  generatedAt: string;
  summary: string;
  observations: AiObservation[];
  correlations: AiCorrelation[];
  uncertainties: string[];
  discussionPoints: string[];
  safetyNotice: string;
  providerMetadata?: AiProviderMetadata;
}

export interface AiErrorResponse {
  success?: boolean;
  message: string;
  errorCode?: string;
  timestamp?: string;
}

export type ReviewPeriodKey = "24h" | "7d" | "14d" | "30d";

export interface ReviewPeriodPreset {
  id: ReviewPeriodKey;
  label: string;
  durationDays: number;
}

export const REVIEW_PERIOD_PRESETS: readonly ReviewPeriodPreset[] = [
  { id: "24h", label: "Last 24 Hours", durationDays: 1 },
  { id: "7d", label: "Last 7 Days", durationDays: 7 },
  { id: "14d", label: "Last 14 Days", durationDays: 14 },
  { id: "30d", label: "Last 30 Days", durationDays: 30 },
] as const;

export const DEFAULT_PERIOD_PRESET: ReviewPeriodKey = "7d";

/**
 * Calculates timezone-aware UTC ISO-8601 strings from a preset key and reference time.
 * Automatically ensures the time period does not exceed the backend's 31-day limit.
 */
export function calculatePeriodDates(
  presetKey: ReviewPeriodKey,
  referenceDate: Date = new Date()
): { from: string; to: string } {
  const preset = REVIEW_PERIOD_PRESETS.find((p) => p.id === presetKey) ?? REVIEW_PERIOD_PRESETS[1];
  
  const toDate = new Date(referenceDate.getTime());
  const fromDate = new Date(referenceDate.getTime() - preset.durationDays * 24 * 60 * 60 * 1000);

  return {
    from: fromDate.toISOString(),
    to: toDate.toISOString(),
  };
}

/**
 * Normalizes evidence references from either camelCase or snake_case fields.
 */
export function getEvidenceReferences(item: { evidenceReferences?: string[]; evidence_references?: string[] }): string[] {
  return item.evidenceReferences ?? item.evidence_references ?? [];
}
