import { Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import PatientSearchFilter from "../../components/caregiver/PatientSearchFilter";
import PatientList from "../../components/caregiver/PatientList";

const AssignedPatientsPage = () => {
  // TODO: Fetch caregiver assigned patients from backend API during feature integration

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Assigned Patients</PageTitle>

      <PatientSearchFilter />

      <PatientList />
    </Box>
  );
};

export default AssignedPatientsPage;
