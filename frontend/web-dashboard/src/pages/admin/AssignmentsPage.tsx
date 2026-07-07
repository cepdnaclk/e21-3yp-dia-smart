import { Grid, Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import PatientDoctorSection from "../../components/admin/PatientDoctorSection";
import PatientCaregiverSection from "../../components/admin/PatientCaregiverSection";
import DevicePatientSection from "../../components/admin/DevicePatientSection";

const AssignmentsPage = () => {
  // TODO: Fetch caregiver mappings, clinician mappings, and provisioning data from adminService

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Care Team & Device Mappings</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <PatientDoctorSection />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <PatientCaregiverSection />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <DevicePatientSection />
        </Grid>
      </Grid>
    </Box>
  );
};

export default AssignmentsPage;
