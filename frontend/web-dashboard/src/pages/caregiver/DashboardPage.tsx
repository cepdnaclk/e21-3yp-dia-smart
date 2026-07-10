import { useState, useEffect } from "react";
import { Grid, Box, CircularProgress, Alert } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import AssignedPatientsSummary from "../../components/caregiver/AssignedPatientsSummary";
import TodayAlerts from "../../components/caregiver/TodayAlerts";
import MissedDoses from "../../components/caregiver/MissedDoses";
import StorageWarnings from "../../components/caregiver/StorageWarnings";
import RecentActivity from "../../components/caregiver/RecentActivity";

import { caregiverService } from "../../services/caregiverService";
import type { Alert as VitalsAlert } from "../../types/alert";

export interface MissedDoseRecord {
  patientId: number;
  patientName: string;
  scheduleLabel: string;
  scheduledTime: string;
  doseUnits: number;
}

const DashboardPage = () => {
  const [alerts, setAlerts] = useState<VitalsAlert[]>([]);
  const [missedDoses, setMissedDoses] = useState<MissedDoseRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const loadData = async () => {
    setLoading(true);
    setError(null);
    try {
      const [patientsList, alertsList] = await Promise.all([
        caregiverService.getAssignedPatients(),
        caregiverService.getAlerts()
      ]);
      setAlerts(alertsList);

      // Fetch today's schedule adherence for each patient to collect missed doses
      const resolvedMissed: MissedDoseRecord[] = [];
      await Promise.all(
        patientsList.map(async (patient) => {
          try {
            const adherence = await caregiverService.getPatientTodayAdherence(patient.patientId);
            adherence.forEach((entry) => {
              if (entry.status === "MISSED") {
                resolvedMissed.push({
                  patientId: patient.patientId,
                  patientName: patient.patientName,
                  scheduleLabel: entry.scheduleLabel,
                  scheduledTime: entry.scheduledTime,
                  doseUnits: entry.doseUnits
                });
              }
            });
          } catch (e) {
            console.error(`Failed to load adherence for patient ${patient.patientId}`, e);
          }
        })
      );
      setMissedDoses(resolvedMissed);
    } catch (err: any) {
      console.error(err);
      setError("Failed to load caregiver dashboard metrics.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "60vh" }}>
        <CircularProgress />
      </Box>
    );
  }

  // Filter specific warning segments
  const activeAlerts = alerts.filter((a) => a.status === "OPEN");

  const storageWarningsList = alerts.filter(
    (a) =>
      a.status === "OPEN" &&
      (a.alertType === "TEMP_LOW" ||
        a.alertType === "TEMP_HIGH" ||
        a.alertType === "CRITICAL_INVENTORY" ||
        a.alertType === "LOW_INVENTORY")
  );

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Caregiver Dashboard</PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <AssignedPatientsSummary />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <RecentActivity alerts={alerts} />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <TodayAlerts alerts={activeAlerts} />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <MissedDoses missedDoses={missedDoses} />
        </Grid>

        <Grid size={{ xs: 12, md: 4 }}>
          <StorageWarnings warnings={storageWarningsList} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
