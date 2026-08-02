import { Card, CardContent, Typography, Box } from "@mui/material";

interface SystemStatusCardProps {
  status: string;
}

const SystemStatusCard: React.FC<SystemStatusCardProps> = ({ status }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
        <Typography variant="subtitle2" color="text.secondary">
          System Status
        </Typography>
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Box sx={{ width: 10, height: 10, borderRadius: "50%", bgcolor: "success.main" }} />
          <Typography variant="h5" sx={{ fontWeight: "bold", color: "success.main" }}>
            {status}
          </Typography>
        </Box>
        <Typography variant="body2" color="text.secondary">
          Overall system health monitoring cloud server service status, datastore latency, and device API integrations.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default SystemStatusCard;
