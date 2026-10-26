import { useEffect, useState } from "react";
import { Grid, Typography, Box, Button, Card, CardContent, CircularProgress } from "@mui/material";
import ShowChartIcon from "@mui/icons-material/ShowChart";
import VaccinesIcon from "@mui/icons-material/Vaccines";
import InsightsIcon from "@mui/icons-material/Insights";
import ArrowForwardIcon from "@mui/icons-material/ArrowForward";
import { Link } from "react-router-dom";

import StatCard from "../../components/common/StatCard";
import PageTitle from "../../components/common/PageTitle";
import CardHeading from "../../components/common/CardHeading";
import EmptyState from "../../components/common/EmptyState";
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

const EmptyChartState = ({ message }: { message: string }) => (
  <EmptyState message={message} minHeight={300} />
);

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
    return (
      <Box sx={{ display: "flex", alignItems: "center", justifyContent: "center", gap: 1.5, minHeight: "40vh", p: 4 }}>
        <CircularProgress size={22} thickness={5} sx={{ color: "secondary.dark" }} />
        <Typography color="text.secondary" sx={{ fontWeight: 500 }}>Loading...</Typography>
      </Box>
    );
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
          <Card sx={{ height: "100%" }}>
            <CardContent>
              <CardHeading
                title="Glucose Trends"
                subtitle="Recorded blood glucose readings (mg/dL)"
                icon={<ShowChartIcon />}
                color="#ef4444"
              />
              {glucoseHistory.length === 0 ? (
                <EmptyChartState message="No glucose trend records available." />
              ) : (
                <GlucoseChart data={glucoseHistory} />
              )}
            </CardContent>
          </Card>
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <Card sx={{ height: "100%" }}>
            <CardContent>
              <CardHeading
                title="Insulin Dosage History"
                subtitle="Injected insulin doses (units)"
                icon={<VaccinesIcon />}
                color="#0284c7"
              />
              {doseHistory.length === 0 ? (
                <EmptyChartState message="No insulin dose events recorded." />
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
              p: { xs: 2.5, sm: 3.5 },
              borderRadius: 4,
              position: "relative",
              overflow: "hidden",
              color: "#ffffff",
              background: "linear-gradient(120deg, #12233b 0%, #1b3559 60%, #0b4a6f 100%)",
              display: "flex",
              justifyContent: "space-between",
              alignItems: "center",
              flexWrap: "wrap",
              gap: 2.5
            }}
          >
            <Box
              aria-hidden
              sx={{
                position: "absolute",
                right: -60,
                top: -80,
                width: 260,
                height: 260,
                borderRadius: "50%",
                background: "radial-gradient(circle, rgba(62,193,250,0.28) 0%, transparent 70%)",
                pointerEvents: "none",
              }}
            />
            <Box sx={{ display: "flex", alignItems: "center", gap: 2, position: "relative", minWidth: 0, flex: "1 1 320px" }}>
              <Box
                aria-hidden
                sx={{
                  width: 48,
                  height: 48,
                  borderRadius: 3,
                  flexShrink: 0,
                  display: { xs: "none", sm: "flex" },
                  alignItems: "center",
                  justifyContent: "center",
                  bgcolor: "rgba(62,193,250,0.16)",
                  color: "#3ec1fa",
                }}
              >
                <InsightsIcon />
              </Box>
              <Box sx={{ minWidth: 0 }}>
                <Typography variant="subtitle1" sx={{ fontWeight: 800, color: "#ffffff" }}>
                  Need deeper insights?
                </Typography>
                <Typography variant="body2" sx={{ color: "rgba(255,255,255,0.75)" }}>
                  Configure personalized targets, calculate Time in Range (TIR), and analyze dose-glucose correlations.
                </Typography>
              </Box>
            </Box>
            <Button
              component={Link}
              to="/analytics"
              variant="contained"
              endIcon={<ArrowForwardIcon />}
              sx={{
                position: "relative",
                textTransform: "none",
                borderRadius: 2.5,
                fontWeight: 700,
                px: 2.5,
                py: 1.1,
                bgcolor: "#3ec1fa",
                color: "#12233b",
                "&:hover": { bgcolor: "#7dd6fc" },
              }}
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
