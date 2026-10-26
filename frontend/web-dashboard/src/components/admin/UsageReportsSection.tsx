import { Card, CardContent, Box } from "@mui/material";
import { AreaChart, Area, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from "recharts";
import InsightsOutlinedIcon from "@mui/icons-material/InsightsOutlined";
import CardHeading from "../common/CardHeading";
import {
  chartAxisProps,
  chartCursor,
  chartGridProps,
  chartTooltipContentStyle,
  chartTooltipItemStyle,
  chartTooltipLabelStyle,
} from "../charts/chartStyles";

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
    <Card>
      <CardContent sx={{ display: "flex", flexDirection: "column" }}>
        <CardHeading
          title="Platform Ingestion Volume & Activity"
          subtitle="Daily telemetry ingestion message records processed by the ingestion gateways."
          icon={<InsightsOutlinedIcon />}
          color="#0284c7"
          sx={{ alignItems: "flex-start" }}
        />

        <Box sx={{ width: "100%", height: 300 }}>
          <ResponsiveContainer width="100%" height="100%">
            <AreaChart data={data} margin={{ top: 10, right: 10, left: -5, bottom: 0 }}>
              <defs>
                <linearGradient id="adminUsageVolumeFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stopColor="#3ec1fa" stopOpacity={0.35} />
                  <stop offset="100%" stopColor="#3ec1fa" stopOpacity={0.02} />
                </linearGradient>
              </defs>
              <CartesianGrid {...chartGridProps} />
              <XAxis dataKey="date" {...chartAxisProps} />
              <YAxis {...chartAxisProps} />
              <Tooltip
                cursor={chartCursor}
                contentStyle={chartTooltipContentStyle}
                labelStyle={chartTooltipLabelStyle}
                itemStyle={chartTooltipItemStyle}
              />
              <Area type="monotone" dataKey="volume" stroke="#0284c7" fill="url(#adminUsageVolumeFill)" strokeWidth={2.5} name="Telemetry Records" />
            </AreaChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default UsageReportsSection;
