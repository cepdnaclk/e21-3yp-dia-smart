import { useEffect, useState } from "react";

import {
  Typography,
  Card,
  CardContent,
  Grid,
  Chip,
  Stack,
  CircularProgress,
  Alert,
} from "@mui/material";

import { patientAccessService } from "../../services/patientAccessService";
import type { PatientAccess } from "../../types/patientAccess";

const MyAccessPage = () => {
  const [accesses, setAccesses] =
    useState<PatientAccess[]>([]);

  const [loading, setLoading] =
    useState(true);

  const [error, setError] =
    useState("");

  useEffect(() => {
    const loadAccess =
      async () => {
        try {
          const data =
            await patientAccessService.getMyAccess();

          setAccesses(data);
        } catch (err) {
          console.error(err);

          setError(
            "Failed to load patient access information"
          );
        } finally {
          setLoading(false);
        }
      };

    loadAccess();
  }, []);

  if (loading) {
    return <CircularProgress />;
  }

  if (error) {
    return (
      <Alert severity="error">
        {error}
      </Alert>
    );
  }

  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        My Patient Access
      </Typography>

      <Stack spacing={3}>
        {accesses.map((access) => (
          <Card key={access.accessId}>
            <CardContent>
              <Grid container spacing={2}>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography>
                    <strong>
                      Access Role:
                    </strong>{" "}
                    {access.accessRole}
                  </Typography>
                </Grid>

                <Grid size={{ xs: 12, md: 6 }}>
                  <Chip
                    label={
                      access.status
                    }
                    color="success"
                  />
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Typography>
                    <strong>
                      Patient ID:
                    </strong>{" "}
                    {access.patientId}
                  </Typography>
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Typography>
                    <strong>
                      Relationship:
                    </strong>{" "}
                    {access.relationshipLabel ??
                      "N/A"}
                  </Typography>
                </Grid>

                <Grid size={{ xs: 12 }}>
                  <Stack
                    direction="row"
                    spacing={1}
                    flexWrap="wrap"
                  >
                    <Chip
                      label="Can View"
                      color={
                        access.canView
                          ? "success"
                          : "default"
                      }
                    />

                    <Chip
                      label="Can Acknowledge Alerts"
                      color={
                        access.canAcknowledgeAlerts
                          ? "success"
                          : "default"
                      }
                    />

                    <Chip
                      label="Can Edit Prescriptions"
                      color={
                        access.canEditPrescriptions
                          ? "success"
                          : "default"
                      }
                    />
                  </Stack>
                </Grid>
              </Grid>
            </CardContent>
          </Card>
        ))}
      </Stack>
    </>
  );
};

export default MyAccessPage;