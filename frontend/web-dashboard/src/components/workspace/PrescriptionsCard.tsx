import { useState, useEffect } from "react";
import { Card, CardContent, Typography, List, ListItem, ListItemText, Box, CircularProgress, Chip } from "@mui/material";
import { prescriptionsService } from "../../services/prescriptionsService";
import type { Prescription } from "../../types/prescription";

interface PrescriptionsCardProps {
  patientId: number;
}

const PrescriptionsCard = ({ patientId }: PrescriptionsCardProps) => {
  const [loading, setLoading] = useState(true);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchPrescriptions = async () => {
      try {
        const response = await prescriptionsService.getPrescriptions(patientId);
        setPrescriptions(response || []);
      } catch (err) {
        console.error("Failed to load prescriptions", err);
      } finally {
        setLoading(false);
      }
    };
    fetchPrescriptions();
  }, [patientId]);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Prescriptions
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4 }}>
            <CircularProgress />
          </Box>
        ) : prescriptions.length === 0 ? (
          <Typography color="text.secondary" sx={{ py: 2 }}>
            No active prescriptions for this patient
          </Typography>
        ) : (
          <List disablePadding sx={{ maxHeight: 300, overflowY: "auto" }}>
            {prescriptions.map((pres) => (
              <ListItem
                key={pres.prescriptionId}
                disableGutters
                secondaryAction={
                  <Chip
                    label={pres.active ? "Active" : "Inactive"}
                    color={pres.active ? "success" : "default"}
                    size="small"
                    variant="outlined"
                  />
                }
              >
                <ListItemText
                  primary={pres.prescriptionName}
                  secondary={`Regimen dates: ${new Date(pres.startDate).toLocaleDateString()} - ${new Date(pres.endDate).toLocaleDateString()} ${pres.notes ? `• Note: "${pres.notes}"` : ""}`}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
};

export default PrescriptionsCard;
