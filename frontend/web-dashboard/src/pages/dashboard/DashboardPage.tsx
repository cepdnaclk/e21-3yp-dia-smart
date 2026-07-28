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

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Dashboard</PageTitle>

      {/* Main Stats Cards */}
      <Grid container spacing={3} sx={{ mb: 4 }}>
        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Glucose"
            value={`${dashboardData.glucose} mg/dL`}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Inventory"
            value={`${dashboardData.inventory} g`}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Temperature"
            value={`${dashboardData.temperature} °C`}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Last Dose"
            value={`${dashboardData.lastDose} Units`}
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
