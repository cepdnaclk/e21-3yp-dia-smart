import { Card, CardContent, Typography } from "@mui/material";

const TodayAlerts = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Today's Alerts
        </Typography>

        {/* TODO: Integrate caregiver alerts api for active warnings */}
        <Typography variant="body2" color="text.secondary">
          Display of active alerts and notifications specific to today, helping the caregiver monitor patient events in real-time.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default TodayAlerts;
