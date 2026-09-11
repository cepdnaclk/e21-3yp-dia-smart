import React, { useState, useEffect, useRef, useTransition } from "react";
import {
  Card,
  CardContent,
  Typography,
  Box,
  Button,
  Select,
  MenuItem,
  FormControl,
  InputLabel,
  CircularProgress,
  Alert,
  Chip,
  Stack,
  Divider,
  List,
  ListItem,
  ListItemText,
} from "@mui/material";
import RefreshIcon from "@mui/icons-material/Refresh";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { aiService, AiServiceError } from "../../services/aiService";
import {
  type AiClinicalSummaryResponse,
  type AiConfidenceLevel,
  DEFAULT_PERIOD_PRESET,
  REVIEW_PERIOD_PRESETS,
  type ReviewPeriodKey,
  calculatePeriodDates,
  getEvidenceReferences,
} from "../../types/ai";

export interface AiClinicalSummaryCardProps {
  patientId: number;
  refreshTrigger?: number; // Intentionally unused to prevent automatic repeated AI polling
}

/**
 * Returns user-safe error message based on backend error codes.
 */
function mapErrorMessage(error: unknown): { message: string; canRetry: boolean } {
  if (error instanceof AiServiceError) {
    switch (error.errorCode) {
      case "AI_DISABLED":
        return {
          message: "AI-assisted clinical insight is currently unavailable.",
          canRetry: false,
        };
      case "AI_CONFIGURATION_ERROR":
      case "SERVICE_UNAVAILABLE":
        return {
          message: "The AI-assisted insight service is temporarily unavailable.",
          canRetry: true,
        };
      case "AI_INSUFFICIENT_DATA":
        return {
          message: "There is not enough information in the selected period to generate a clinical insight.",
          canRetry: false,
        };
      case "AI_GATEWAY_UNAVAILABLE":
        return {
          message: "The AI-assisted insight service is temporarily unavailable. Please try again later.",
          canRetry: true,
        };
      case "AI_GATEWAY_TIMEOUT":
        return {
          message: "The insight request took too long. Please try again.",
          canRetry: true,
        };
      case "AI_INVALID_RESPONSE":
        return {
          message: "The generated insight could not be safely displayed. Please try again later.",
          canRetry: true,
        };
      case "INVALID_PERIOD":
        return {
          message: "Invalid review period selected. Please select a period within 31 days.",
          canRetry: false,
        };
      case "ACCESS_DENIED":
      case "FORBIDDEN":
        return {
          message: "You do not have permission to view clinical insights for this patient.",
          canRetry: false,
        };
      case "NOT_FOUND":
        return {
          message: "Patient not found.",
          canRetry: false,
        };
      default:
        if (error.status === 403) {
          return {
            message: "You do not have permission to view clinical insights for this patient.",
            canRetry: false,
          };
        }
        if (error.status === 404) {
          return {
            message: "Patient not found.",
            canRetry: false,
          };
        }
        return {
          message: error.message || "An error occurred while generating the clinical insight.",
          canRetry: true,
        };
    }
  }

  return {
    message: "An unexpected error occurred while communicating with the insight service.",
    canRetry: true,
  };
}

function getConfidenceChipColor(confidence: AiConfidenceLevel | string): "success" | "warning" | "default" {
  switch (confidence) {
    case "HIGH":
      return "success";
    case "MEDIUM":
      return "warning";
    default:
      return "default";
  }
}

