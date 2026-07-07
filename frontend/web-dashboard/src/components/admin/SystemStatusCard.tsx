import { Card, CardContent, Typography } from "@mui/material";

const SystemStatusCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          System Status
        </Typography>
        
        {/* TODO: Integrate server health metrics api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Overall system health indicator monitoring cloud service status, datastore latency, and device telemetry latency.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Status: --]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default SystemStatusCard;
