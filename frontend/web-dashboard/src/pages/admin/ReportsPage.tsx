import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import UserReportsSection from "../../components/admin/UserReportsSection";
import DeviceReportsSection from "../../components/admin/DeviceReportsSection";
import UsageReportsSection from "../../components/admin/UsageReportsSection";

const ReportsPage = () => {
  // TODO: Fetch admin analytics and reports generation templates via adminService

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>System & Administrative Reports</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <UserReportsSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <DeviceReportsSection />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <UsageReportsSection />
        </Grid>
      </Grid>
    </Box>
  );
};

export default ReportsPage;
