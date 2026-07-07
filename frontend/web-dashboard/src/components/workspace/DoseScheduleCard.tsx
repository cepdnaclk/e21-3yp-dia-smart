import { Card, CardContent, Typography } from "@mui/material";

const DoseScheduleCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Dose Schedule
        </Typography>

        {/* TODO: Integrate schedule plans and adherence compliance trackers */}
        <Typography variant="body2" color="text.secondary">
          Display of the patient's daily dosing schedule (basal/bolus times) and timing configurations for insulin delivery reminders.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DoseScheduleCard;
