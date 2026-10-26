import { Box, Typography } from "@mui/material";
import MonitorHeartIcon from "@mui/icons-material/MonitorHeart";

import MetricCard from "../common/MetricCard";

interface SystemStatusCardProps {
  status: string;
}

const SystemStatusCard: React.FC<SystemStatusCardProps> = ({ status }) => {
  return (
    <MetricCard
      label="System Status"
      tone="success"
      icon={<MonitorHeartIcon />}
      value={
        <Box sx={{ display: "flex", alignItems: "center", gap: 1 }}>
          <Box
            sx={{
              width: 10,
              height: 10,
              borderRadius: "50%",
              bgcolor: "success.main",
              boxShadow: "0 0 0 4px rgba(16, 185, 129, 0.18)",
            }}
          />
          <Typography variant="h5" component="span" sx={{ fontWeight: 800, color: "success.dark" }}>
            {status}
          </Typography>
        </Box>
      }
      description="Overall system health monitoring cloud server service status, datastore latency, and device API integrations."
    />
  );
};

export default SystemStatusCard;
