import { Card, CardContent, Typography, Box } from "@mui/material";

const RecentActivity = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Recent Activity
        </Typography>

        {/* TODO: Integrate with audit logs and patient logs APIs */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          This card will show recent activities, updates, and events related to your assigned patients, such as newly registered devices, logged doses, and report generations.
        </Typography>

        <Box sx={{ p: 2, bgcolor: "action.hover", borderRadius: 1 }}>
          <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
            [Placeholder: Chronological activity timeline feed]
          </Typography>
        </Box>
      </CardContent>
    </Card>
  );
};

export default RecentActivity;
