import { useEffect, useState } from "react";

import {
  Typography,
  Paper,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Chip,
} from "@mui/material";

import { prescriptionsService } from "../../services/prescriptionsService";
import type { Prescription } from "../../types/prescription";

const PrescriptionsPage = () => {
  const [prescriptions, setPrescriptions] = useState<
    Prescription[]
  >([]);

  useEffect(() => {
    const loadPrescriptions = async () => {
      const data =
        await prescriptionsService.getPrescriptions();

      setPrescriptions(data);
    };

    loadPrescriptions();
  }, []);

  return (
    <>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Prescriptions
      </Typography>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>Medication</TableCell>
              <TableCell>Dosage</TableCell>
              <TableCell>Frequency</TableCell>
              <TableCell>Status</TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {prescriptions.map((item) => (
              <TableRow key={item.id}>
                <TableCell>
                  {item.medication}
                </TableCell>

                <TableCell>
                  {item.dosage}
                </TableCell>

                <TableCell>
                  {item.frequency}
                </TableCell>

                <TableCell>
                  <Chip
                    label={item.status}
                    color={
                      item.status === "Active"
                        ? "success"
                        : "default"
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

export default PrescriptionsPage;