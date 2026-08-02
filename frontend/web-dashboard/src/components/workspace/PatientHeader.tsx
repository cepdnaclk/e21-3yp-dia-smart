import { Card, CardContent, Typography, Box } from "@mui/material";
import type { PatientProfileResponse } from "../../services/patientsService";

interface PatientHeaderProps {
  patientProfile: PatientProfileResponse;
}

const calculateAge = (dobString: string) => {
  if (!dobString) return "N/A";
  const dob = new Date(dobString);
  const diffMs = Date.now() - dob.getTime();
  const ageDate = new Date(diffMs);
  return Math.abs(ageDate.getUTCFullYear() - 1970);
};

const PatientHeader = ({ patientProfile }: PatientHeaderProps) => {
  return (
    <Card elevation={2} sx={{ borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h5" sx={{ fontWeight: "bold", mb: 1 }}>
          {patientProfile?.fullName || "Unknown Patient"}
        </Typography>

        <Box sx={{ display: "flex", flexWrap: "wrap", gap: 3 }}>
          <Typography variant="body2" color="text.secondary">
            <strong>Patient ID:</strong> {patientProfile?.patientId || "N/A"}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <strong>Age:</strong> {calculateAge(patientProfile?.dateOfBirth)}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <strong>Diabetes Type:</strong> {patientProfile?.diabetesType || "N/A"}
          </Typography>
          <Typography variant="body2" color="text.secondary">
            <strong>Current Status:</strong> Active
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default PatientHeader;
