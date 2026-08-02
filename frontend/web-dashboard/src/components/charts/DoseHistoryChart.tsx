import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";

import type { DoseReading } from "../../types/analytics";

interface Props {
  data: DoseReading[];
}

const DoseHistoryChart = ({ data }: Props) => {
  const chartData = data.map((item) => ({
    time: new Date(item.injectedAt).toLocaleDateString(),
    dose: item.doseUnits,
  }));

  return (
    <ResponsiveContainer
      width="100%"
      height={300}
    >
      <AreaChart data={chartData} margin={{ top: 10, right: 10, left: -5, bottom: 0 }}>
        <defs>
          <linearGradient id="colorDose" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#3ec1fa" stopOpacity={0.2} />
            <stop offset="95%" stopColor="#3ec1fa" stopOpacity={0.0} />
          </linearGradient>
        </defs>

        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />

        <XAxis
          dataKey="time"
          stroke="#94a3b8"
          fontSize={11}
          tickLine={false}
          axisLine={false}
          dy={10}
        />

        <YAxis
          stroke="#94a3b8"
          fontSize={11}
          tickLine={false}
          axisLine={false}
          dx={-10}
        />

        <Tooltip
          contentStyle={{
            backgroundColor: "#12233b",
            borderRadius: 8,
            border: "none",
            color: "#ffffff",
            boxShadow: "0 4px 12px rgba(0,0,0,0.1)",
          }}
          labelStyle={{ fontWeight: "bold", color: "#3ec1fa" }}
        />

        <Area
          type="monotone"
          dataKey="dose"
          stroke="#3ec1fa"
          strokeWidth={3}
          fillOpacity={1}
          fill="url(#colorDose)"
          activeDot={{ r: 6, stroke: "#ffffff", strokeWidth: 2, fill: "#3ec1fa" }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default DoseHistoryChart;