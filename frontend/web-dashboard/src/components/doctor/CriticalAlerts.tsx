import { useState } from "react";
import {
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Chip,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  TextField,
  Box,
  CircularProgress,
  Alert as MuiAlert
} from "@mui/material";
import CheckIcon from "@mui/icons-material/Check";
import DoneAllIcon from "@mui/icons-material/DoneAll";

import { doctorService } from "../../services/doctorService";
import type { Alert } from "../../types/alert";
import type { DoctorAssignedPatient } from "../../types/doctor";

interface CriticalAlertsProps {
  alerts: Alert[];
  patients: DoctorAssignedPatient[];
  onRefresh: () => void;
}

const CriticalAlerts: React.FC<CriticalAlertsProps> = ({ alerts, patients, onRefresh }) => {
  const [dialogOpen, setDialogOpen] = useState(false);
  const [selectedAlertId, setSelectedAlertId] = useState<number | null>(null);
  const [resolutionNote, setResolutionNote] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const handleAcknowledge = async (alertId: number) => {
    try {
      await doctorService.acknowledgeAlert(alertId);
      onRefresh();
    } catch (err: any) {
      console.error(err);
      alert(err.response?.data?.message || err.message || "Failed to acknowledge alert.");
    }
  };

  const handleOpenResolveDialog = (alertId: number) => {
    setSelectedAlertId(alertId);
    setResolutionNote("");
    setDialogOpen(true);
    setError(null);
  };

  const handleResolve = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedAlertId) return;

    setSubmitting(true);
    setError(null);
    try {
      await doctorService.resolveAlert(selectedAlertId, resolutionNote);
      setDialogOpen(false);
      onRefresh();
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message || err.message || "Failed to resolve alert.");
    } finally {
      setSubmitting(false);
    }
  };

  const getSeverityColor = (severity: string) => {
    switch (severity.toUpperCase()) {
      case "CRITICAL":
        return "error";
      case "HIGH":
        return "warning";
      case "MEDIUM":
      case "LOW":
      default:
        return "info";
    }
  };

  // Filter only unresolved alerts to show critical focus
  const activeAlerts = alerts.filter((a) => a.status !== "RESOLVED");

  return (
    <Card elevation={2} sx={{ borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: "bold" }}>
            Critical Medical & Vitals Alerts
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Outstanding high-risk alerts retrieved from CGM sensors and smart insulin pumps. Review and coordinate care plans immediately.
          </Typography>
        </Box>

        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2 }}>
          <Table size="small">
            <TableHead sx={{ backgroundColor: "action.hover" }}>
              <TableRow>
                <TableCell sx={{ fontWeight: "bold" }}>Severity</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Patient</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Alert Description</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Status</TableCell>
                <TableCell sx={{ fontWeight: "bold" }}>Timestamp</TableCell>
                <TableCell align="center" sx={{ fontWeight: "bold" }}>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {activeAlerts.map((alert) => {
                const patient = patients.find((p) => p.patientId === alert.patientId);
                const patientName = patient ? patient.patientName : (alert.patientId ? `Patient ID: ${alert.patientId}` : "N/A");
                return (
                  <TableRow key={alert.alertId} hover>
                    <TableCell>
                      <Chip
                        label={alert.severity}
                        color={getSeverityColor(alert.severity)}
                        size="small"
                        sx={{ fontWeight: "bold", textTransform: "uppercase" }}
                      />
                    </TableCell>
                    <TableCell sx={{ fontWeight: "medium" }}>
                      {patientName}
                    </TableCell>
                    <TableCell>
                      <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                        {alert.title}
                      </Typography>
                      <Typography variant="caption" color="text.secondary">
                        {alert.message}
                      </Typography>
                    </TableCell>
                    <TableCell>
                      <Chip
                        label={alert.status}
                        variant="outlined"
                        size="small"
                        color={alert.status === "ACKNOWLEDGED" ? "primary" : "default"}
                      />
                    </TableCell>
                    <TableCell>
                      {new Date(alert.createdAt).toLocaleString()}
                    </TableCell>
                    <TableCell align="center">
                      <Box sx={{ display: "flex", justifyContent: "center", gap: 1 }}>
                        {alert.status === "OPEN" && (
                          <Button
                            variant="outlined"
                            size="small"
                            startIcon={<CheckIcon />}
                            onClick={() => handleAcknowledge(alert.alertId)}
                            sx={{ textTransform: "none", borderRadius: 2 }}
                          >
                            Acknowledge
                          </Button>
                        )}
                        <Button
                          variant="contained"
                          size="small"
                          color="success"
                          startIcon={<DoneAllIcon />}
                          onClick={() => handleOpenResolveDialog(alert.alertId)}
                          sx={{ textTransform: "none", borderRadius: 2 }}
                        >
                          Resolve
                        </Button>
                      </Box>
                    </TableCell>
                  </TableRow>
                );
              })}

              {activeAlerts.length === 0 && (
                <TableRow>
                  <TableCell colSpan={5} align="center" sx={{ py: 4 }}>
                    <Typography variant="body2" color="text.secondary">
                      All alerts have been resolved. Excellent work!
                    </Typography>
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>

      {/* Resolution Note Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle sx={{ fontWeight: "bold" }}>Resolve Patient Alert</DialogTitle>
        <form onSubmit={handleResolve}>
          <DialogContent dividers sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
            {error && <MuiAlert severity="error">{error}</MuiAlert>}
            <Typography variant="body2" color="text.secondary">
              Please input a clinical resolution note outlining actions taken (e.g. contacted patient, adjusted dose insulin levels).
            </Typography>
            <TextField
              label="Resolution Note"
              multiline
              rows={3}
              value={resolutionNote}
              onChange={(e) => setResolutionNote(e.target.value)}
              fullWidth
              required
              variant="outlined"
            />
          </DialogContent>
          <DialogActions sx={{ px: 3, py: 2 }}>
            <Button onClick={() => setDialogOpen(false)} disabled={submitting}>
              Cancel
            </Button>
            <Button type="submit" variant="contained" color="success" disabled={submitting}>
              {submitting ? <CircularProgress size={24} /> : "Confirm Resolution"}
            </Button>
          </DialogActions>
        </form>
      </Dialog>
    </Card>
  );
};

export default CriticalAlerts;
