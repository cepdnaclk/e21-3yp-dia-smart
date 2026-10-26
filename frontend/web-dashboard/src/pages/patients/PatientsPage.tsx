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
  Avatar,
  Box,
  Typography,
} from "@mui/material";

import PageTitle from "../../components/common/PageTitle";
import { patientsService } from "../../services/patientsService";
import type { Patient } from "../../types/patient";

const PatientsPage = () => {
  // TODO: Scope patient list behavior by doctor, caregiver, and admin permissions in Milestone 4.
  const [patients, setPatients] = useState<
    Patient[]
  >([]);

  useEffect(() => {
    const loadPatients = async () => {
      const data =
        await patientsService.getPatients();

      setPatients(data);
    };

    loadPatients();
  }, []);

  return (
    <>
      <PageTitle>Patients</PageTitle>

      <TableContainer
        component={Paper}
        variant="outlined"
        sx={{ borderRadius: 3, overflowX: "auto", boxShadow: "0 1px 2px rgba(15, 23, 42, 0.04)" }}
      >
        <Table sx={{ minWidth: 560 }}>
          <TableHead>
            <TableRow>
              <TableCell>Patient</TableCell>
              <TableCell>Age</TableCell>
              <TableCell>Glucose (mg/dL)</TableCell>
              <TableCell>Inventory</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {patients.map((patient) => (
              <TableRow key={patient.id} hover>
                <TableCell>
                  <Box sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
                    <Avatar
                      sx={{ width: 34, height: 34, fontSize: "0.8rem", fontWeight: 700, bgcolor: "#e0f2fe", color: "#0284c7" }}
                    >
                      {String(patient.name ?? "").trim().charAt(0).toUpperCase() || "?"}
                    </Avatar>
                    <Typography variant="body2" sx={{ fontWeight: 600, color: "text.primary" }}>
                      {patient.name}
                    </Typography>
                  </Box>
                </TableCell>

                <TableCell sx={{ color: "text.secondary" }}>
                  {patient.age}
                </TableCell>

                <TableCell sx={{ fontWeight: 600, fontVariantNumeric: "tabular-nums" }}>
                  {patient.glucose}
                </TableCell>

                <TableCell sx={{ fontVariantNumeric: "tabular-nums" }}>
                  {patient.inventory} Units
                </TableCell>

                <TableCell>
                  <Chip
                    size="small"
                    sx={{ fontWeight: 600 }}
                    label={patient.status}
                    color={
                      patient.status === "Stable"
                        ? "success"
                        : "error"
                    }
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};

export default PatientsPage;
