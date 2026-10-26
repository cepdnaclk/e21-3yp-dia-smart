import { useState, useEffect } from "react";
import { Card, CardContent, List, ListItem, ListItemText, Box, CircularProgress } from "@mui/material";
import { alpha } from "@mui/material/styles";
import NotificationsActiveOutlinedIcon from "@mui/icons-material/NotificationsActiveOutlined";
import WarningAmberRoundedIcon from "@mui/icons-material/WarningAmberRounded";
import CheckCircleOutlineIcon from "@mui/icons-material/CheckCircleOutlined";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import { alertsService } from "../../services/alertsService";
import type { Alert } from "../../types/alert";

interface AlertsCardProps {
  patientId: number;
  refreshTrigger?: number;
}

const AlertsCard = ({ patientId, refreshTrigger }: AlertsCardProps) => {
  const [loading, setLoading] = useState(true);
  const [alerts, setAlerts] = useState<Alert[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchAlerts = async (silent = false) => {
      try {
        if (!silent) setLoading(true);
        const response = await alertsService.getAlerts(0, 100);
        const filtered = (response?.content || []).filter((a) => a.patientId === patientId);
        setAlerts(filtered);
      } catch (err) {
        console.error("Failed to load alerts", err);
      } finally {
        if (!silent) setLoading(false);
      }
    };

    const isInitial = !refreshTrigger || refreshTrigger === 0;
    fetchAlerts(!isInitial);
  }, [patientId, refreshTrigger]);

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading title="Active Alerts" icon={<NotificationsActiveOutlinedIcon />} color="#ef4444" />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: 160 }}>
            <CircularProgress size={28} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : alerts.length === 0 ? (
          <EmptyState message="No active alerts for this patient" icon={<CheckCircleOutlineIcon sx={{ color: "#10b981" }} />} />
        ) : (
          <List disablePadding sx={{ maxHeight: 300, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1, pr: 0.5 }}>
            {alerts.map((alert) => (
              <ListItem
                key={alert.alertId}
                disableGutters
                sx={{
                  alignItems: "flex-start",
                  gap: 1.25,
                  px: 1.5,
                  py: 1.25,
                  borderRadius: 2.5,
                  border: "1px solid",
                  borderColor: alpha("#ef4444", 0.2),
                  borderLeft: "4px solid #ef4444",
                  bgcolor: alpha("#ef4444", 0.04),
                }}
              >
                <WarningAmberRoundedIcon aria-hidden sx={{ color: "#ef4444", fontSize: 20, mt: 0.25, flexShrink: 0 }} />
                <ListItemText
                  sx={{ m: 0, minWidth: 0 }}
                  primary={alert.title}
                  secondary={alert.message}
                  slotProps={{
                    primary: { sx: { fontWeight: 700, color: "#12233b", fontSize: 14, overflowWrap: "anywhere" } },
                    secondary: { sx: { fontSize: 13, overflowWrap: "anywhere" } },
                  }}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
};

export default AlertsCard;
