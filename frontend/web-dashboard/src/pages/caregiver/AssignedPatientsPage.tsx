import { useState, useEffect } from "react";
import { Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";
import PageLoading from "../../components/common/PageLoading";
import PageError from "../../components/common/PageError";

import PatientSearchFilter from "../../components/caregiver/PatientSearchFilter";
import PatientList from "../../components/caregiver/PatientList";
import { caregiverService } from "../../services/caregiverService";
import type { CaregiverAssignedPatient } from "../../types/caregiver";

const AssignedPatientsPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [patients, setPatients] = useState<CaregiverAssignedPatient[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    const fetchPatients = async () => {
      try {
        const data = await caregiverService.getAssignedPatients();
        setPatients(data);
      } catch (err: any) {
        setError("Failed to load assigned patients. Please try again.");
      } finally {
        setLoading(false);
      }
    };
    fetchPatients();
  }, []);

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  const filteredPatients = patients.filter((p) =>
    p.patientName.toLowerCase().includes(searchQuery.toLowerCase())
  );

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Assigned Patients</PageTitle>

      <PatientSearchFilter query={searchQuery} onQueryChange={setSearchQuery} />

      <PatientList patients={filteredPatients} />
    </Box>
  );
};

export default AssignedPatientsPage;
