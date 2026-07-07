import { Card, CardContent, Typography } from "@mui/material";

const DoseHistoryCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Dose History
        </Typography>

        {/* TODO: Integrate historical dose events and log verification APIs */}
        <Typography variant="body2" color="text.secondary">
          Historical log of all verified insulin dose administrations, displaying exact times, dosage units, and logging source (doser/manual override).
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DoseHistoryCard;
