import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress, List, ListItem, ListItemText, Chip } from "@mui/material";
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
        setDoses(data);
      } catch (err) {
        console.error("Failed to load today's doses", err);
      } finally {
        setLoading(false);
      }
    };
    fetchDoses();
  }, [patientId]);

  const getStatusChip = (status: string) => {
    switch (status) {
      case "ON_TIME":
        return <Chip label="On Time" color="success" size="small" variant="outlined" icon={<CheckCircleIcon />} />;
      case "LATE":
        return <Chip label="Late" color="warning" size="small" variant="outlined" icon={<AccessTimeIcon />} />;
      case "MISSED":
        return <Chip label="Missed" color="error" size="small" variant="outlined" icon={<CancelIcon />} />;
      default:
        return <Chip label="Unscheduled" color="default" size="small" variant="outlined" />;
    }
  };

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Today's Dose Adherence
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4, flexGrow: 1, alignItems: "center" }}>
            <CircularProgress size={32} />
          </Box>
        ) : doses.length === 0 ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 2 }}>
            No dosing schedules configured for today.
          </Typography>
        ) : (
          <List disablePadding sx={{ maxHeight: 300, overflowY: "auto" }}>
            {doses.map((dose, idx) => (
              <ListItem
                key={dose.scheduleId || idx}
                disableGutters
                sx={{
                  px: 1.5,
                  py: 1,
                  bgcolor: "action.hover",
                  borderRadius: 2,
                  mb: 1
                }}
                secondaryAction={getStatusChip(dose.status)}
              >
                <ListItemText
                  primary={<Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>{dose.scheduleLabel}</Typography>}
                  secondary={`Scheduled Time: ${dose.scheduledTime.substring(0, 5)} • Dosage: ${dose.doseUnits} Units ${dose.injectedAt ? `• Injected: ${new Date(dose.injectedAt).toLocaleTimeString()}` : ""}`}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
};

export default TodayDoseCard;
