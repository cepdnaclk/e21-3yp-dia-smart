import { Card, CardContent, Typography } from "@mui/material";

const DevicePatientSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Device ↔ Patient Associations
        </Typography>
        
        {/* TODO: Integrate device hardware profiling and patient configuration APIs */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Verify sensor calibrations, review telemetry streams, and manage inventory scale association keys for each patient account.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Registry: Device Patient Mappings]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DevicePatientSection;
