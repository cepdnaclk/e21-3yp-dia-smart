import { Card, CardContent, Typography, Box } from "@mui/material";
import { BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer } from "recharts";

const DeviceReportsSection = () => {
  const data = [
    { name: "Active", value: 34, color: "#2e7d32" },
    { name: "Warning", value: 4, color: "#ed6c02" },
    { name: "Offline", value: 2, color: "#d32f2f" }
  ];

  return (
    <Card elevation={2} sx={{ borderRadius: 3, height: "100%", display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ flexGrow: 1, display: "flex", flexDirection: "column", gap: 2 }}>
        <Box>
          <Typography variant="h6" sx={{ fontWeight: "bold" }}>
            Hardware Provisioning & Status
          </Typography>
          <Typography variant="body2" color="text.secondary">
            Operational statuses mapping active, warning, and offline hardware dosing units or storage sensors.
          </Typography>
        </Box>

        <Box sx={{ width: "100%", height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
              <XAxis dataKey="name" />
              <YAxis allowDecimals={false} />
              <Tooltip />
              <Bar dataKey="value" radius={[4, 4, 0, 0]}>
                {data.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default DeviceReportsSection;
