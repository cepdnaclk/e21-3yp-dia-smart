import { Card, CardContent, Typography } from "@mui/material";

const DeviceReportsSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Hardware Inventory & Battery Status Reports
        </Typography>
        
        {/* TODO: Integrate administrative hardware devices analytics reports */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Statistical summary reports mapping device assignments counts, cell battery warnings, offline devices counts, and provisioning logs.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Registry: Device Inventory Reports]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DeviceReportsSection;
