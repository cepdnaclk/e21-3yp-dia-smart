import { Card, CardContent, Typography } from "@mui/material";

const AnalyticsCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Analytics
        </Typography>

        {/* TODO: Integrate compliance graphs, average glucose, and time-in-range metrics */}
        <Typography variant="body2" color="text.secondary">
          Detailed analytical metrics, including time-in-range (TIR) percentages, glucose variability, and insulin-to-carb ratio predictions, will appear here.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default AnalyticsCard;
