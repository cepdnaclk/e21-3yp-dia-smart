import { Card, CardContent, Typography } from "@mui/material";

interface ActivePatientsCardProps {
  count: number;
}

const ActivePatientsCard: React.FC<ActivePatientsCardProps> = ({ count }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
        <Typography variant="subtitle2" color="text.secondary">
          Active Patients
        </Typography>
        <Typography variant="h4" sx={{ fontWeight: "bold", color: "primary.main" }}>
          {count}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Number of patients actively using monitoring systems, dosing schedules, and caregiver support.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default ActivePatientsCard;
