import { useEffect, useState } from "react";
import { Grid, Typography, Box, Button, Card, CardContent } from "@mui/material";
import { Link } from "react-router-dom";

import StatCard from "../../components/common/StatCard";
import PageTitle from "../../components/common/PageTitle";
import GlucoseChart from "../../components/charts/GlucoseChart";
import DoseHistoryChart from "../../components/charts/DoseHistoryChart";
import { dashboardService } from "../../services/dashboardService";
import { analyticsService } from "../../services/analyticsService";
import type { DashboardData } from "../../types/dashboard";
import type { DoseReading } from "../../types/analytics";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";
import { getPatientId } from "../../utils/patient";

const getSafePatientId = () => {
  try {
    return getPatientId();
  } catch {
    return null;
  }
};

const getRelativeTime = (timestamp?: string) => {
  if (!timestamp) return "No recent record";
  const date = new Date(timestamp);
  if (isNaN(date.getTime())) return "No recent record";

  const diffMs = Date.now() - date.getTime();
  if (diffMs < 0) return "Just now";

  const diffSecs = Math.floor(diffMs / 1000);
  const diffMins = Math.floor(diffSecs / 60);
  const diffHours = Math.floor(diffMins / 60);
  const diffDays = Math.floor(diffHours / 24);

  if (diffSecs < 60) return "Just now";
  if (diffMins < 60) return `${diffMins} min${diffMins > 1 ? "s" : ""} ago`;
  if (diffHours < 24) return `${diffHours} hr${diffHours > 1 ? "s" : ""} ago`;
  return `${diffDays} day${diffDays > 1 ? "s" : ""} ago`;
};

const getGlucoseStatus = (glucose: number, patientId: string | null) => {
  if (!glucose || glucose === 0) {
    return { text: "No Reading", color: "#64748b" };
  }
  let targetMin = 70;
  let targetMax = 180;
  if (patientId) {
    const cachedMin = localStorage.getItem(`diasmart_patient_${patientId}_target_min`);
    const cachedMax = localStorage.getItem(`diasmart_patient_${patientId}_target_max`);
    if (cachedMin && !isNaN(Number(cachedMin))) targetMin = Number(cachedMin);
    if (cachedMax && !isNaN(Number(cachedMax))) targetMax = Number(cachedMax);
  }

  if (glucose < targetMin) {
    return { text: "Low", color: "#ef4444" };
  }
  if (glucose > targetMax) {
    return { text: "High", color: "#ef4444" };
  }
  return { text: "In Range", color: "#10b981" };
};

const getInventoryStatus = (data: DashboardData) => {
  if (data.inventoryStatus === "REMOVED") {
    return { text: "Pen/Cartridge Removed", color: "#f59e0b" };
  }
  if (data.inventoryStatus === "EMPTY" || (data.inventory === 0 && data.estimatedRemainingPercent === undefined)) {
    return { text: "Cartridge Empty", color: "#ef4444" };
  }
  if (data.inventoryStatus === "CRITICAL") {
    return { text: "Critically Low", color: "#ef4444" };
  }
  if (data.estimatedRemainingPercent !== undefined && data.estimatedRemainingPercent !== null) {
    const pct = Math.round(data.estimatedRemainingPercent);
    if (pct <= 20) {
      return { text: `Low (${pct}% Left)`, color: "#f59e0b" };
    }
    return { text: `${pct}% Remaining`, color: "#10b981" };
  }
  if (data.inventoryStatus === "LOW") {
    return { text: "Low Supply", color: "#f59e0b" };
  }
  return { text: "Adequate Level", color: "#10b981" };
};

const getTemperatureStatus = (data: DashboardData) => {
  if (data.temperature === 0 && !data.temperatureStatus) {
    return { text: "No Sensor Data", color: "#64748b" };
  }
  if (data.temperatureStatus === "LOW" || data.temperature < 2.0) {
    return { text: "Too Cold (< 2°C)", color: "#3ec1fa" };
  }
  if (data.temperatureStatus === "HIGH" || data.temperature > 8.0) {
    return { text: "Too Warm (> 8°C)", color: "#ef4444" };
  }
  return { text: "Normal Range (2–8°C)", color: "#10b981" };
};

const getLastDoseStatus = (data: DashboardData) => {
  if (!data.lastDoseInjectedAt || data.lastDose === 0) {
    return { text: "No Recent Dose", color: "#64748b" };
  }
  return { text: getRelativeTime(data.lastDoseInjectedAt), color: "#64748b" };
};

