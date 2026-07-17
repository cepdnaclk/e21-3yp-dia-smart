import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  Typography,
  Box,
  CircularProgress,
  Button,
} from "@mui/material";
import AccessTimeIcon from "@mui/icons-material/AccessTime";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import CancelIcon from "@mui/icons-material/Cancel";

import { caregiverService } from "../../services/caregiverService";
import type { ScheduleAdherenceResponse } from "../../types/analytics";

interface TodayDoseCardProps {
  patientId: number;
}

const TodayDoseCard = ({ patientId }: TodayDoseCardProps) => {
  const [loading, setLoading] = useState(true);
  const [doses, setDoses] = useState<ScheduleAdherenceResponse[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchDoses = async () => {
      try {
        const data = await caregiverService.getPatientTodayAdherence(patientId);
        // Sort doses by scheduled time
        const sortedData = [...data].sort((a, b) =>
          a.scheduledTime.localeCompare(b.scheduledTime)
        );
        setDoses(sortedData);
      } catch (err) {
        console.error("Failed to load today's doses", err);
      } finally {
        setLoading(false);
      }
    };
    fetchDoses();
  }, [patientId]);

  const getStatusStyles = (status: string) => {
    switch (status) {
      case "ON_TIME":
        return {
          color: "#10b981",
          bgColor: "#e6f7ed",
          icon: <CheckCircleIcon sx={{ fontSize: 18 }} />,
          label: "On Time",
        };
      case "LATE":
        return {
          color: "#f59e0b",
          bgColor: "#fffbeb",
          icon: <AccessTimeIcon sx={{ fontSize: 18 }} />,
          label: "Late",
        };
      case "MISSED":
        return {
          color: "#ef4444",
          bgColor: "#fef2f2",
          icon: <CancelIcon sx={{ fontSize: 18 }} />,
          label: "Missed",
        };
      default:
        return {
          color: "#64748b",
          bgColor: "#f1f5f9",
          icon: <AccessTimeIcon sx={{ fontSize: 18 }} />,
          label: "Scheduled",
        };
    }
  };

  return (
    <Card sx={{ height: "100%", borderRadius: 4 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%", p: 3 }}>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3 }}>
          <Typography variant="h6" sx={{ fontWeight: 700, color: "#12233b" }}>
            Today's Dose Adherence
          </Typography>
          <Button size="small" variant="text" sx={{ color: "#3ec1fa", fontWeight: 700 }}>
            View Logs
          </Button>
        </Box>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 6, flexGrow: 1, alignItems: "center" }}>
            <CircularProgress size={32} sx={{ color: "#3ec1fa" }} />
          </Box>
        ) : doses.length === 0 ? (
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", flexGrow: 1, py: 6 }}>
            <Typography color="text.secondary" variant="body2">
              No dosing schedules configured for today.
            </Typography>
          </Box>
        ) : (
          <Box sx={{ display: "flex", flexDirection: "column", gap: 0, flexGrow: 1, mt: 1 }}>
            {doses.map((dose, idx) => {
              const styles = getStatusStyles(dose.status);
              const isLast = idx === doses.length - 1;

              return (
                <Box
                  key={dose.scheduleId || idx}
                  sx={{
                    display: "flex",
                    position: "relative",
                    pb: isLast ? 0 : 3,
                  }}
                >
                  {/* Timeline connector line */}
                  {!isLast && (
                    <Box
                      sx={{
                        position: "absolute",
                        left: 17,
                        top: 36,
                        bottom: 0,
                        width: 2,
                        backgroundColor: "#e2e8f0",
                        zIndex: 1,
                      }}
                    />
                  )}

                  {/* Status Indicator Icon Circle */}
                  <Box
                    sx={{
                      width: 36,
                      height: 36,
                      borderRadius: "50%",
                      backgroundColor: styles.bgColor,
                      color: styles.color,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      zIndex: 2,
                      mr: 2,
                      boxShadow: "0 0 0 4px #fff",
                    }}
                  >
                    {styles.icon}
                  </Box>

                  {/* Timeline Details Content Card */}
                  <Box
                    sx={{
                      flexGrow: 1,
                      backgroundColor: "#f8f9fa",
                      borderRadius: 3,
                      p: 2,
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      border: "1px solid #e2e8f0",
                    }}
                  >
                    <Box>
                      <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#12233b", mb: 0.5 }}>
                        {dose.scheduleLabel}
                      </Typography>
                      <Typography variant="caption" sx={{ color: "text.secondary", display: "block" }}>
                        Scheduled: <strong>{dose.scheduledTime.substring(0, 5)}</strong> • Dosage: <strong>{dose.doseUnits} Units</strong>
                      </Typography>
                      {dose.injectedAt && (
                        <Typography variant="caption" sx={{ color: "success.main", display: "block", mt: 0.5, fontWeight: 600 }}>
                          Injected at {new Date(dose.injectedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                        </Typography>
                      )}
                    </Box>

                    {/* Status Badge */}
                    <Box
                      sx={{
                        px: 1.5,
                        py: 0.5,
                        borderRadius: 2,
                        backgroundColor: styles.bgColor,
                        color: styles.color,
                        fontSize: "0.75rem",
                        fontWeight: 700,
                        textTransform: "uppercase",
                        letterSpacing: "0.5px",
                        border: `1px solid ${styles.color}20`,
                      }}
                    >
                      {styles.label}
                    </Box>
                  </Box>
                </Box>
              );
            })}
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default TodayDoseCard;
