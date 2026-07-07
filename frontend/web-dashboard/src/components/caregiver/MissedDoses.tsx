import { Card, CardContent, Typography } from "@mui/material";

const MissedDoses = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Missed Doses
        </Typography>

        {/* TODO: Integrate schedule compliance api for skipped/delayed insulin dose alerts */}
        <Typography variant="body2" color="text.secondary">
          Alerts for skipped or delayed insulin administrations today. Allows caregivers to contact patients or schedule reminders.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default MissedDoses;
