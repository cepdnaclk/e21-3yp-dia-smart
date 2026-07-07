import { Card, CardContent, Typography } from "@mui/material";

const PatientDetailsCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patient Details
        </Typography>

        {/* TODO: Integrate patient demographics and bio APIs */}
        <Typography variant="body2" color="text.secondary">
          Detailed information about the patient including contact details, primary care physician, caregiver relations, and diagnostic logs will be displayed here.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default PatientDetailsCard;
