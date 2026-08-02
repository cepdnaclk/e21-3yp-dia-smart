import { useState, useEffect } from "react";
import { Grid, Box, CircularProgress, Alert } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import OverviewStats from "../../components/doctor/OverviewStats";
import AssignedPatientsSummary from "../../components/doctor/AssignedPatientsSummary";
import CriticalAlerts from "../../components/doctor/CriticalAlerts";
import RecentActivity from "../../components/doctor/RecentActivity";

import { doctorService } from "../../services/doctorService";
import type { Alert as VitalsAlert } from "../../types/alert";
import type { DoctorAssignedPatient } from "../../types/doctor";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const DashboardPage = () => {
  const [patientsCount, setPatientsCount] = useState<number>(0);
  const [patients, setPatients] = useState<DoctorAssignedPatient[]>([]);
  const [alerts, setAlerts] = useState<VitalsAlert[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async (silent = false) => {
    if (!silent) {
      setLoading(true);
      setError(null);
    }
    try {
      const [patientsList, alertsList] = await Promise.all([
        doctorService.getAssignedPatients(),
        doctorService.getAlerts()
      ]);
      setPatientsCount(patientsList.length);
      setPatients(patientsList);
      setAlerts(alertsList);
    } catch (err: any) {
      console.error(err);
      if (!silent) {
        setError("Failed to retrieve clinician dashboard overview stats.");
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

  const openAlertsCount = alerts.filter((a) => a.status === "OPEN").length;

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Doctor Dashboard</PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Box sx={{ mb: 4 }}>
        <OverviewStats patientsCount={patientsCount} openAlertsCount={openAlertsCount} />
      </Box>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <AssignedPatientsSummary />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RecentActivity />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <CriticalAlerts alerts={alerts} patients={patients} onRefresh={loadData} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
