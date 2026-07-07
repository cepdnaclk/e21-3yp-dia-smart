import { Card, CardContent, Typography } from "@mui/material";

const ActivePatientsCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Active Patients
        </Typography>
        
        {/* TODO: Integrate active patient sessions count statistics api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Number of patients actively using monitoring systems, dosing schedules, and caregiver support.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Count: --]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default ActivePatientsCard;
