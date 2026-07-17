import { Card, CardContent, Typography } from "@mui/material";

interface RegisteredDevicesCardProps {
  count: number;
}

const RegisteredDevicesCard: React.FC<RegisteredDevicesCardProps> = ({ count }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
        <Typography variant="subtitle2" color="text.secondary">
          Registered Devices
        </Typography>
        <Typography variant="h4" sx={{ fontWeight: "bold", color: "primary.main" }}>
          {count}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Total registered dosing units, inventory scales, and storage monitoring sensors currently online.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default RegisteredDevicesCard;
