import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import OverviewStats from "../../components/doctor/OverviewStats";
import AssignedPatientsSummary from "../../components/doctor/AssignedPatientsSummary";
import CriticalAlerts from "../../components/doctor/CriticalAlerts";
import RecentActivity from "../../components/doctor/RecentActivity";

const DashboardPage = () => {
  // TODO: Integrate Doctor Dashboard APIs via doctorService during feature implementation.

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Doctor Dashboard</PageTitle>

      <Box sx={{ mb: 4 }}>
        <OverviewStats />
      </Box>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <AssignedPatientsSummary />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RecentActivity />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <CriticalAlerts />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
