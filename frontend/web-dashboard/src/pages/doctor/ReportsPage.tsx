import { useState, useEffect } from "react";
import {
  Grid,
  Box,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  TextField,
  CircularProgress,
  Alert,
  Card,
  CardContent
} from "@mui/material";

import PageTitle from "../../components/common/PageTitle";
import PatientReports from "../../components/doctor/PatientReports";
import AdherenceReports from "../../components/doctor/AdherenceReports";
import ExportReports from "../../components/doctor/ExportReports";

import { doctorService } from "../../services/doctorService";
import type { DoctorAssignedPatient } from "../../types/doctor";
import type { AdherenceAnalyticsResponse } from "../../types/analytics";

const ReportsPage = () => {
  const [patients, setPatients] = useState<DoctorAssignedPatient[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<number | "">("");
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(d.getDate() - 14); // default to 14 days ago
    return d.toISOString().split("T")[0];
  });
  const [endDate, setEndDate] = useState(() => {
    return new Date().toISOString().split("T")[0];
  });

  const [adherenceData, setAdherenceData] = useState<AdherenceAnalyticsResponse | null>(null);
  const [loadingList, setLoadingList] = useState(true);
  const [loadingData, setLoadingData] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const loadPatients = async () => {
      try {
        const list = await doctorService.getAssignedPatients();
        setPatients(list);
        if (list.length > 0) {
          setSelectedPatientId(list[0].patientId);
        }
      } catch (err: any) {
        console.error(err);
        setError("Failed to retrieve assigned patient cohort list.");
      } finally {
        setLoadingList(false);
      }
    };
    loadPatients();
  }, []);

  useEffect(() => {
    const fetchAnalytics = async () => {
      if (!selectedPatientId) return;
      setLoadingData(true);
      setError(null);
      try {
        const data = await doctorService.getAdherenceAnalytics(
          Number(selectedPatientId),
          startDate,
          endDate
        );
        setAdherenceData(data);
      } catch (err: any) {
        console.error(err);
        setError("Failed to load compliance records for selected patient.");
      } finally {
        setLoadingData(false);
      }
    };

    fetchAnalytics();
  }, [selectedPatientId, startDate, endDate]);

  if (loadingList) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Reports & Analytics</PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      {/* Patient Selection & Filter Controls */}
      <Card elevation={2} sx={{ mb: 3, borderRadius: 3 }}>
        <CardContent sx={{ display: "flex", flexWrap: "wrap", gap: 3, alignItems: "center" }}>
          <FormControl sx={{ minWidth: 240 }} size="small">
            <InputLabel id="reports-patient-select-label">Select Patient</InputLabel>
            <Select
              labelId="reports-patient-select-label"
              label="Select Patient"
              value={selectedPatientId}
              onChange={(e) => setSelectedPatientId(e.target.value as number | "")}
            >
              {patients.map((p) => (
                <MenuItem key={p.patientId} value={p.patientId}>
                  {p.patientName} (ID: {p.patientId})
                </MenuItem>
              ))}
            </Select>
          </FormControl>

          <TextField
            label="Start Date"
            type="date"
            size="small"
            slotProps={{ inputLabel: { shrink: true } }}
            value={startDate}
            onChange={(e) => setStartDate(e.target.value)}
          />

          <TextField
            label="End Date"
            type="date"
            size="small"
            slotProps={{ inputLabel: { shrink: true } }}
            value={endDate}
            onChange={(e) => setEndDate(e.target.value)}
          />
        </CardContent>
      </Card>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 7 }}>
          <PatientReports />
        </Grid>

        <Grid size={{ xs: 12, md: 5 }}>
          <ExportReports />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <AdherenceReports adherenceData={adherenceData} loading={loadingData} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default ReportsPage;
