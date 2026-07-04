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
    <>
      <PageTitle>Prescriptions</PageTitle>

      <TableContainer component={Paper}>
        <Table>
          <TableHead>
            <TableRow>
              <TableCell>
                Prescription
              </TableCell>

              <TableCell>
                Start Date
              </TableCell>

              <TableCell>
                End Date
              </TableCell>

              <TableCell>
                Status
              </TableCell>

              <TableCell>
                Notes
              </TableCell>
            </TableRow>
          </TableHead>

          <TableBody>
            {prescriptions.length ===
            0 ? (
              <TableRow>
                <TableCell
                  colSpan={5}
                  align="center"
                >
                  No prescriptions
                  available
                </TableCell>
              </TableRow>
            ) : (
              prescriptions.map(
                (item) => (
                  <TableRow
                    key={
                      item.prescriptionId
                    }
                  >
                    <TableCell>
                      {
                        item.prescriptionName
                      }
                    </TableCell>

                    <TableCell>
                      {
                        item.startDate
                      }
                    </TableCell>

                    <TableCell>
                      {item.endDate}
                    </TableCell>

                    <TableCell>
                      <Chip
                        label={
                          item.active
                            ? "Active"
                            : "Inactive"
                        }
                        color={
                          item.active
                            ? "success"
                            : "default"
                        }
                      />
                    </TableCell>

                    <TableCell>
                      {item.notes ??
                        "No notes"}
                    </TableCell>
                  </TableRow>
                )
              )
            )}
          </TableBody>
        </Table>
      </TableContainer>
    </>
  );
};

export default PrescriptionsPage;
