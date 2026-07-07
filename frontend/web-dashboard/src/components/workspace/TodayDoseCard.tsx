import { Card, CardContent, Typography } from "@mui/material";

const TodayDoseCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Today's Dose
        </Typography>

        {/* TODO: Integrate with caregiver daily dosing scheduler and validation APIs */}
        <Typography variant="body2" color="text.secondary">
          Summary of today's insulin doses. Displays planned doses, administered events, and pending dose reminders for the current day.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default TodayDoseCard;
