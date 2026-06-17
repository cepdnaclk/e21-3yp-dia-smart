import { useEffect, useState } from "react";

import { Grid, Typography } from "@mui/material";

import StatCard from "../../components/dashboard/StatCard";

import type { DashboardData } from "../../types/dashboard";
import { dashboardService } from "../../services/dashboardService";

const DashboardPage = () => {
  const [dashboardData, setDashboardData] =
    useState<DashboardData | null>(null);

  useEffect(() => {
    const loadDashboard = async () => {
      const data =
        await dashboardService.getDashboardData();

      setDashboardData(data);
    };

    loadDashboard();
  }, []);

  if (!dashboardData) {
    return <Typography>Loading...</Typography>;
  }

  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        Dashboard
      </Typography>

      <Grid container spacing={3}>
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
    </>
  );
};

export default DashboardPage;