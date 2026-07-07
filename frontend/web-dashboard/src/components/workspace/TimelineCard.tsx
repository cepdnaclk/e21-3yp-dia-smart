import { Card, CardContent, Typography } from "@mui/material";

const TimelineCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Timeline
        </Typography>

        {/* TODO: Integrate caregiver-patient audit activity feed logs APIs */}
        <Typography variant="body2" color="text.secondary">
          Activity feed log. Chronological view of recent sensor readings, administered doses, acknowledged alerts, and status changes.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default TimelineCard;
