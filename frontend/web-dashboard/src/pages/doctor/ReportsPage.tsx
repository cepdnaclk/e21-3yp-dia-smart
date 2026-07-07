import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import PatientReports from "../../components/doctor/PatientReports";
import AdherenceReports from "../../components/doctor/AdherenceReports";
import ExportReports from "../../components/doctor/ExportReports";

const ReportsPage = () => {
  // TODO: Integrate doctor reports and exports endpoints during feature integration

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Reports & Analytics</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <PatientReports />
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <ExportReports />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <AdherenceReports />
        </Grid>
      </Grid>
    </Box>
  );
};

export default ReportsPage;
