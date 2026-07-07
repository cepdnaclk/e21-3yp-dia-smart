import { Card, CardContent, Typography } from "@mui/material";

const StorageMonitoringCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Storage Monitoring
        </Typography>

        {/* TODO: Integrate storage compartment temperature sensor logs APIs */}
        <Typography variant="body2" color="text.secondary">
          Real-time temperature tracking for insulin storage. Alerts if temperature wanders outside the safe 2°C - 8°C boundaries.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default StorageMonitoringCard;
