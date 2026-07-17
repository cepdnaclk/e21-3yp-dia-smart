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
} from "@mui/material";

import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import { prescriptionsService } from "../../services/prescriptionsService";
import type { Prescription } from "../../types/prescription";

const PrescriptionsPage = () => {
  const [prescriptions, setPrescriptions] =
    useState<Prescription[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {
    const loadPrescriptions = async () => {
      try {
        const data =
          await prescriptionsService.getPrescriptions();

        setPrescriptions(data);
      } catch (err) {
        console.error(err);

        setError(
          "Failed to load prescriptions"
        );
      } finally {
        setLoading(false);
      }
    };

    loadPrescriptions();
  }, []);

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

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
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>Status</TableCell>
              <TableCell sx={{ fontWeight: 700, color: "#12233b" }}>Notes</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {prescriptions.length === 0 ? (
              <TableRow>
                <TableCell colSpan={5} align="center" sx={{ py: 3, color: "text.secondary" }}>
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
