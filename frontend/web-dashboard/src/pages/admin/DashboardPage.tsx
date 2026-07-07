import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import TotalUsersCard from "../../components/admin/TotalUsersCard";
import RegisteredDevicesCard from "../../components/admin/RegisteredDevicesCard";
import ActivePatientsCard from "../../components/admin/ActivePatientsCard";
import SystemStatusCard from "../../components/admin/SystemStatusCard";
import RecentActivityCard from "../../components/admin/RecentActivityCard";

const DashboardPage = () => {
  // TODO: Fetch dashboard status metrics via adminService

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Admin Dashboard</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <TotalUsersCard />
        </Grid>
        
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <RegisteredDevicesCard />
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <ActivePatientsCard />
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <SystemStatusCard />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <RecentActivityCard />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
