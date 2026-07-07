import { Card, CardContent, Typography } from "@mui/material";

const RecentActivityCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Recent Activity
        </Typography>
        
        {/* TODO: Integrate administrative audit logs feed api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Timeline view of administrative activities, system status flags, device provisioning actions, and assignment updates.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Administrative Logs Feed]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default RecentActivityCard;