function formatDisplayDate(isoString: string): string {
  try {
    const d = new Date(isoString);
    return isNaN(d.getTime()) ? isoString : d.toLocaleDateString(undefined, {
      month: "short",
      day: "numeric",
      year: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  } catch {
    return isoString;
  }
}

const AiClinicalSummaryCard: React.FC<AiClinicalSummaryCardProps> = ({ patientId }) => {
  const [selectedPeriod, setSelectedPeriod] = useState<ReviewPeriodKey>(DEFAULT_PERIOD_PRESET);
  const [loading, setLoading] = useState(false);
  const [summaryData, setSummaryData] = useState<AiClinicalSummaryResponse | null>(null);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [canRetry, setCanRetry] = useState(false);
  const [, startTransition] = useTransition();

  const [prevPatientId, setPrevPatientId] = useState(patientId);

  // When patient ID changes, reset state during render per React guidelines
  if (patientId !== prevPatientId) {
    setPrevPatientId(patientId);
    setSummaryData(null);
    setErrorMessage(null);
    setLoading(false);
  }

  // Reference for request cancellation
  const abortControllerRef = useRef<AbortController | null>(null);
  const activeRequestIdRef = useRef<number>(0);

  // Abort any in-flight request when patientId changes or component unmounts
  useEffect(() => {
    return () => {
      if (abortControllerRef.current) {
        abortControllerRef.current.abort();
        abortControllerRef.current = null;
      }
    };
  }, [patientId]);

  const handleGenerate = async (periodKey: ReviewPeriodKey = selectedPeriod) => {
    if (!patientId || loading) return;

    // Abort previous in-flight request if exists
    if (abortControllerRef.current) {
      abortControllerRef.current.abort();
    }

    const currentRequestId = ++activeRequestIdRef.current;
    const controller = new AbortController();
    abortControllerRef.current = controller;

    setLoading(true);
    setErrorMessage(null);

    const { from, to } = calculatePeriodDates(periodKey);

    try {
      const response = await aiService.getAiClinicalSummary(
        patientId,
        from,
        to,
        controller.signal
      );

      // Race-condition guard: ensure only the latest initiated request applies
      if (currentRequestId !== activeRequestIdRef.current) {
        return;
      }

      // Defense-in-depth safety check: uncertainties are mandatory
      if (!response.uncertainties || response.uncertainties.length === 0) {
        throw new AiServiceError(
          "The generated insight does not include required uncertainty disclosures.",
          "AI_INVALID_RESPONSE",
          502
        );
      }

      startTransition(() => {
        setSummaryData(response);
        setErrorMessage(null);
      });
    } catch (err: unknown) {
      // Ignore aborted requests
      if (
        typeof err === "object" &&
        err !== null &&
        "name" in err &&
        ((err as { name: string }).name === "CanceledError" ||
          (err as { name: string }).name === "AbortError")
      ) {
        return;
      }

      if (currentRequestId !== activeRequestIdRef.current) {
        return;
      }

      const { message, canRetry: retryAllowed } = mapErrorMessage(err);
      startTransition(() => {
        setErrorMessage(message);
        setCanRetry(retryAllowed);
      });
    } finally {
      if (currentRequestId === activeRequestIdRef.current) {
        setLoading(false);
      }
    }
  };

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        {/* Header Section */}
        <Box sx={{ mb: 2 }}>
          <Typography variant="h6" component="h2" sx={{ fontWeight: 600, color: "text.primary" }}>
            AI-Assisted Clinical Insight
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mt: 0.5 }}>
            Review patterns found in the selected Dia-Smart records. This feature supports clinical
            review and does not provide diagnosis or treatment recommendations.
          </Typography>
        </Box>

        {/* Controls Section: Period selector and Generate button */}
        <Stack
          direction={{ xs: "column", sm: "row" }}
          spacing={2}
          sx={{ mb: 3, alignItems: { xs: "stretch", sm: "center" } }}
        >
          <FormControl size="small" sx={{ minWidth: 200 }}>
            <InputLabel id="ai-period-select-label">Review Period</InputLabel>
            <Select
              labelId="ai-period-select-label"
              id="ai-period-select"
              value={selectedPeriod}
              label="Review Period"
              disabled={loading}
              onChange={(e) => {
                const newPeriod = e.target.value as ReviewPeriodKey;
                setSelectedPeriod(newPeriod);
              }}
            >
              {REVIEW_PERIOD_PRESETS.map((preset) => (
                <MenuItem key={preset.id} value={preset.id}>
                  {preset.label}
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <Button
            variant="contained"
            color="primary"
            onClick={() => handleGenerate(selectedPeriod)}
            disabled={loading}
            startIcon={summaryData && !loading ? <RefreshIcon /> : undefined}
            aria-label={summaryData ? "Refresh clinical insight" : "Generate clinical insight"}
            sx={{ px: 3, fontWeight: 600 }}
          >
            {loading
              ? "Generating..."
              : summaryData
              ? "Refresh Summary"
              : "Generate Summary"}
          </Button>
        </Stack>

        {/* Loading State */}
        {loading && (
          <Box
            sx={{
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              gap: 2,
              py: 5,
            }}
            role="status"
            aria-live="polite"
          >
            <CircularProgress size={26} />
            <Typography variant="body2" color="text.secondary">
              Generating clinical insight...
            </Typography>
          </Box>
        )}

        {/* Error State */}
        {errorMessage && !loading && (
          <Alert
            severity="warning"
            sx={{ mb: 2, borderRadius: 1.5 }}
            action={
              canRetry ? (
                <Button
                  color="inherit"
                  size="small"
                  onClick={() => handleGenerate(selectedPeriod)}
                >
                  Retry
                </Button>
              ) : undefined
            }
          >
            {errorMessage}
          </Alert>
        )}

        {/* Success Structured Summary */}
        {summaryData && !loading && (
          <Box sx={{ mt: 2 }}>
            <Divider sx={{ mb: 2 }} />

            {/* Metadata Badges */}
            <Box
              sx={{
                display: "flex",
                flexDirection: { xs: "column", sm: "row" },
                alignItems: { xs: "flex-start", sm: "center" },
                justifyContent: "space-between",
                gap: 1,
                mb: 2,
              }}
            >
              <Typography variant="caption" color="text.secondary">
                Period: {formatDisplayDate(summaryData.periodFrom)} – {formatDisplayDate(summaryData.periodTo)}
              </Typography>
              <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                <Typography variant="caption" color="text.secondary">
                  Generated: {formatDisplayDate(summaryData.generatedAt)}
                </Typography>
                {summaryData.providerMetadata?.provider && (
                  <Chip
                    label={`Provider: ${summaryData.providerMetadata.provider}`}
                    size="small"
                    variant="outlined"
                    sx={{ fontSize: "0.7rem", height: 20 }}
                  />
                )}
              </Box>
            </Box>

            {/* Section 1: Clinical Summary */}
            <Box sx={{ mb: 2.5 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", mb: 0.5 }}>
                Summary
              </Typography>
              <Typography variant="body2" color="text.primary" sx={{ lineHeight: 1.6 }}>
                {summaryData.summary}
              </Typography>
            </Box>

            {/* Section 2: Observations */}
            {summaryData.observations && summaryData.observations.length > 0 && (
              <Box sx={{ mb: 2.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", mb: 0.5 }}>
                  Observations
                </Typography>
                <Box component="ul" sx={{ listStyle: "none", p: 0, m: 0 }}>
                  {summaryData.observations.map((obs, idx) => {
                    const refs = getEvidenceReferences(obs);
                    return (
                      <Box component="li" key={idx} sx={{ py: 0.75 }}>
                        <Typography variant="body2" color="text.primary">
                          {obs.statement}
                        </Typography>
                        {refs.length > 0 && (
                          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5, mt: 0.5 }}>
                            {refs.map((ref) => (
                              <Chip
                                key={ref}
                                label={ref}
                                size="small"
                                variant="outlined"
                                sx={{ fontSize: "0.68rem", height: 18 }}
                              />
                            ))}
                          </Box>
                        )}
                      </Box>
                    );
                  })}
                </Box>
              </Box>
            )}

            {/* Section 3: Observed Associations (Correlations) - Rendered only if non-empty */}
            {summaryData.correlations && summaryData.correlations.length > 0 && (
              <Box sx={{ mb: 2.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", mb: 0.25 }}>
                  Observed Associations
                </Typography>
                <Typography variant="caption" color="text.secondary" sx={{ display: "block", mb: 1 }}>
                  Associations in recorded data do not establish causation.
                </Typography>
                <Box component="ul" sx={{ listStyle: "none", p: 0, m: 0 }}>
                  {summaryData.correlations.map((corr, idx) => {
                    const refs = getEvidenceReferences(corr);
                    return (
                      <Box component="li" key={idx} sx={{ py: 0.75 }}>
                        <Box sx={{ display: "flex", alignItems: "center", flexWrap: "wrap", gap: 1 }}>
                          <Typography variant="body2" color="text.primary">
                            {corr.statement}
                          </Typography>
                          {corr.confidence && (
                            <Chip
                              label={`Confidence: ${corr.confidence}`}
                              size="small"
                              color={getConfidenceChipColor(corr.confidence)}
                              sx={{ fontSize: "0.68rem", height: 18 }}
                            />
                          )}
                        </Box>
                        {refs.length > 0 && (
                          <Box sx={{ display: "flex", flexWrap: "wrap", gap: 0.5, mt: 0.5 }}>
                            {refs.map((ref) => (
                              <Chip
                                key={ref}
                                label={ref}
                                size="small"
                                variant="outlined"
                                sx={{ fontSize: "0.68rem", height: 18 }}
                              />
                            ))}
                          </Box>
                        )}
                      </Box>
                    );
                  })}
                </Box>
              </Box>
            )}

            {/* Section 4: Limitations and Uncertainty - Always required */}
            {summaryData.uncertainties && summaryData.uncertainties.length > 0 && (
              <Box sx={{ mb: 2.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", mb: 0.5 }}>
                  Limitations and Uncertainty
                </Typography>
                <List disablePadding>
                  {summaryData.uncertainties.map((item, idx) => (
                    <ListItem key={idx} disableGutters sx={{ py: 0.25 }}>
                      <ListItemText
                        primary={
                          <Typography variant="body2" color="text.secondary">
                            • {item}
                          </Typography>
                        }
                      />
                    </ListItem>
                  ))}
                </List>
              </Box>
            )}

            {/* Section 5: Points for Clinical Review - Rendered only if non-empty */}
            {summaryData.discussionPoints && summaryData.discussionPoints.length > 0 && (
              <Box sx={{ mb: 2.5 }}>
                <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", mb: 0.5 }}>
                  Points for Clinical Review
                </Typography>
                <List disablePadding>
                  {summaryData.discussionPoints.map((point, idx) => (
                    <ListItem key={idx} disableGutters sx={{ py: 0.25 }}>
                      <ListItemText
                        primary={
                          <Typography variant="body2" color="text.primary">
                            • {point}
                          </Typography>
                        }
                      />
                    </ListItem>
                  ))}
                </List>
              </Box>
            )}

            {/* Section 6: Safety Notice - Mandatory disclaimer verbatim from backend */}
            <Alert
              severity="info"
              icon={<WarningAmberIcon />}
              sx={{ mt: 3, borderRadius: 1.5, "& .MuiAlert-message": { width: "100%" } }}
            >
              <Typography variant="body2" sx={{ fontWeight: 500, color: "text.primary" }}>
                {summaryData.safetyNotice}
              </Typography>
            </Alert>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default AiClinicalSummaryCard;
