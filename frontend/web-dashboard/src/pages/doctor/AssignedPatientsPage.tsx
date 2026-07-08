import { useState, useEffect } from "react";
import { Box } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";
import PageLoading from "../../components/common/PageLoading";
import PageError from "../../components/common/PageError";

import PatientSearchFilter from "../../components/doctor/PatientSearchFilter";
import PatientList from "../../components/doctor/PatientList";
import { doctorService } from "../../services/doctorService";
import type { DoctorAssignedPatient } from "../../types/doctor";

const AssignedPatientsPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [patients, setPatients] = useState<DoctorAssignedPatient[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  useEffect(() => {
    const fetchPatients = async () => {
      try {
        const data = await doctorService.getAssignedPatients();
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
