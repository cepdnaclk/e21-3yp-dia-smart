import { Card, CardContent, Typography } from "@mui/material";

const PrescriptionsCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Prescriptions
        </Typography>

        {/* TODO: Integrate active prescriptions and medical logs APIs */}
        <Typography variant="body2" color="text.secondary">
          Details of active insulin prescriptions, current regimens, titration rules, and authorization status will be listed in this section.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default PrescriptionsCard;
