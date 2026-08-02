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
  Tooltip
} from "@mui/material";
import DeleteIcon from "@mui/icons-material/Delete";
import AddIcon from "@mui/icons-material/Add";
import CheckIcon from "@mui/icons-material/Check";
import CloseIcon from "@mui/icons-material/Close";

import { adminService } from "../../services/adminService";
import type { AdminUserRecord, PatientAccessResponse } from "../../types/admin";

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
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3, display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: "bold" }}>
            Patient ↔ Doctor Assignments
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Configure clinician access policies. Assign patients to clinicians for remote monitoring, alert reviews, and prescription authorization.
          </Typography>
        </Box>

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
            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
              <Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>
                Active Assigned Patients
              </Typography>
              <Button
                variant="outlined"
                size="small"
                startIcon={<AddIcon />}
                onClick={handleOpenDialog}
                sx={{ textTransform: "none", borderRadius: 2 }}
              >
                Assign Patient
              </Button>
            </Box>

            {loading ? (
              <Box sx={{ display: "flex", justifyContent: "center", py: 3 }}>
                <CircularProgress size={30} />
              </Box>
            ) : error ? (
              <Alert severity="error">{error}</Alert>
            ) : (
              <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
                <Table size="small">
                  <TableHead sx={{ backgroundColor: "action.hover" }}>
                    <TableRow>
                      <TableCell sx={{ fontWeight: "bold" }}>Patient</TableCell>
                      <TableCell sx={{ fontWeight: "bold" }}>Label</TableCell>
                      <TableCell align="center" sx={{ fontWeight: "bold" }}>Permissions</TableCell>
                      <TableCell align="center" sx={{ fontWeight: "bold" }}>Revoke</TableCell>
                    </TableRow>
                  </TableHead>
                  <TableBody>
                    {accessRecords.map((record) => {
                      const patientUser = patientIdMap[record.patientId];
                      return (
                        <TableRow key={record.accessId} hover>
                          <TableCell>
                            <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                              {patientUser?.displayName || `Patient ID: ${record.patientId}`}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              {patientUser?.email || ""}
                            </Typography>
                          </TableCell>
                          <TableCell>
                            {record.relationshipLabel || "Clinician"}
                          </TableCell>
                          <TableCell align="center">
                            <Box sx={{ display: "flex", justifyContent: "center", gap: 1 }}>
                              <Tooltip title="View Vitals">
                                <Box sx={{ color: record.canView ? "success.main" : "text.disabled" }}>
                                  {record.canView ? <CheckIcon fontSize="small" /> : <CloseIcon fontSize="small" />}
                                </Box>
                              </Tooltip>
                              <Tooltip title="Acknowledge Alerts">
                                <Box sx={{ color: record.canAcknowledgeAlerts ? "success.main" : "text.disabled" }}>
                                  {record.canAcknowledgeAlerts ? <CheckIcon fontSize="small" /> : <CloseIcon fontSize="small" />}
                                </Box>
                              </Tooltip>
                              <Tooltip title="Edit Prescriptions">
                                <Box sx={{ color: record.canEditPrescriptions ? "success.main" : "text.disabled" }}>
                                  {record.canEditPrescriptions ? <CheckIcon fontSize="small" /> : <CloseIcon fontSize="small" />}
                                </Box>
                              </Tooltip>
                            </Box>
                          </TableCell>
                          <TableCell align="center">
                            <IconButton size="small" color="error" onClick={() => handleRevoke(record.accessId)}>
                              <DeleteIcon fontSize="small" />
                            </IconButton>
                          </TableCell>
                        </TableRow>
                      );
                    })}

                    {accessRecords.length === 0 && (
                      <TableRow>
                        <TableCell colSpan={4} align="center" sx={{ py: 3 }}>
                          <Typography variant="body2" color="text.secondary">
                            No active patients assigned to this doctor.
                          </Typography>
                        </TableCell>
                      </TableRow>
                    )}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </Box>
        ) : (
          <Typography variant="body2" color="text.disabled" sx={{ textAlign: "center", py: 4 }}>
            Please select a clinician to manage patient mapping configurations.
          </Typography>
        )}
      </CardContent>

      {/* Assign Patient Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: "bold" }}>Assign Patient to Doctor</DialogTitle>
        <form onSubmit={handleAssign}>
          <DialogContent dividers sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
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

            <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5 }}>
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
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setDialogOpen(false)} disabled={dialogLoading}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={dialogLoading}>
              {dialogLoading ? <CircularProgress size={24} /> : "Establish Mapping"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Card>
  );
};

export default PatientDoctorSection;
