import { useState, useEffect } from "react";
import type { SelectChangeEvent } from "@mui/material";
import {
  Card,
  CardContent,
  Typography,
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Checkbox,
  FormControlLabel,
  Box,
  CircularProgress,
  Alert,
  IconButton,
  Tooltip,
  Chip
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";
import PersonSearchOutlinedIcon from "@mui/icons-material/PersonSearchOutlined";
import LinkOutlinedIcon from "@mui/icons-material/LinkOutlined";
import MedicalServicesOutlinedIcon from "@mui/icons-material/MedicalServicesOutlined";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

import { adminService } from "../../services/adminService";
import type { AdminUserRecord, PatientAccessResponse } from "../../types/admin";

const permBadgeSx = {
  width: 26,
  height: 26,
  borderRadius: "50%",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
} as const;

interface PatientDoctorSectionProps {
  doctors: AdminUserRecord[];
  patients: AdminUserRecord[];
  patientIdMap: Record<number, AdminUserRecord>;
}

const PatientDoctorSection: React.FC<PatientDoctorSectionProps> = ({
  doctors,
  patients,
  patientIdMap
}) => {
  const [selectedDoctorId, setSelectedDoctorId] = useState<number | "">("");
  const [accessRecords, setAccessRecords] = useState<PatientAccessResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Dialog State
  const [dialogOpen, setDialogOpen] = useState(false);
  const [dialogLoading, setDialogLoading] = useState(false);
  const [dialogError, setDialogError] = useState<string | null>(null);
  const [formData, setFormData] = useState({
    patientUserId: "",
    relationshipLabel: "Primary Doctor",
    canView: true,
    canAcknowledgeAlerts: true,
    canEditPrescriptions: true
  });

  const fetchDoctorMappings = async (doctorId: number) => {
    setLoading(true);
    setError(null);
    try {
      const records = await adminService.getPatientAccessForUser(doctorId);
      // Filter only active Patient Access records
      setAccessRecords(records.filter((r) => r.status === "ACTIVE" && r.accessRole === "DOCTOR"));
    } catch (err: any) {
      console.error(err);
      setError("Failed to load doctor's assignments.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (selectedDoctorId) {
      fetchDoctorMappings(selectedDoctorId);
    } else {
      setAccessRecords([]);
    }
  }, [selectedDoctorId]);

  const handleDoctorChange = (e: SelectChangeEvent<number | "">) => {
    setSelectedDoctorId(e.target.value as number | "");
  };

  const handleRevoke = async (accessId: number) => {
    if (!window.confirm("Are you sure you want to revoke this patient access mapping?")) {
      return;
    }
    try {
      await adminService.revokePatientAccess(accessId);
      setAccessRecords((prev) => prev.filter((r) => r.accessId !== accessId));
    } catch (err: any) {
      console.error(err);
      alert(err.response?.data?.message || err.message || "Failed to revoke relationship mapping.");
    }
  };

  const handleOpenDialog = () => {
    setDialogOpen(true);
    setDialogError(null);
    setFormData({
      patientUserId: "",
      relationshipLabel: "Primary Doctor",
      canView: true,
      canAcknowledgeAlerts: true,
      canEditPrescriptions: true
    });
  };

  const handleDialogChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value, checked, type } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: type === "checkbox" ? checked : value
    }));
  };

  const handlePatientSelectChange = (e: SelectChangeEvent<string>) => {
    setFormData((prev) => ({
      ...prev,
      patientUserId: e.target.value
    }));
  };

  const handleAssign = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedDoctorId) return;
    if (!formData.patientUserId) {
      setDialogError("Please select a patient.");
      return;
    }

    setDialogLoading(true);
    setDialogError(null);

    // Resolve patientId from selected patient's userId using patientIdMap
    const pUserId = Number(formData.patientUserId);
    const resolvedEntry = Object.entries(patientIdMap).find(
      ([_, user]) => user.userId === pUserId
    );

    if (!resolvedEntry) {
      setDialogError("Selected patient profile does not have an initialized database patientId.");
      setDialogLoading(false);
      return;
    }

    const resolvedPatientId = Number(resolvedEntry[0]);

    try {
      await adminService.createPatientAccess({
        userId: selectedDoctorId,
        patientId: resolvedPatientId,
        accessRole: "DOCTOR",
        relationshipLabel: formData.relationshipLabel,
        canView: formData.canView,
        canAcknowledgeAlerts: formData.canAcknowledgeAlerts,
        canEditPrescriptions: formData.canEditPrescriptions
      });

      setDialogOpen(false);
      fetchDoctorMappings(selectedDoctorId);
    } catch (err: any) {
      console.error(err);
      setDialogError(err.response?.data?.message || err.message || "Failed to establish access.");
    } finally {
      setDialogLoading(false);
    }
  };

  return (
    <Card sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
        <CardHeading
          title="Patient ↔ Doctor Assignments"
          subtitle="Configure clinician access policies. Assign patients to clinicians for remote monitoring, alert reviews, and prescription authorization."
          icon={<MedicalServicesOutlinedIcon />}
          color="#0284c7"
          sx={{ mb: 0, alignItems: "flex-start" }}
        />

        {/* Doctor Dropdown Select */}
        <FormControl fullWidth size="small">
          <InputLabel id="doctor-select-label">Select Clinician / Doctor</InputLabel>
          <Select
            labelId="doctor-select-label"
            label="Select Clinician / Doctor"
            value={selectedDoctorId}
            onChange={handleDoctorChange}
          >
            <MenuItem value="">
              <em>NoneSelected</em>
            </MenuItem>
            {doctors.map((doc) => (
              <MenuItem key={doc.userId} value={doc.userId}>
                {doc.displayName} ({doc.email})
              </MenuItem>
            ))}
          </Select>
        </FormControl>

        {/* Mapped Patients Table */}
        {selectedDoctorId ? (
          <Box sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 1 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "primary.main" }}>
                Active Assigned Patients
              </Typography>
              <Button
                variant="outlined"
                size="small"
                startIcon={<AddIcon />}
                onClick={handleOpenDialog}
              >
                Assign Patient
              </Button>
            </Box>

            {loading ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
                <CircularProgress size={30} thickness={4} sx={{ color: "secondary.dark" }} />
              </Box>
            ) : error ? (
              <Alert severity="error">{error}</Alert>
            ) : (
              <TableContainer component={Paper} variant="outlined" sx={{ overflowX: "auto" }}>
                <Table size="small" sx={{ minWidth: 460 }}>
                  <TableHead>
                    <TableRow>
                      <TableCell>Patient</TableCell>
                      <TableCell>Label</TableCell>
                      <TableCell align="center">Permissions</TableCell>
                      <TableCell align="center">Revoke</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {accessRecords.map((record) => {
                      const patientUser = patientIdMap[record.patientId];
                      return (
                        <TableRow key={record.accessId} hover>
                          <TableCell>
                            <Typography variant="body2" sx={{ fontWeight: 600, color: "text.primary" }}>
                              {patientUser?.displayName || `Patient ID: ${record.patientId}`}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" sx={{ display: "block", overflowWrap: "anywhere" }}>
                              {patientUser?.email || ""}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            <Chip
                              size="small"
                              variant="outlined"
                              label={record.relationshipLabel || "Clinician"}
                              sx={{ color: "#12233b", bgcolor: "#f1f5f9", borderColor: "#e2e8f0" }}
                            />
                          </TableCell>
                          <TableCell align="center">
                            <Box sx={{ display: "flex", justifyContent: "center", gap: 1 }}>
                              <Tooltip title="View Vitals">
                                <Box sx={{ ...permBadgeSx, color: record.canView ? "success.main" : "text.disabled", bgcolor: record.canView ? "#d1fae5" : "#f1f5f9" }}>
                                  {record.canView ? <CheckIcon fontSize="small" /> : <CloseIcon fontSize="small" />}
                                </Box>
                              </Tooltip>
                              <Tooltip title="Acknowledge Alerts">
                                <Box sx={{ ...permBadgeSx, color: record.canAcknowledgeAlerts ? "success.main" : "text.disabled", bgcolor: record.canAcknowledgeAlerts ? "#d1fae5" : "#f1f5f9" }}>
                                  {record.canAcknowledgeAlerts ? <CheckIcon fontSize="small" /> : <CloseIcon fontSize="small" />}
                                </Box>
                              </Tooltip>
                              <Tooltip title="Edit Prescriptions">
                                <Box sx={{ ...permBadgeSx, color: record.canEditPrescriptions ? "success.main" : "text.disabled", bgcolor: record.canEditPrescriptions ? "#d1fae5" : "#f1f5f9" }}>
                                  {record.canEditPrescriptions ? <CheckIcon fontSize="small" /> : <CloseIcon fontSize="small" />}
                                </Box>
                              </Tooltip>
                            </Box>
                          </TableCell>
                          <TableCell align="center">
                            <IconButton size="small" color="error" aria-label="Revoke" onClick={() => handleRevoke(record.accessId)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      );
                    })}

                    {accessRecords.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} sx={{ py: 2 }}>
                          <EmptyState
                            message="No active patients assigned to this doctor."
                            icon={<PersonSearchOutlinedIcon />}
                            minHeight={120}
                          />
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        ) : (
          <EmptyState
            message="Please select a clinician to manage patient mapping configurations."
            icon={<MedicalServicesOutlinedIcon />}
          />
        )}
      </CardContent>

      {/* Assign Patient Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
          <Box
            aria-hidden
            sx={{
              width: 38,
              height: 38,
              borderRadius: 2,
              flexShrink: 0,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              color: "#0284c7",
              bgcolor: "#e0f2fe",
            }}
          >
            <LinkOutlinedIcon fontSize="small" />
          </Box>
          Assign Patient to Doctor
        </DialogTitle>
        <form onSubmit={handleAssign}>
          <DialogContent dividers sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
            {dialogError && <Alert severity="error">{dialogError}</Alert>}

            <FormControl fullWidth required size="small">
              <InputLabel id="dialog-patient-select-label">Select Patient</InputLabel>
              <Select
                labelId="dialog-patient-select-label"
                label="Select Patient"
                value={formData.patientUserId}
                onChange={handlePatientSelectChange}
              >
                <MenuItem value="">
                  <em>Select...</em>
                </MenuItem>
                {patients.map((p) => (
                  <MenuItem key={p.userId} value={p.userId.toString()}>
                    {p.displayName} ({p.email})
                  </MenuItem>
                ))}
              </Select>
            </FormControl>

            <TextField
              size="small"
              label="Relationship Label"
              name="relationshipLabel"
              value={formData.relationshipLabel}
              onChange={handleDialogChange}
              fullWidth
            />

            <Box
              sx={{
                display: "flex",
                flexDirection: "column",
                gap: 0.25,
                px: 1.5,
                py: 1,
                borderRadius: 2,
                border: "1px solid",
                borderColor: "divider",
                bgcolor: "#f8fafc",
              }}
            >
              <FormControlLabel
                control={
                  <Checkbox
                    name="canView"
                    checked={formData.canView}
                    onChange={handleDialogChange}
                    color="primary"
                  />
                }
                label="Can View Medical Vitals"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    name="canAcknowledgeAlerts"
                    checked={formData.canAcknowledgeAlerts}
                    onChange={handleDialogChange}
                    color="primary"
                  />
                }
                label="Can Acknowledge Vitals Alerts"
              />
              <FormControlLabel
                control={
                  <Checkbox
                    name="canEditPrescriptions"
                    checked={formData.canEditPrescriptions}
                    onChange={handleDialogChange}
                    color="primary"
                  />
                }
                label="Can Edit Prescriptions/Schedules"
              />
            </Box>
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setDialogOpen(false)} disabled={dialogLoading} color="inherit">
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={dialogLoading} sx={{ minWidth: 150 }}>
              {dialogLoading ? <CircularProgress size={22} color="inherit" /> : "Establish Mapping"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Card>
  );
};

export default PatientDoctorSection;
