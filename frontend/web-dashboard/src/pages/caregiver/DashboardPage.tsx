import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import AssignedPatientsSummary from "../../components/caregiver/AssignedPatientsSummary";
import TodayAlerts from "../../components/caregiver/TodayAlerts";
import MissedDoses from "../../components/caregiver/MissedDoses";
import StorageWarnings from "../../components/caregiver/StorageWarnings";
import RecentActivity from "../../components/caregiver/RecentActivity";

const DashboardPage = () => {
  // TODO: Integrate caregiver dashboard metrics via caregiverService during feature implementation.

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Caregiver Dashboard</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <AssignedPatientsSummary />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RecentActivity />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <TodayAlerts />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <MissedDoses />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <StorageWarnings />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
