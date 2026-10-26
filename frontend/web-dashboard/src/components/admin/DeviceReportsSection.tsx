import { Card, CardContent, Box } from "@mui/material";
import { BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer, CartesianGrid } from "recharts";
import RouterOutlinedIcon from "@mui/icons-material/RouterOutlined";
import CardHeading from "../common/CardHeading";
import {
  chartAxisProps,
  chartGridProps,
  chartTooltipContentStyle,
  chartTooltipItemStyle,
  chartTooltipLabelStyle,
} from "../charts/chartStyles";

const DeviceReportsSection = () => {
  const data = [
    { name: "Active", value: 34, color: "#10b981" },
    { name: "Warning", value: 4, color: "#f59e0b" },
    { name: "Offline", value: 2, color: "#ef4444" }
  ];

  return (
    <Card sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ flexGrow: 1, display: "flex", flexDirection: "column" }}>
        <CardHeading
          title="Hardware Provisioning & Status"
          subtitle="Operational statuses mapping active, warning, and offline hardware dosing units or storage sensors."
          icon={<RouterOutlinedIcon />}
          color="#0ea5e9"
          sx={{ alignItems: "flex-start" }}
        />

        <Box sx={{ width: "100%", height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <BarChart data={data} margin={{ top: 10, right: 10, left: -25, bottom: 0 }}>
              <CartesianGrid {...chartGridProps} />
              <XAxis dataKey="name" {...chartAxisProps} />
              <YAxis allowDecimals={false} {...chartAxisProps} />
              <Tooltip
                cursor={{ fill: "rgba(62, 193, 250, 0.08)" }}
                contentStyle={chartTooltipContentStyle}
                labelStyle={chartTooltipLabelStyle}
                itemStyle={chartTooltipItemStyle}
              />
              <Bar dataKey="value" radius={[6, 6, 0, 0]} maxBarSize={56}>
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
