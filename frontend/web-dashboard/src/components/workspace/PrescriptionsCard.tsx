import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  Typography,
  List,
  ListItem,
  ListItemText,
  Box,
  CircularProgress,
  Chip,
  Button,
  IconButton,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  FormControlLabel,
  Switch,
  Alert
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";
import MedicationOutlinedIcon from "@mui/icons-material/MedicationOutlined";
import DescriptionOutlinedIcon from "@mui/icons-material/DescriptionOutlined";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

import { useAuth } from "../../context/AuthContext";
import { prescriptionsService } from "../../services/prescriptionsService";
import type { Prescription } from "../../types/prescription";

interface PrescriptionsCardProps {
  patientId: number;
  refreshTrigger?: number;
}

const PrescriptionsCard = ({ patientId, refreshTrigger }: PrescriptionsCardProps) => {
  const { role } = useAuth();
  const isDoctor = role === "DOCTOR";

  const [loading, setLoading] = useState(true);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Dialog state
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedPrescription, setSelectedPrescription] = useState<Prescription | null>(null);

  // Form states
  const [prescriptionName, setPrescriptionName] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [notes, setNotes] = useState("");
  const [active, setActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const fetchPrescriptions = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      const response = await prescriptionsService.getPrescriptions(patientId);
      setPrescriptions(response || []);
    } catch (err) {
      console.error("Failed to load prescriptions", err);
    } finally {
      if (!silent) setLoading(false);
    }
  };

  useEffect(() => {
    if (!patientId) return;
    const isInitial = !refreshTrigger || refreshTrigger === 0;
    fetchPrescriptions(!isInitial);
  }, [patientId, refreshTrigger]);

  const handleOpenCreate = () => {
    setPrescriptionName("");
    setStartDate(new Date().toISOString().split("T")[0]);
    setEndDate(new Date(Date.now() + 30 * 24 * 60 * 60 * 1000).toISOString().split("T")[0]); // 30 days default
    setNotes("");
    setActive(true);
    setCreateDialogOpen(true);
    setError(null);
  };

  const handleOpenEdit = (pres: Prescription) => {
    setSelectedPrescription(pres);
    setPrescriptionName(pres.prescriptionName);
    setStartDate(pres.startDate.split("T")[0]);
    setEndDate(pres.endDate.split("T")[0]);
    setNotes(pres.notes || "");
    setActive(pres.active);
    setEditDialogOpen(true);
    setError(null);
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setSubmitting(true);
    setError(null);
    try {
      await prescriptionsService.createPrescription(patientId, {
        prescriptionName,
        startDate,
        endDate,
        notes: notes || undefined
      });
      setCreateDialogOpen(false);
      fetchPrescriptions();
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || err.message || "Failed to create prescription.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPrescription) return;
    setSubmitting(true);
    setError(null);
    try {
      await prescriptionsService.updatePrescription(selectedPrescription.prescriptionId, {
        prescriptionName,
        startDate,
        endDate,
        active,
        notes: notes || undefined
      });
      setEditDialogOpen(false);
      fetchPrescriptions();
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || err.message || "Failed to update prescription.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (prescriptionId: number) => {
    if (!window.confirm("Are you sure you want to deactivate/delete this prescription?")) return;
    try {
      await prescriptionsService.deactivatePrescription(prescriptionId);
      fetchPrescriptions();
    } catch (err: any) {
      console.error(err);
      alert(err.response?.data?.message || err.message || "Failed to deactivate prescription.");
    }
  };

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <CardHeading
          title="Prescriptions"
          icon={<MedicationOutlinedIcon />}
          color="#0284c7"
          sx={{ mb: 0 }}
          action={
            isDoctor && (
              <Button
                variant="contained"
                size="small"
                startIcon={<AddIcon />}
                onClick={handleOpenCreate}
                sx={{ textTransform: "none" }}
              >
                Add Prescription
              </Button>
            )
          }
        />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: 160 }}>
            <CircularProgress size={28} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : prescriptions.length === 0 ? (
          <EmptyState message="No active prescriptions for this patient" icon={<DescriptionOutlinedIcon />} />
        ) : (
          <List disablePadding sx={{ maxHeight: 300, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1 }}>
            {prescriptions.map((pres) => (
              <ListItem
                key={pres.prescriptionId}
                disableGutters
                sx={{
                  px: 1.5,
                  py: 1.25,
                  bgcolor: "#f8fafc",
                  border: "1px solid",
                  borderColor: "divider",
                  borderRadius: 2.5,
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  flexWrap: "wrap",
                  gap: 1.5
                }}
              >
                <Box sx={{ flex: "1 1 180px", minWidth: 0 }}>
                  <ListItemText
                    sx={{ m: 0 }}
                    primary={<Typography variant="subtitle2" sx={{ fontWeight: 700, color: "#12233b", overflowWrap: "anywhere" }}>{pres.prescriptionName}</Typography>}
                    secondary={<Typography variant="caption" color="text.secondary" sx={{ display: "block", overflowWrap: "anywhere" }}>{`Regimen: ${new Date(pres.startDate).toLocaleDateString()} - ${new Date(pres.endDate).toLocaleDateString()} ${pres.notes ? `• ${pres.notes}` : ""}`}</Typography>}
                  />
                </Box>
                <Box sx={{ display: "flex", alignItems: "center", gap: 0.5, flexShrink: 0 }}>
                  <Chip
                    label={pres.active ? "Active" : "Inactive"}
                    color={pres.active ? "success" : "default"}
                    size="small"
                    variant="outlined"
                  />
                  {isDoctor && (
                    <>
                      <IconButton size="small" color="primary" aria-label="Edit prescription" onClick={() => handleOpenEdit(pres)}>
                        <EditIcon fontSize="small" />
                      </IconButton>
                      <IconButton size="small" color="error" aria-label="Deactivate prescription" onClick={() => handleDelete(pres.prescriptionId)}>
                        <DeleteIcon fontSize="small" />
                      </IconButton>
                    </>
                  )}
                </Box>
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>

      {/* Create Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: 800, color: "#12233b" }}>New Prescription</DialogTitle>
        <form onSubmit={handleCreateSubmit}>
          <DialogContent dividers sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Prescription Name / Medicine"
              value={prescriptionName}
              onChange={(e) => setPrescriptionName(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Start Date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
              slotProps={{ inputLabel: { shrink: true } }}
              fullWidth
            />
            <TextField
              label="End Date"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
              slotProps={{ inputLabel: { shrink: true } }}
              fullWidth
            />
            <TextField
              label="Regimen Notes / Instructions"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              multiline
              rows={2}
              fullWidth
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setCreateDialogOpen(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={submitting}>
              {submitting ? <CircularProgress size={24} /> : "Create Prescription"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: 800, color: "#12233b" }}>Edit Prescription</DialogTitle>
        <form onSubmit={handleEditSubmit}>
          <DialogContent dividers sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Prescription Name / Medicine"
              value={prescriptionName}
              onChange={(e) => setPrescriptionName(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Start Date"
              type="date"
              value={startDate}
              onChange={(e) => setStartDate(e.target.value)}
              required
              slotProps={{ inputLabel: { shrink: true } }}
              fullWidth
            />
            <TextField
              label="End Date"
              type="date"
              value={endDate}
              onChange={(e) => setEndDate(e.target.value)}
              required
              slotProps={{ inputLabel: { shrink: true } }}
              fullWidth
            />
            <TextField
              label="Regimen Notes / Instructions"
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              multiline
              rows={2}
              fullWidth
            />
            <FormControlLabel
              control={
                <Switch
                  checked={active}
                  onChange={(e) => setActive(e.target.checked)}
                  color="primary"
                />
              }
              label="Prescription Status Active"
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setEditDialogOpen(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={submitting}>
              {submitting ? <CircularProgress size={24} /> : "Save Changes"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Card>
  );
};

export default PrescriptionsCard;
