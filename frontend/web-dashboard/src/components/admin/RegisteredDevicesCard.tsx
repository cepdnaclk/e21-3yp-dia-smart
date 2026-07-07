import { Card, CardContent, Typography } from "@mui/material";

const RegisteredDevicesCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Registered Devices
        </Typography>
        
        {/* TODO: Integrate hardware devices count statistics api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Total registered dosing units, inventory scales, and storage monitoring sensors currently online.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Count: --]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default RegisteredDevicesCard;
