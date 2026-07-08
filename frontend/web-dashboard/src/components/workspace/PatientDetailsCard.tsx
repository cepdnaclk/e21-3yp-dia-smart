import { Card, CardContent, Typography, Grid, Box } from "@mui/material";
import type { PatientProfileResponse } from "../../services/patientsService";

interface PatientDetailsCardProps {
  patientProfile: PatientProfileResponse;
}

const PatientDetailsCard = ({ patientProfile }: PatientDetailsCardProps) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 2, fontWeight: "medium" }}
        >
          Patient Details
        </Typography>

        <Grid container spacing={2}>
          <Grid size={{ xs: 6 }}>
            <Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: "block" }}
              >
                Gender
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                {patientProfile?.gender || "N/A"}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: "block" }}
              >
                Date of Birth
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                {patientProfile?.dateOfBirth || "N/A"}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: "block" }}
              >
                Contact Number
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                {patientProfile?.contactNumber || "N/A"}
              </Typography>
            </Box>
          </Grid>
          <Grid size={{ xs: 6 }}>
            <Box>
              <Typography
                variant="caption"
                color="text.secondary"
                sx={{ display: "block" }}
              >
                Emergency Contact
              </Typography>
              <Typography variant="body2" sx={{ fontWeight: 500 }}>
                {patientProfile?.emergencyContactNumber || "N/A"}
              </Typography>
            </Box>
          </Grid>
        </Grid>
      </CardContent>
    </Card>
  );
};

export default PatientDetailsCard;
