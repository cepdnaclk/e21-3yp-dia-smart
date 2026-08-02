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

      <TableContainer component={Paper}>
        <Table>
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
              <TableRow key={patient.id}>
                <TableCell>
                  {patient.name}
                </TableCell>

                <TableCell>
                  {patient.age}
                </TableCell>

                <TableCell>
                  {patient.glucose}
                </TableCell>

                <TableCell>
                  {patient.inventory} Units
                </TableCell>

                <TableCell>
                  <Chip
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
