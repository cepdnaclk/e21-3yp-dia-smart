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
import WarningAmberRoundedIcon from "@mui/icons-material/WarningAmberRounded";
import TaskAltIcon from "@mui/icons-material/TaskAlt";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

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
    <Card>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 0.5 }}>
        <CardHeading
          title="Critical Medical & Vitals Alerts"
          subtitle="Outstanding high-risk alerts retrieved from CGM sensors and smart insulin pumps. Review and coordinate care plans immediately."
          icon={<WarningAmberRoundedIcon />}
          color="#ef4444"
        />

        <TableContainer component={Paper} variant="outlined" sx={{ borderRadius: 2, overflowX: "auto" }}>
          <Table size="small" sx={{ minWidth: 760 }}>
            <TableHead>
              <TableRow>
                <TableCell>Severity</TableCell>
                <TableCell>Patient</TableCell>
                <TableCell>Alert Description</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Timestamp</TableCell>
                <TableCell align="right">Actions</TableCell>
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
                    <TableCell sx={{ fontWeight: 600, color: "text.primary" }}>
                      {patientName}
                    </TableCell>
                    <TableCell sx={{ maxWidth: 320 }}>
                      <Typography variant="body2" sx={{ fontWeight: 600, color: "text.primary" }}>
                        {alert.title}
                      </Typography>
                      <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 0.25 }}>
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
                    <TableCell sx={{ whiteSpace: "nowrap", color: "text.secondary" }}>
                      {new Date(alert.createdAt).toLocaleString()}
                    </TableCell>
                    <TableCell align="right">
                      <Box sx={{ display: "flex", justifyContent: "flex-end", gap: 1, flexWrap: "wrap" }}>
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
                  <TableCell colSpan={6} sx={{ py: 2, borderBottom: 0 }}>
                    <EmptyState
                      message="All alerts have been resolved. Excellent work!"
                      icon={<TaskAltIcon sx={{ color: "#10b981" }} />}
                      minHeight={120}
                    />
                  </TableCell>
                </TableRow>
              )}
            </TableBody>
          </Table>
        </TableContainer>
      </CardContent>

      {/* Resolution Note Dialog */}
      <Dialog open={dialogOpen} onClose={() => setDialogOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Resolve Patient Alert</DialogTitle>
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
