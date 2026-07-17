import { Card, CardContent, Typography, Box } from "@mui/material";
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";

const UsageReportsSection = () => {
  const data = [
    { date: "Jul 04", volume: 1200 },
    { date: "Jul 05", volume: 1500 },
    { date: "Jul 06", volume: 1100 },
    { date: "Jul 07", volume: 1800 },
    { date: "Jul 08", volume: 2200 },
    { date: "Jul 09", volume: 2100 },
    { date: "Jul 10", volume: 2500 }
  ];

  return (
    <Card elevation={2} sx={{ borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: "bold" }}>
            Platform Ingestion Volume & Activity
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Daily telemetry ingestion message records processed by the ingestion gateways.
          </Typography>
        </Box>

        <Box sx={{ width: "100%", height: 300 }}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
              <CartesianGrid strokeDasharray="3 3" vertical={false} />
              <XAxis dataKey="date" />
              <YAxis />
              <Tooltip />
              <Area type="monotone" dataKey="volume" stroke="#9c27b0" fill="#9c27b0" fillOpacity={0.15} strokeWidth={2.5} name="Telemetry Records" />
            </AreaChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default UsageReportsSection;
