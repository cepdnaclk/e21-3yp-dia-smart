import { Card, CardContent, Box } from "@mui/material";
import { PieChart, Pie, Cell, Tooltip, Legend, ResponsiveContainer } from "recharts";
import DonutLargeOutlinedIcon from "@mui/icons-material/DonutLargeOutlined";
import CardHeading from "../common/CardHeading";
import {
  chartTooltipContentStyle,
  chartTooltipItemStyle,
  chartTooltipLabelStyle,
} from "../charts/chartStyles";

const UserReportsSection = () => {
  const data = [
    { name: "Patients", value: 45, color: "#0284c7" },
    { name: "Clinicians", value: 20, color: "#10b981" },
    { name: "Caregivers", value: 25, color: "#f59e0b" },
    { name: "Administrators", value: 10, color: "#12233b" }
  ];

  return (
    <Card sx={{ height: "100%", display: "flex", flexDirection: "column" }}>
      <CardContent sx={{ flexGrow: 1, display: "flex", flexDirection: "column" }}>
        <CardHeading
          title="User Account Distribution"
          subtitle="Proportion of registered system roles across patients, medical clinicians, caregiver mappings, and administrators."
          icon={<DonutLargeOutlinedIcon />}
          color="#0284c7"
          sx={{ alignItems: "flex-start" }}
        />

        <Box sx={{ width: "100%", height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <PieChart>
              <Pie
                data={data}
                cx="50%"
                cy="50%"
                innerRadius={60}
                outerRadius={80}
                paddingAngle={5}
                dataKey="value"
                stroke="#ffffff"
                strokeWidth={2}
              >
                {data.map((entry, index) => (
                  <Cell key={`cell-${index}`} fill={entry.color} />
                ))}
              </Pie>
              <Tooltip
                contentStyle={chartTooltipContentStyle}
                labelStyle={chartTooltipLabelStyle}
                itemStyle={chartTooltipItemStyle}
              />
              <Legend
                verticalAlign="bottom"
                height={36}
                iconType="circle"
                iconSize={8}
                wrapperStyle={{ fontSize: 12, fontWeight: 600, color: "#475569" }}
              />
            </PieChart>
          </ResponsiveContainer>
        </Box>
      </CardContent>
    </Card>
  );
};

export default UserReportsSection;
