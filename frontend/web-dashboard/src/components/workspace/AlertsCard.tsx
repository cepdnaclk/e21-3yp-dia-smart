import { useState, useEffect } from "react";
import { Card, CardContent, Typography, List, ListItem, ListItemText, Box, CircularProgress } from "@mui/material";
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
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Active Alerts
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress />
          </Box>
        ) : alerts.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No active alerts for this patient
          </Typography>
        ) : (
          <List disablePadding sx={{ maxHeight: 300, overflowY: "auto" }}>
            {alerts.map((alert) => (
              <ListItem key={alert.alertId} disableGutters>
                <ListItemText
                  primary={alert.title}
                  secondary={alert.message}
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
