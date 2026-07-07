import { Card, CardContent, Typography } from "@mui/material";

const StorageWarnings = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Storage Warnings
        </Typography>

        {/* TODO: Integrate temperature sensor warning notifications and stock low warning APIs */}
        <Typography variant="body2" color="text.secondary">
          Alerts for storage device temperature anomalies and low inventory levels across monitored storage containers.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default StorageWarnings;
