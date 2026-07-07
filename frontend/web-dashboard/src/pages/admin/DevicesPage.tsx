import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import DevicesListSection from "../../components/admin/DevicesListSection";
import DeviceAssignmentSection from "../../components/admin/DeviceAssignmentSection";
import DeviceHealthSection from "../../components/admin/DeviceHealthSection";

const DevicesPage = () => {
  // TODO: Fetch hardware monitoring telemetry and registrations via adminService

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Device Registry</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12 }}>
          <DevicesListSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <DeviceAssignmentSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <DeviceHealthSection />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DevicesPage;
