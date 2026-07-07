import { Card, CardContent, Typography, Box } from "@mui/material";

const CriticalAlerts = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Critical Alerts
        </Typography>

        {/* TODO: Integrate with alerts API to display real-time medical and equipment alerts */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Critical alerts requiring immediate attention from the doctor (e.g., hyperglycemia events, battery alerts, or critical dose updates) will be displayed here in real time.
        </Typography>

        <Box sx={{ p: 2, bgcolor: "error.light", borderRadius: 1, opacity: 0.8 }}>
          <Typography variant="caption" color="error.dark" sx={{ display: "block", fontWeight: "medium" }}>
            [Placeholder: Live alerts feed and actions to acknowledge/resolve alerts]
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default CriticalAlerts;
