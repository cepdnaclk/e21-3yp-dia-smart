import { useEffect, useState } from "react";

import {
  Typography,
  Stack,
} from "@mui/material";

import AlertCard from "../../components/alerts/AlertCard";

import { alertsService } from "../../services/alertsService";
import type { Alert } from "../../types/alert";

const AlertsPage = () => {
  const [alerts, setAlerts] =
    useState<Alert[]>([]);

  useEffect(() => {
    const loadAlerts = async () => {
      const data =
        await alertsService.getAlerts();

      setAlerts(data);
    };

    loadAlerts();
  }, []);

  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        Alerts
      </Typography>

      <Stack spacing={2}>
        {alerts.map((alert) => (
          <AlertCard
            key={alert.id}
            severity={alert.severity}
            title={alert.title}
            description={alert.description}
          />
        ))}
      </Stack>
    </>
  );
};

export default AlertsPage;