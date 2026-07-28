import { useState, useEffect } from "react";
import { Grid, Box, CircularProgress, Alert } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";
import PatientDoctorSection from "../../components/admin/PatientDoctorSection";
import PatientCaregiverSection from "../../components/admin/PatientCaregiverSection";

import { adminService } from "../../services/adminService";
import type { AdminUserRecord } from "../../types/admin";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const AssignmentsPage = () => {
  const [doctors, setDoctors] = useState<AdminUserRecord[]>([]);
  const [caregivers, setCaregivers] = useState<AdminUserRecord[]>([]);
  const [patients, setPatients] = useState<AdminUserRecord[]>([]);
  const [patientIdMap, setPatientIdMap] = useState<Record<number, AdminUserRecord>>({});
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    try {
      const allUsers = await adminService.getAllUsers();
      
      const docsList = allUsers.filter((u) => u.role === "DOCTOR" && u.active);
      const cgList = allUsers.filter((u) => u.role === "CAREGIVER" && u.active);
      const patList = allUsers.filter((u) => u.role === "PATIENT" && u.active);

      setDoctors(docsList);
      setCaregivers(cgList);
      setPatients(patList);

      // Resolve database patientId for each Patient User
      const resolvedMappings: Record<number, AdminUserRecord> = {};
      await Promise.all(
        patList.map(async (p) => {
          try {
            const accesses = await adminService.getPatientAccessForUser(p.userId);
            const selfAccess = accesses.find((a) => a.accessRole === "SELF");
            if (selfAccess) {
              resolvedMappings[selfAccess.patientId] = p;
            }
          } catch (e) {
            console.error(`Failed to resolve patientId mapping for userId ${p.userId}`, e);
          }
        })
      );

      setPatientIdMap(resolvedMappings);
    } catch (err: any) {
      console.error(err);
      if (!silent) {
        setError("Failed to retrieve user directory or mapping logs.");
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    loadData(false);
  }, []);

  useAutoRefresh(() => loadData(true), 5000);

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "60vh" }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Care Team Assignments</PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <PatientDoctorSection
            doctors={doctors}
            patients={patients}
            patientIdMap={patientIdMap}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <PatientCaregiverSection
            caregivers={caregivers}
            patients={patients}
            patientIdMap={patientIdMap}
          />
        </Grid>
      </Grid>
    </Box>
  );
};

export default AssignmentsPage;
