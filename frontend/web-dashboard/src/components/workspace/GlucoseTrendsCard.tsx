import { Card, CardContent, Typography } from "@mui/material";

const GlucoseTrendsCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Glucose Trends
        </Typography>

        {/* TODO: Integrate glucose trend charts and daily logs APIs */}
        <Typography variant="body2" color="text.secondary">
          Graphical representations of glucose levels (mg/dL) over 7-day, 14-day, or custom intervals, showing hypes and hypos thresholds, will be visualised here.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default GlucoseTrendsCard;
