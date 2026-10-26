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
import MedicationRoundedIcon from "@mui/icons-material/MedicationRounded";
import ScheduleRoundedIcon from "@mui/icons-material/ScheduleRounded";
import CheckCircleRoundedIcon from "@mui/icons-material/CheckCircleRounded";
import PauseCircleOutlineRoundedIcon from "@mui/icons-material/PauseCircleOutlineRounded";

import EmptyState from "../../components/common/EmptyState";
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
      <TableContainer
        component={Paper}
        variant="outlined"
        sx={{
          display: { xs: "none", md: "block" },
          borderRadius: "14px",
          boxShadow: "0 1px 2px rgba(15, 23, 42, 0.04), 0 4px 16px rgba(15, 23, 42, 0.04)",
          overflowX: "auto",
        }}
      >
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Prescription</TableCell>
              <TableCell>Start Date</TableCell>
              <TableCell>End Date</TableCell>
              <TableCell>Dosing Schedules</TableCell>
              <TableCell>Status</TableCell>
              <TableCell>Notes</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {prescriptions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={6} sx={{ p: 3 }}>
                  <EmptyState
                    message="No prescriptions available"
                    icon={<MedicationRoundedIcon />}
                  />
                </TableCell>
              </TableRow>
            ) : (
              prescriptions.map((item) => (
                <TableRow key={item.prescriptionId} hover>
                  <TableCell sx={{ fontWeight: 600, color: "primary.main" }}>
                    <Box sx={{ display: "flex", alignItems: "center", gap: 1.25 }}>
                      <Box
                        aria-hidden
                        sx={{
                          width: 32,
                          height: 32,
                          flexShrink: 0,
                          borderRadius: 2,
                          display: "flex",
                          alignItems: "center",
                          justifyContent: "center",
                          bgcolor: "#e0f2fe",
                          color: "#0284c7",
                        }}
                      >
                        <MedicationRoundedIcon sx={{ fontSize: 18 }} />
                      </Box>
                      {item.prescriptionName}
                    </Box>
                  </TableCell>
                  <TableCell sx={{ whiteSpace: "nowrap" }}>{item.startDate}</TableCell>
                  <TableCell sx={{ whiteSpace: "nowrap" }}>{item.endDate}</TableCell>
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
                              px: 1.25,
                              py: 1,
                              bgcolor: "#f8fafc",
                              borderRadius: 2,
                              border: "1px solid #e2e8f0",
                              borderLeft: "3px solid #3ec1fa",
                              minWidth: 160
                            }}
                          >
                            <Typography variant="caption" sx={{ fontWeight: 700, color: "#1e293b", display: "block" }}>
                              {s.scheduleLabel}
                            </Typography>
                            <Typography variant="caption" color="text.secondary" sx={{ display: "inline-flex", alignItems: "center", gap: 0.5 }}>
                              <ScheduleRoundedIcon aria-hidden sx={{ fontSize: 13 }} />
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
                      icon={item.active ? <CheckCircleRoundedIcon /> : <PauseCircleOutlineRoundedIcon />}
                      label={item.active ? "Active" : "Inactive"}
                      color={item.active ? "success" : "default"}
                      variant="outlined"
                      sx={{ fontWeight: 700, fontSize: "0.75rem" }}
                    />
                  </TableCell>
                  <TableCell sx={{ color: "text.secondary", maxWidth: 280 }}>{item.notes ?? "No notes"}</TableCell>
                </TableRow>
              ))
            )}
          </TableBody>
        </Table>
      </TableContainer>

      {/* Mobile Card List View */}
      <Box sx={{ display: { xs: "flex", md: "none" }, flexDirection: "column", gap: 2 }}>
        {prescriptions.length === 0 ? (
          <EmptyState
            message="No prescriptions available."
            icon={<MedicationRoundedIcon />}
          />
        ) : (
          prescriptions.map((item) => (
            <Card key={item.prescriptionId}>
              <Box sx={{ p: 2.25 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "center", gap: 1.5, mb: 1.5 }}>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1.25, minWidth: 0 }}>
                    <Box
                      aria-hidden
                      sx={{
                        width: 34,
                        height: 34,
                        flexShrink: 0,
                        borderRadius: 2,
                        display: "flex",
                        alignItems: "center",
                        justifyContent: "center",
                        bgcolor: "#e0f2fe",
                        color: "#0284c7",
                      }}
                    >
                      <MedicationRoundedIcon sx={{ fontSize: 19 }} />
                    </Box>
                    <Typography variant="subtitle1" sx={{ fontWeight: 800, color: "#12233b", overflowWrap: "anywhere" }}>
                      {item.prescriptionName}
                    </Typography>
                  </Box>
                  <Chip
                    size="small"
                    icon={item.active ? <CheckCircleRoundedIcon /> : <PauseCircleOutlineRoundedIcon />}
                    label={item.active ? "Active" : "Inactive"}
                    color={item.active ? "success" : "default"}
                    variant="outlined"
                    sx={{ fontWeight: 700, fontSize: "0.7rem", flexShrink: 0 }}
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
