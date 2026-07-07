import { Card, CardContent, Typography } from "@mui/material";

const InventoryMonitoringCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Inventory Monitoring
        </Typography>

        {/* TODO: Integrate insulin cartridge weight scale sensor and stock APIs */}
        <Typography variant="body2" color="text.secondary">
          Remaining insulin stock. Tracks current cartridge weight, estimated remaining units, and average daily consumption logs.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default InventoryMonitoringCard;
