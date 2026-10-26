import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
  CartesianGrid,
} from "recharts";

import {
  chartAxisProps,
  chartCursor,
  chartGridProps,
  chartTooltipContentStyle,
  chartTooltipItemStyle,
  chartTooltipLabelStyle,
} from "./chartStyles";

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
            <stop offset="5%" stopColor="#3ec1fa" stopOpacity={0.24} />
            <stop offset="95%" stopColor="#3ec1fa" stopOpacity={0.0} />
          </linearGradient>
        </defs>

        <CartesianGrid {...chartGridProps} />

        <XAxis
          dataKey="time"
          {...chartAxisProps}
          dy={10}
        />

        <YAxis
          {...chartAxisProps}
          dx={-10}
        />

        <Tooltip
          contentStyle={chartTooltipContentStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          cursor={chartCursor}
        />

        <Area
          type="monotone"
          dataKey="dose"
          stroke="#3ec1fa"
          strokeWidth={2.5}
          fillOpacity={1}
          fill="url(#colorDose)"
          activeDot={{ r: 6, stroke: "#ffffff", strokeWidth: 2, fill: "#3ec1fa" }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default DoseHistoryChart;
