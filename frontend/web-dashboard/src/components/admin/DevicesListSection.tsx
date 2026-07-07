import { Card, CardContent, Typography } from "@mui/material";

const DevicesListSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Registered Devices Registry
        </Typography>
        
        {/* TODO: Integrate medical/hardware devices registry query endpoints */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Catalog of all hardware devices (dosing remind boxes, smart weighing scales, cooling temperature monitors) provisioned in the system.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Table: Registered Devices List]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DevicesListSection;
