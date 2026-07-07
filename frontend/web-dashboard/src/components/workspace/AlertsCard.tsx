import { Card, CardContent, Typography } from "@mui/material";

const AlertsCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Alerts
        </Typography>

        {/* TODO: Integrate alert settings and active notifications APIs for this patient */}
        <Typography variant="body2" color="text.secondary">
          Display of active alerts and notifications specific to this patient, including device battery, sensor errors, or inventory warning logs.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default AlertsCard;
