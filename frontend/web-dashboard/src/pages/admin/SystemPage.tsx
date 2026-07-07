import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import AuditLogsSection from "../../components/admin/AuditLogsSection";
import SystemHealthSection from "../../components/admin/SystemHealthSection";

const SystemPage = () => {
  // TODO: Fetch system status telemetry and audit log history via adminService

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>System Telemetry & Audit Logs</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <AuditLogsSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <SystemHealthSection />
        </Grid>
      </Grid>
    </Box>
  );
};

export default SystemPage;
