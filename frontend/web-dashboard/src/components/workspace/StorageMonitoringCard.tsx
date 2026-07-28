import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress, Alert } from "@mui/material";
import ThermostatIcon from "@mui/icons-material/Thermostat";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";

import { caregiverService } from "../../services/caregiverService";

interface StorageMonitoringCardProps {
  patientId: number;
  refreshTrigger?: number;
}

const StorageMonitoringCard = ({ patientId, refreshTrigger }: StorageMonitoringCardProps) => {
  const [loading, setLoading] = useState(true);
  const [reading, setReading] = useState<any>(null);

  useEffect(() => {
    if (!patientId) return;
    const fetchStorage = async (silent = false) => {
      try {
        if (!silent) setLoading(true);
        const data = await caregiverService.getLatestStorageReading(patientId);
        setReading(data);
      } catch (err) {
        console.error("Failed to load storage readings", err);
      } finally {
        if (!silent) setLoading(false);
      }
    };

    const isInitial = !refreshTrigger || refreshTrigger === 0;
    fetchStorage(!isInitial);
  }, [patientId, refreshTrigger]);

  const isSafe = reading ? reading.temperatureC >= 2.0 && reading.temperatureC <= 8.0 : true;

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Storage Temperature Telemetry
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4, flexGrow: 1, alignItems: "center" }}>
            <CircularProgress size={32} />
          </Box>
        ) : !reading ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 2 }}>
            No storage device telemetry received from patient.
          </Typography>
        ) : (
          <Box sx={{ flexGrow: 1, display: "flex", flexDirection: "column", justifyContent: "center", gap: 2 }}>
            <Box sx={{ display: "flex", alignItems: "center", gap: 2 }}>
              <Box
                sx={{
                  p: 2,
                  borderRadius: "50%",
                  bgcolor: isSafe ? "success.light" : "error.light",
                  color: isSafe ? "success.dark" : "error.dark",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center"
                }}
              >
                {isSafe ? <ThermostatIcon fontSize="large" /> : <WarningAmberIcon fontSize="large" />}
              </Box>
              <Box>
                <Typography variant="h4" sx={{ fontWeight: "bold" }}>
                  {reading.temperatureC.toFixed(1)} °C
                </Typography>
                <Typography variant="body2" color="text.secondary">
                  Last updated: {new Date(reading.measuredAt || Date.now()).toLocaleTimeString()}
                </Typography>
              </Box>
            </Box>

            <Alert severity={isSafe ? "success" : "error"} sx={{ borderRadius: 2 }}>
              {isSafe
                ? `Safe Range: Storage compartment is within safe limits (2°C - 8°C). Status is ${reading.temperatureStatus || "NORMAL"}.`
                : `Anomaly Warning: Current temperature is outside boundaries! Status is ${reading.temperatureStatus || "CRITICAL"}.`}
            </Alert>
          </Box>
        )}
      </CardContent>
    </Card>
  );
};

export default StorageMonitoringCard;
