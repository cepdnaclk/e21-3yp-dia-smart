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
  FormControl,
  InputLabel,
  Select,
  MenuItem,
  Switch,
  FormControlLabel,
  Alert
} from "@mui/material";
import AddIcon from "@mui/icons-material/Add";
import EditIcon from "@mui/icons-material/Edit";
import DeleteIcon from "@mui/icons-material/Delete";

import { useAuth } from "../../context/AuthContext";
import { doseScheduleService } from "../../services/doseScheduleService";
import { prescriptionsService } from "../../services/prescriptionsService";
import type { DoseSchedule } from "../../types/doseSchedule";
import type { Prescription } from "../../types/prescription";

interface DoseScheduleCardProps {
  patientId: number;
}

const DoseScheduleCard = ({ patientId }: DoseScheduleCardProps) => {
  const { role } = useAuth();
  const isDoctor = role === "DOCTOR";

  const [loading, setLoading] = useState(true);
  const [schedules, setSchedules] = useState<DoseSchedule[]>([]);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [error, setError] = useState<string | null>(null);

  // Dialog controls
  const [createDialogOpen, setCreateDialogOpen] = useState(false);
  const [editDialogOpen, setEditDialogOpen] = useState(false);
  const [selectedSchedule, setSelectedSchedule] = useState<DoseSchedule | null>(null);

  // Form states
  const [prescriptionId, setPrescriptionId] = useState<number | "">("");
  const [scheduleLabel, setScheduleLabel] = useState("");
  const [scheduledTime, setScheduledTime] = useState("08:00");
  const [doseUnits, setDoseUnits] = useState<number | "">(4);
  const [daysOfWeek, setDaysOfWeek] = useState("1,2,3,4,5,6,7");
  const [windowStart, setWindowStart] = useState("07:30");
  const [windowEnd, setWindowEnd] = useState("08:30");
  const [active, setActive] = useState(true);
  const [submitting, setSubmitting] = useState(false);

  const fetchSchedules = async () => {
    try {
      const [schedResponse, presResponse] = await Promise.all([
        doseScheduleService.getDoseSchedules(patientId),
        isDoctor ? prescriptionsService.getPrescriptions(patientId) : Promise.resolve([])
      ]);
      setSchedules(schedResponse?.content ?? []);
      setPrescriptions(presResponse.filter(p => p.active) || []);
    } catch (err) {
      console.error("Failed to load dose schedules or prescriptions", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (!patientId) return;
    fetchSchedules();
  }, [patientId]);

  const handleOpenCreate = () => {
    setPrescriptionId(prescriptions.length > 0 ? prescriptions[0].prescriptionId : "");
    setScheduleLabel("");
    setScheduledTime("08:00");
    setDoseUnits(4);
    setDaysOfWeek("1,2,3,4,5,6,7");
    setWindowStart("07:30");
    setWindowEnd("08:30");
    setCreateDialogOpen(true);
    setError(null);
  };

  const handleOpenEdit = (sched: DoseSchedule) => {
    setSelectedSchedule(sched);
    setScheduleLabel(sched.scheduleLabel);
    setScheduledTime(sched.targetTime ? sched.targetTime.substring(0, 5) : sched.scheduledTime.substring(0, 5));
    setDoseUnits(sched.doseUnits);
    setDaysOfWeek(sched.daysOfWeek);
    setWindowStart(sched.windowStart ? sched.windowStart.substring(0, 5) : "07:30");
    setWindowEnd(sched.windowEnd ? sched.windowEnd.substring(0, 5) : "08:30");
    setActive(sched.active);
    setEditDialogOpen(true);
    setError(null);
  };

  const validateTimeWindow = (target: string, start: string, end: string): boolean => {
    if (start >= target) {
      setError("Window Start must be before the Target Time.");
      return false;
    }
    if (target >= end) {
      setError("Target Time must be before the Window End.");
      return false;
    }
    return true;
  };

  const handleCreateSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!prescriptionId) {
      setError("Active prescription is required to set dose schedules.");
      return;
    }
    if (!validateTimeWindow(scheduledTime, windowStart, windowEnd)) {
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const formattedTime = scheduledTime.length === 5 ? `${scheduledTime}:00` : scheduledTime;
      const formattedStart = windowStart.length === 5 ? `${windowStart}:00` : windowStart;
      const formattedEnd = windowEnd.length === 5 ? `${windowEnd}:00` : windowEnd;

      await doseScheduleService.createDoseSchedule(patientId, {
        prescriptionId: Number(prescriptionId),
        scheduleLabel,
        scheduledTime: formattedTime,
        targetTime: formattedTime,
        windowStart: formattedStart,
        windowEnd: formattedEnd,
        doseUnits: Number(doseUnits),
        daysOfWeek
      });
      setCreateDialogOpen(false);
      fetchSchedules();
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || err.message || "Failed to create schedule.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedSchedule) return;
    if (!validateTimeWindow(scheduledTime, windowStart, windowEnd)) {
      return;
    }
    setSubmitting(true);
    setError(null);
    try {
      const formattedTime = scheduledTime.length === 5 ? `${scheduledTime}:00` : scheduledTime;
      const formattedStart = windowStart.length === 5 ? `${windowStart}:00` : windowStart;
      const formattedEnd = windowEnd.length === 5 ? `${windowEnd}:00` : windowEnd;

      await doseScheduleService.updateDoseSchedule(selectedSchedule.scheduleId, {
        scheduleLabel,
        scheduledTime: formattedTime,
        targetTime: formattedTime,
        windowStart: formattedStart,
        windowEnd: formattedEnd,
        doseUnits: Number(doseUnits),
        daysOfWeek,
        active
      });
      setEditDialogOpen(false);
      fetchSchedules();
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || err.message || "Failed to update schedule.");
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (scheduleId: number) => {
    if (!window.confirm("Are you sure you want to deactivate/delete this dose schedule?")) return;
    try {
      await doseScheduleService.deactivateDoseSchedule(scheduleId);
      fetchSchedules();
    } catch (err: any) {
      console.error(err);
      alert(err.response?.data?.message || err.message || "Failed to deactivate dose schedule.");
    }
  };

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
          <Typography variant="h6" sx={{ fontWeight: "bold" }}>
            Dose Schedule
          </Typography>
          {isDoctor && (
            <Button
              variant="contained"
              size="small"
              startIcon={<AddIcon />}
              onClick={handleOpenCreate}
              sx={{ textTransform: "none", borderRadius: 2 }}
            >
              Add Schedule
            </Button>
          )}
        </Box>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress />
          </Box>
        ) : schedules.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2, textAlign: "center" }}>
            No dosing schedules configured for this patient
          </Typography>
        ) : (
          <List disablePadding sx={{ maxHeight: 300, overflowY: "auto", display: "flex", flexDirection: "column", gap: 1 }}>
            {schedules.map((sched) => (
              <ListItem
                key={sched.scheduleId}
                disableGutters
                sx={{
                  px: 1.5,
                  py: 1,
                  bgcolor: "action.hover",
                  borderRadius: 2
                }}
                secondaryAction={
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
                    <Chip
                      label={sched.active ? "Active" : "Inactive"}
                      color={sched.active ? "success" : "default"}
                      size="small"
                      variant="outlined"
                    />
                    {isDoctor && (
                      <>
                        <IconButton size="small" color="primary" onClick={() => handleOpenEdit(sched)}>
                          <EditIcon fontSize="small" />
                        </IconButton>
                        <IconButton size="small" color="error" onClick={() => handleDelete(sched.scheduleId)}>
                          <DeleteIcon fontSize="small" />
                        </IconButton>
                      </>
                    )}
                  </Box>
                }
              >
                <ListItemText
                  primary={<Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>{sched.scheduleLabel}</Typography>}
                  secondary={`Target: ${sched.targetTime ? sched.targetTime.substring(0, 5) : sched.scheduledTime.substring(0, 5)} (Window: ${sched.windowStart ? sched.windowStart.substring(0, 5) : "—"} to ${sched.windowEnd ? sched.windowEnd.substring(0, 5) : "—"}) • Dosage: ${sched.doseUnits} Units • Days: [${sched.daysOfWeek}]`}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>

      {/* Create Dialog */}
      <Dialog open={createDialogOpen} onClose={() => setCreateDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: "bold" }}>New Dose Schedule</DialogTitle>
        <form onSubmit={handleCreateSubmit}>
          <DialogContent dividers sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <FormControl fullWidth required>
              <InputLabel id="select-prescription-label">Prescription</InputLabel>
              <Select
                labelId="select-prescription-label"
                label="Prescription"
                value={prescriptionId}
                onChange={(e) => setPrescriptionId(e.target.value as number)}
              >
                {prescriptions.map((pres) => (
                  <MenuItem key={pres.prescriptionId} value={pres.prescriptionId}>
                    {pres.prescriptionName}
                  </MenuItem>
                ))}
              </Select>
            </FormControl>
            <TextField
              label="Schedule Label (e.g. Morning Bolus)"
              value={scheduleLabel}
              onChange={(e) => setScheduleLabel(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Target Dose Time"
              type="time"
              value={scheduledTime}
              onChange={(e) => setScheduledTime(e.target.value)}
              required
              slotProps={{ inputLabel: { shrink: true } }}
              fullWidth
            />
            <Box sx={{ display: "flex", gap: 2 }}>
              <TextField
                label="Window Start"
                type="time"
                value={windowStart}
                onChange={(e) => setWindowStart(e.target.value)}
                required
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="Window End"
                type="time"
                value={windowEnd}
                onChange={(e) => setWindowEnd(e.target.value)}
                required
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
            </Box>
            <TextField
              label="Dose Units"
              type="number"
              value={doseUnits}
              onChange={(e) => setDoseUnits(e.target.value === "" ? "" : Number(e.target.value))}
              required
              fullWidth
            />
            <TextField
              label="Days Of Week (e.g. 1,2,3,4,5,6,7)"
              value={daysOfWeek}
              onChange={(e) => setDaysOfWeek(e.target.value)}
              required
              fullWidth
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setCreateDialogOpen(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" disabled={submitting}>
              {submitting ? <CircularProgress size={24} /> : "Create Schedule"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>

      {/* Edit Dialog */}
      <Dialog open={editDialogOpen} onClose={() => setEditDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: "bold" }}>Edit Dose Schedule</DialogTitle>
        <form onSubmit={handleEditSubmit}>
          <DialogContent dividers sx={{ display: "flex", flexDirection: "column", gap: 2.5 }}>
            {error && <Alert severity="error">{error}</Alert>}
            <TextField
              label="Schedule Label (e.g. Morning Bolus)"
              value={scheduleLabel}
              onChange={(e) => setScheduleLabel(e.target.value)}
              required
              fullWidth
            />
            <TextField
              label="Target Dose Time"
              type="time"
              value={scheduledTime}
              onChange={(e) => setScheduledTime(e.target.value)}
              required
              slotProps={{ inputLabel: { shrink: true } }}
              fullWidth
            />
            <Box sx={{ display: "flex", gap: 2 }}>
              <TextField
                label="Window Start"
                type="time"
                value={windowStart}
                onChange={(e) => setWindowStart(e.target.value)}
                required
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
              <TextField
                label="Window End"
                type="time"
                value={windowEnd}
                onChange={(e) => setWindowEnd(e.target.value)}
                required
                slotProps={{ inputLabel: { shrink: true } }}
                fullWidth
              />
            </Box>
            <TextField
              label="Dose Units"
              type="number"
              value={doseUnits}
              onChange={(e) => setDoseUnits(e.target.value === "" ? "" : Number(e.target.value))}
              required
              fullWidth
            />
            <TextField
              label="Days Of Week (e.g. 1,2,3,4,5,6,7)"
              value={daysOfWeek}
              onChange={(e) => setDaysOfWeek(e.target.value)}
              required
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
              label="Schedule Status Active"
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

export default DoseScheduleCard;
