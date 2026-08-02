import { useEffect, useState } from "react";
import {
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
  Card,
  Typography,
  Box,
  Divider,
  Stack
} from "@mui/material";

import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import { prescriptionsService } from "../../services/prescriptionsService";
import { doseScheduleService } from "../../services/doseScheduleService";
import type { Prescription } from "../../types/prescription";
import type { DoseSchedule } from "../../types/doseSchedule";

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const PrescriptionsPage = () => {
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [schedules, setSchedules] = useState<DoseSchedule[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadPrescriptionsAndSchedules = async (silent = false) => {
    try {
      if (!silent) {
        setLoading(true);
        setError("");
      }
      const [presData, schedData] = await Promise.all([
        prescriptionsService.getPrescriptions(),
        doseScheduleService.getDoseSchedules()
      ]);

      setPrescriptions(presData);
      setSchedules(schedData?.content ?? []);
    } catch (err) {
      console.error(err);
      if (!silent) {
        setError("Failed to load prescriptions and schedules");
      }
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    loadPrescriptionsAndSchedules(false);
  }, []);

  useAutoRefresh(() => loadPrescriptionsAndSchedules(true), 5000);

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  const getSchedulesForPrescription = (prescriptionId: number) => {
    return schedules.filter((s) => s.prescriptionId === prescriptionId && s.active);
  };

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle mb={3}>Prescriptions</PageTitle>

      {/* Laptop / Desktop / Tablet Table View */}
      <TableContainer component={Paper} sx={{ display: { xs: "none", md: "block" }, borderRadius: 4 }}>
        <Table>
          <TableHead sx={{ bgcolor: "#f8f9fa" }}>
            <TableRow>
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>Prescription</TableCell>
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>Start Date</TableCell>
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>End Date</TableCell>
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>Dosing Schedules</TableCell>
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>Status</TableCell>
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>Notes</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {prescriptions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} align="center" sx={{ py: 3, color: "text.secondary" }}>
                  No prescriptions available
                </TableCell>
              </TableRow>
            ) : (
              prescriptions.map((item) => (
                <TableRow key={item.prescriptionId} hover>
                  <TableCell sx={{ fontWeight: 600 }}>{item.prescriptionName}</TableCell>
                  <TableCell>{item.startDate}</TableCell>
                  <TableCell>{item.endDate}</TableCell>
                  <TableCell>
                    {getSchedulesForPrescription(item.prescriptionId).length === 0 ? (
                      <Typography variant="body2" color="text.secondary" sx={{ fontStyle: "italic" }}>
                        None configured
                      </Typography>
                    ) : (
                      <Stack spacing={1}>
                        {getSchedulesForPrescription(item.prescriptionId).map((s) => (
                          <Box
                            key={s.scheduleId}
                            sx={{
                              p: 1,
                              bgcolor: "#f1f5f9",
                              borderRadius: 2,
                              border: "1px solid #e2e8f0",
                              minWidth: 160
                            }}
                          >
                            <Typography variant="caption" sx={{ fontWeight: 700, color: "#1e293b", display: "block" }}>
                              {s.scheduleLabel}
                            </Typography>
                            <Typography variant="caption" color="text.secondary">
                              Time: {s.targetTime ? s.targetTime.substring(0, 5) : s.scheduledTime.substring(0, 5)} • {s.doseUnits} Units
                            </Typography>
                          </Box>
                        ))}
                      </Stack>
                    )}
                  </TableCell>
                  <TableCell>
                    <Chip
                      size="small"
                      label={item.active ? "Active" : "Inactive"}
                      color={item.active ? "success" : "default"}
                      sx={{ fontWeight: 700, fontSize: "0.75rem" }}
                    />
                  </TableCell>
                  <TableCell color="text.secondary">{item.notes ?? "No notes"}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Mobile Card List View */}
      <Box sx={{ display: { xs: "flex", md: "none" }, flexDirection: "column", gap: 2 }}>
        {prescriptions.length === 0 ? (
          <Paper sx={{ p: 4, textAlign: "center", borderRadius: 3 }}>
            <Typography color="text.secondary" variant="body2">
              No prescriptions available.
            </Typography>
          </Paper>
        ) : (
          prescriptions.map((item) => (
            <Card key={item.prescriptionId} sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
              <Box sx={{ p: 2 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", mb: 1.5 }}>
                  <Typography variant="subtitle1" sx={{ fontWeight: 800, color: "#12233b" }}>
                    {item.prescriptionName}
                  </Typography>
                  <Chip
                    size="small"
                    label={item.active ? "Active" : "Inactive"}
                    color={item.active ? "success" : "default"}
                    sx={{ fontWeight: 700, fontSize: "0.7rem", height: 20 }}
                  />
                </Box>

                <Box sx={{ display: "flex", flexDirection: "column", gap: 1, mb: 1.5 }}>
                  <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                      Start Date
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                      {item.startDate}
                    </Typography>
                  </Box>
                  <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600 }}>
                      End Date
                    </Typography>
                    <Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b" }}>
                      {item.endDate}
                    </Typography>
                  </Box>
                </Box>

                <Divider sx={{ my: 1.5 }} />

                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block", mb: 1 }}>
                  Dosing Schedules
                </Typography>
                {getSchedulesForPrescription(item.prescriptionId).length === 0 ? (
                  <Typography variant="body2" color="text.secondary" sx={{ fontStyle: "italic", mb: 1.5 }}>
                    No dosing schedules configured.
                  </Typography>
                ) : (
                  <Stack spacing={1} sx={{ mb: 1.5 }}>
                    {getSchedulesForPrescription(item.prescriptionId).map((s) => (
                      <Box
                        key={s.scheduleId}
                        sx={{
                          p: 1.25,
                          bgcolor: "#f8fafc",
                          borderRadius: 2,
                          border: "1px solid #e2e8f0",
                          display: "flex",
                          justifyContent: "space-between",
                          alignItems: "center"
                        }}
                      >
                        <Box>
                          <Typography variant="body2" sx={{ fontWeight: 700, color: "#1e293b" }}>
                            {s.scheduleLabel}
                          </Typography>
                          <Typography variant="caption" color="text.secondary">
                            Dosage: {s.doseUnits} Units
                          </Typography>
                        </Box>
                        <Chip
                          label={s.targetTime ? s.targetTime.substring(0, 5) : s.scheduledTime.substring(0, 5)}
                          size="small"
                          sx={{ fontWeight: 700, bgcolor: "#e0f2fe", color: "#0369a1" }}
                        />
                      </Box>
                    ))}
                  </Stack>
                )}

                <Divider sx={{ my: 1.5 }} />

                <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, display: "block", mb: 0.5 }}>
                  Notes & Guidelines
                </Typography>
                <Typography variant="body2" color="text.primary" sx={{ fontSize: "0.85rem", lineHeight: 1.4 }}>
                  {item.notes ?? "No notes provided"}
                </Typography>
              </Box>
            </Card>
          ))
        )}
      </Box>
    </Box>
  );
};

export default PrescriptionsPage;
