import { Card, CardContent, Typography } from "@mui/material";

const RecentActivity = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Recent Activity
        </Typography>

        {/* TODO: Integrate caregiver recent logs and alerts history APIs */}
        <Typography variant="body2" color="text.secondary">
          A chronological log of all recent actions, administered doses, and device updates registered for patients under your caregiver status.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default RecentActivity;
