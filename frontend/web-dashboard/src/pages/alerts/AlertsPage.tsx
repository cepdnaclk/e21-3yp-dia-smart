import { useEffect, useState } from "react";

import {
  Typography,
  Stack,
  Alert as MuiAlert,
  CircularProgress,
  Box,
} from "@mui/material";

import AlertCard from "../../components/alerts/AlertCard";

import { alertsService } from "../../services/alertsService";
import type { Alert } from "../../types/alert";

const mapSeverity = (
  severity: string
):
  | "error"
  | "warning"
  | "info"
  | "success" => {
  switch (
    severity?.toUpperCase()
  ) {
    case "CRITICAL":
    case "HIGH":
      return "error";

    case "MEDIUM":
      return "warning";

    case "LOW":
      return "info";

    default:
      return "info";
  }
};

const AlertsPage = () => {
  const [alerts, setAlerts] =
    useState<Alert[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {
    const loadAlerts = async () => {
      try {
        const data =
          await alertsService.getAlerts();

        setAlerts(data);
      } catch (err) {
        console.error(err);

        setError(
          "Failed to load alerts"
        );
      } finally {
        setLoading(false);
      }
    };

    loadAlerts();
  }, []);

  if (loading) {
    return (
      <Box
        sx={{
          display: "flex",
          justifyContent: "center",
          alignItems: "center",
          minHeight: "50vh",
        }}
      >
        <CircularProgress />
      </Box>
    );
  }

  if (error) {
    return (
      <MuiAlert severity="error">
        {error}
      </MuiAlert>
    );
  }

  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        Alerts
      </Typography>

      <Stack spacing={2}>
        {alerts.length === 0 ? (
          <MuiAlert severity="info">
            No alerts available.
          </MuiAlert>
        ) : (
          alerts.map((alert) => (
            <AlertCard
              key={alert.alertId}
              severity={mapSeverity(
                alert.severity
              )}
              title={alert.title}
              description={
                alert.message
              }
            />
          ))
        )}
      </Stack>
    </>
  );
};

export default AlertsPage;