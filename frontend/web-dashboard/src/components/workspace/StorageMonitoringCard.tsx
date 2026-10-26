import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress, Alert } from "@mui/material";
import ThermostatIcon from "@mui/icons-material/Thermostat";
import WarningAmberIcon from "@mui/icons-material/WarningAmber";
import { alpha } from "@mui/material/styles";
import AcUnitOutlinedIcon from "@mui/icons-material/AcUnitOutlined";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

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
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <CardHeading title="Storage Temperature Telemetry" icon={<AcUnitOutlinedIcon />} color="#0ea5e9" />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4, flexGrow: 1, alignItems: "center" }}>
            <CircularProgress size={28} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : !reading ? (
          <EmptyState message="No storage device telemetry received from patient." icon={<ThermostatIcon />} />
        ) : (
          <Box sx={{ flexGrow: 1, display: "flex", flexDirection: "column", justifyContent: "center", gap: 2 }}>
            <Box
              sx={{
                display: "flex",
                alignItems: "center",
                gap: 2,
                flexWrap: "wrap",
                p: 2,
                borderRadius: 2.5,
                border: "1px solid",
                borderColor: "divider",
                bgcolor: "#f8fafc",
              }}
            >
              <Box
                sx={{
                  width: 60,
                  height: 60,
                  flexShrink: 0,
                  borderRadius: "50%",
                  bgcolor: alpha(isSafe ? "#10b981" : "#ef4444", 0.12),
                  color: isSafe ? "#047857" : "#b91c1c",
                  display: "flex",
                  alignItems: "center",
                  justifyContent: "center"
                }}
              >
                {isSafe ? <ThermostatIcon fontSize="large" /> : <WarningAmberIcon fontSize="large" />}
              </Box>
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="h4" sx={{ fontWeight: 800, color: "#12233b", lineHeight: 1.15 }}>
                  {reading.temperatureC !== null && reading.temperatureC !== undefined ? reading.temperatureC.toFixed(1) : "—"} °C
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
