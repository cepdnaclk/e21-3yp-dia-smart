import { Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import PatientSearchFilter from "../../components/doctor/PatientSearchFilter";
import PatientList from "../../components/doctor/PatientList";

const AssignedPatientsPage = () => {
  // TODO: Fetch assigned patients from backend API during feature integration

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Assigned Patients</PageTitle>

      <PatientSearchFilter />

      <PatientList />
    </Box>
  );
};

export default AssignedPatientsPage;