const DashboardPage = () => {
  const [dashboardData, setDashboardData] = useState<DashboardData | null>(null);
  const [glucoseHistory, setGlucoseHistory] = useState<{ date: string; glucose: number }[]>([]);
  const [doseHistory, setDoseHistory] = useState<DoseReading[]>([]);

  const loadDashboard = async () => {
    try {
      const data = await dashboardService.getDashboardData();
      setDashboardData(data);
    } catch (err) {
      console.error("Failed to refresh dashboard:", err);
    }
  };

  useEffect(() => {
    const loadChartData = async () => {
      try {
        const [glucoseData, doseData] = await Promise.all([
          analyticsService.getGlucoseHistory(),
          analyticsService.getDoseHistory()
        ]);

        setGlucoseHistory(
          (glucoseData || []).map((item) => ({
            date: new Date(item.measuredAt).toLocaleDateString(),
            glucose: item.glucoseValueMgDl,
          })).reverse()
        );

        setDoseHistory(doseData || []);
      } catch (err) {
        console.error("Failed to load chart data:", err);
      }
    };

    // Initial load
    loadDashboard();
    loadChartData();
  }, []);

  // Auto-refresh stats every 2 seconds
  useAutoRefresh(loadDashboard, 2000);

  if (!dashboardData) {
    return <Typography sx={{ p: 4 }}>Loading...</Typography>;
  }

  const patientId = getSafePatientId();
  const glucoseStatus = getGlucoseStatus(dashboardData.glucose, patientId);
  const inventoryStatus = getInventoryStatus(dashboardData);
  const tempStatus = getTemperatureStatus(dashboardData);
  const doseStatus = getLastDoseStatus(dashboardData);

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Dashboard</PageTitle>

      {/* Main Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Glucose"
            value={`${dashboardData.glucose} mg/dL`}
            statusText={glucoseStatus.text}
            statusColor={glucoseStatus.color}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Inventory"
            value={`${dashboardData.inventory} g`}
            statusText={inventoryStatus.text}
            statusColor={inventoryStatus.color}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Temperature"
            value={`${dashboardData.temperature} °C`}
            statusText={tempStatus.text}
            statusColor={tempStatus.color}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Last Dose"
            value={`${dashboardData.lastDose} Units`}
            statusText={doseStatus.text}
            statusColor={doseStatus.color}
          />
        </Grid>
      </Grid>

      {/* Simple Daily Telemetry Charts */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, md: 6 }}>
          <Card elevation={2} sx={{ borderRadius: 3 }}>
            <CardContent>
              <Typography variant="subtitle1" sx={{ fontWeight: "bold", mb: 2, color: "#12233b" }}>
                Glucose Trends
              </Typography>
              {glucoseHistory.length === 0 ? (
                <Typography color="text.secondary" sx={{ py: 4, textAlign: "center" }}>
                  No glucose trend records available.
                </Typography>
              ) : (
                <GlucoseChart data={glucoseHistory} />
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Card elevation={2} sx={{ borderRadius: 3 }}>
            <CardContent>
              <Typography variant="subtitle1" sx={{ fontWeight: "bold", mb: 2, color: "#12233b" }}>
                Insulin Dosage History
              </Typography>
              {doseHistory.length === 0 ? (
                <Typography color="text.secondary" sx={{ py: 4, textAlign: "center" }}>
                  No insulin dose events recorded.
                </Typography>
              ) : (
                <DoseHistoryChart data={doseHistory} />
              )}
            </CardContent>
          </Card>
        </Grid>
      </Grid>

      {/* Call To Action Banner for Advanced Analytics */}
      <Grid container>
        <Grid size={{ xs: 12 }}>
          <Box
            sx={{
              p: 3,
              bgcolor: "#f1f5f9",
              borderRadius: 4,
              border: "1px solid #e2e8f0",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              flexWrap: "wrap",
              gap: 2
            }}
          >
            <Box>
              <Typography variant="subtitle1" sx={{ fontWeight: "bold", color: "#12233b" }}>
                Need deeper insights?
              </Typography>
              <Typography variant="body2" color="text.secondary">
                Configure personalized targets, calculate Time in Range (TIR), and analyze dose-glucose correlations.
              </Typography>
            </Box>
            <Button
              component={Link}
              to="/analytics"
              variant="contained"
              color="primary"
              sx={{ textTransform: "none", borderRadius: 2, fontWeight: "bold" }}
            >
              View Advanced Analytics
            </Button>
          </Box>
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
