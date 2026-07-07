import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import PatientsSection from "../../components/admin/PatientsSection";
import DoctorsSection from "../../components/admin/DoctorsSection";
import CaregiversSection from "../../components/admin/CaregiversSection";
import AdministratorsSection from "../../components/admin/AdministratorsSection";

const UsersPage = () => {
  // TODO: Fetch administrative user catalog data from adminService

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>User Management</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <PatientsSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <DoctorsSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <CaregiversSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <AdministratorsSection />
        </Grid>
      </Grid>
    </Box>
  );
};

export default UsersPage;
