import { useState, useEffect } from "react";
import {
  Box,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControlLabel,
  Checkbox,
  Stack,
  Alert
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";

import PageTitle from "../../components/common/PageTitle";
import PageLoading from "../../components/common/PageLoading";
import PageError from "../../components/common/PageError";

import PatientSearchFilter from "../../components/doctor/PatientSearchFilter";
import PatientList from "../../components/doctor/PatientList";
import PendingRequestsCard from "../../components/doctor/PendingRequestsCard";
import { doctorService } from "../../services/doctorService";
import { useAuth } from "../../context/AuthContext";
import type { DoctorAssignedPatient } from "../../types/doctor";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const AssignedPatientsPage = () => {
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [patients, setPatients] = useState<DoctorAssignedPatient[]>([]);
  const [searchQuery, setSearchQuery] = useState("");

  // Self-assignment Dialog states
  const [dialogOpen, setDialogOpen] = useState(false);
  const [assignPatientId, setAssignPatientId] = useState("");
  const [assignLabel, setAssignLabel] = useState("Primary Physician");
  const [canAckAlerts, setCanAckAlerts] = useState(true);
  const [canEditPrescriptions, setCanEditPrescriptions] = useState(true);
  const [submitLoading, setSubmitLoading] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [submitSuccess, setSubmitSuccess] = useState("");

  const { userId } = useAuth();

  const fetchPatients = async (silent = false) => {
    try {
      if (!silent) {
        setLoading(true);
        setError("");
      }
      const data = await doctorService.getAssignedPatients();
      setPatients(data);
    } catch (err: any) {
      if (!silent) {
        setError("Failed to load assigned patients. Please try again.");
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    fetchPatients(false);
  }, []);

  useAutoRefresh(() => fetchPatients(true), 5000);

  const handleAssignPatientSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!userId) {
      setSubmitError("Doctor session information not found. Please log in again.");
      return;
    }

    const patientIdNum = Number(assignPatientId);
    if (isNaN(patientIdNum) || patientIdNum <= 0) {
      setSubmitError("Please enter a valid Patient ID.");
      return;
    }

    try {
      setSubmitLoading(true);
      setSubmitError("");
      setSubmitSuccess("");

      await doctorService.assignPatient({
        userId,
        patientId: patientIdNum,
        accessRole: "DOCTOR",
        relationshipLabel: assignLabel,
        canView: true,
        canAcknowledgeAlerts: canAckAlerts,
        canEditPrescriptions: canEditPrescriptions,
      });

      setSubmitSuccess("Patient assigned successfully!");
      setAssignPatientId("");
      setAssignLabel("Primary Physician");
      setCanAckAlerts(true);
      setCanEditPrescriptions(true);
      
      // Refresh patient list
      await fetchPatients();

      setTimeout(() => {
        setDialogOpen(false);
        setSubmitSuccess("");
      }, 1500);
    } catch (err: any) {
      setSubmitError(
        err.response?.data?.message ??
        "Failed to assign patient. Please verify the Patient ID is correct."
      );
    } finally {
      setSubmitLoading(false);
    }
  };

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
      {/* Title Header with Action Button */}
      <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 3, flexWrap: "wrap", gap: 2 }}>
        <PageTitle>Assigned Patients</PageTitle>
        <Button
          variant="contained"
          startIcon={<AddIcon />}
          onClick={() => setDialogOpen(true)}
          sx={{
            textTransform: "none",
            borderRadius: 3,
            backgroundColor: "#12233b",
            color: "#ffffff",
            fontWeight: 700,
            py: 1,
            px: 3,
            "&:hover": { backgroundColor: "#1b3559" }
          }}
        >
          Assign New Patient
        </Button>
      </Box>

      <PendingRequestsCard onRefresh={fetchPatients} />

      <PatientSearchFilter query={searchQuery} onQueryChange={setSearchQuery} />

      <PatientList patients={filteredPatients} />

      {/* Assign Patient Modal Dialog */}
      <Dialog
        open={dialogOpen}
        onClose={() => !submitLoading && setDialogOpen(false)}
        maxWidth="xs"
        fullWidth
        slotProps={{
          paper: {
            sx: { borderRadius: 4, p: 1 }
          }
        }}
      >
        <form onSubmit={handleAssignPatientSubmit}>
          <DialogTitle sx={{ fontWeight: "bold", pb: 1 }}>
            Assign New Patient
          </DialogTitle>
          <DialogContent>
            <Stack spacing={2.5} sx={{ mt: 1 }}>
              {submitError && <Alert severity="error">{submitError}</Alert>}
              {submitSuccess && <Alert severity="success">{submitSuccess}</Alert>}

              <TextField
                autoFocus
                required
                label="Patient ID"
                placeholder="Enter numeric patient ID (e.g. 1)"
                value={assignPatientId}
                onChange={(e) => setAssignPatientId(e.target.value)}
                disabled={submitLoading}
                fullWidth
              />

              <TextField
                required
                label="Relationship Label"
                placeholder="e.g. Primary Physician"
                value={assignLabel}
                onChange={(e) => setAssignLabel(e.target.value)}
                disabled={submitLoading}
                fullWidth
              />

              <Stack spacing={0.5}>
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={canAckAlerts}
                      onChange={(e) => setCanAckAlerts(e.target.checked)}
                      disabled={submitLoading}
                      color="primary"
                    />
                  }
                  label="Can Acknowledge Alerts"
                />
                <FormControlLabel
                  control={
                    <Checkbox
                      checked={canEditPrescriptions}
                      onChange={(e) => setCanEditPrescriptions(e.target.checked)}
                      disabled={submitLoading}
                      color="primary"
                    />
                  }
                  label="Can Edit Prescriptions"
                />
              </Stack>
            </Stack>
          </DialogContent>
          <DialogActions sx={{ px: 3, pb: 2 }}>
            <Button
              onClick={() => setDialogOpen(false)}
              disabled={submitLoading}
              sx={{ textTransform: "none", fontWeight: 600, color: "text.secondary" }}
            >
              Cancel
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={submitLoading}
              sx={{
                textTransform: "none",
                borderRadius: 2.5,
                fontWeight: 700,
                backgroundColor: "#12233b",
                "&:hover": { backgroundColor: "#1b3559" }
              }}
            >
              {submitLoading ? "Assigning..." : "Assign Patient"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Box>
  );
};

export default AssignedPatientsPage;
