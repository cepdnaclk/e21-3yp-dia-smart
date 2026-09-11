import api from "./api";
import type { AiClinicalSummaryResponse, AiErrorResponse } from "../types/ai";

/**
 * Standardized error representation for AI summary API calls.
 */
export class AiServiceError extends Error {
  readonly errorCode?: string;
  readonly status?: number;

  constructor(message: string, errorCode?: string, status?: number) {
    super(message);
    this.name = "AiServiceError";
    this.errorCode = errorCode;
    this.status = status;
  }
}

export const aiService = {
  /**
   * Fetches an AI-assisted clinical summary for an authorized patient and date range.
   * Communicates exclusively with the Spring Boot backend (/api/v1/patients/{patientId}/ai-summary).
   *
   * @param patientId The unique ID of the patient
   * @param from Timezone-aware ISO-8601 UTC start timestamp
   * @param to Timezone-aware ISO-8601 UTC end timestamp
   * @param signal Optional AbortSignal for race-condition cancellation
   * @returns Structured clinical summary response
   */
  async getAiClinicalSummary(
    patientId: number,
    from: string,
    to: string,
    signal?: AbortSignal
  ): Promise<AiClinicalSummaryResponse> {
    if (!patientId || patientId <= 0) {
      throw new AiServiceError("A valid patient ID is required.", "INVALID_PATIENT_ID", 400);
    }

    if (!from || !to) {
      throw new AiServiceError("Both 'from' and 'to' period parameters are required.", "INVALID_PERIOD", 400);
    }

    try {
      const response = await api.get<AiClinicalSummaryResponse>(
        `/patients/${patientId}/ai-summary`,
        {
          params: { from, to },
          signal,
        }
      );

      return response.data;
    } catch (err: unknown) {
      if (err instanceof AiServiceError) {
        throw err;
      }

      // Check if request was aborted by AbortController
      if (
        typeof err === "object" &&
        err !== null &&
        "name" in err &&
        ((err as { name: string }).name === "CanceledError" || (err as { name: string }).name === "AbortError")
      ) {
        throw err;
      }

      if (typeof err === "object" && err !== null && "response" in err) {
        const axiosError = err as {
          response?: {
            status: number;
            data?: AiErrorResponse | string;
          };
          message?: string;
        };

        const status = axiosError.response?.status;
        const data = axiosError.response?.data;

        if (data && typeof data === "object" && "errorCode" in data) {
          throw new AiServiceError(
            data.message || "Failed to retrieve clinical summary.",
            data.errorCode,
            status
          );
        }

        if (status === 403) {
          throw new AiServiceError(
            "You do not have permission to view this patient's clinical summary.",
            "ACCESS_DENIED",
            403
          );
        }

        if (status === 404) {
          throw new AiServiceError("Patient not found.", "NOT_FOUND", 404);
        }

        throw new AiServiceError(
          axiosError.message || "An error occurred while communicating with the server.",
          "NETWORK_ERROR",
          status
        );
      }

      throw new AiServiceError("An unexpected network or client error occurred.", "UNKNOWN_ERROR");
    }
  },
};
