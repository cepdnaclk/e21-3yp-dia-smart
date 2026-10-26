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

interface Props {
  data: {
    date: string;
    glucose: number;
  }[];
}

const GlucoseChart = ({ data }: Props) => {
  return (
    <ResponsiveContainer
      width="100%"
      height={300}
    >
      <AreaChart data={data} margin={{ top: 10, right: 10, left: -5, bottom: 0 }}>
        <defs>
          <linearGradient id="colorGlucose" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#ef4444" stopOpacity={0.24} />
            <stop offset="95%" stopColor="#ef4444" stopOpacity={0.0} />
          </linearGradient>
        </defs>

        <CartesianGrid {...chartGridProps} />

        <XAxis
          dataKey="date"
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
          dataKey="glucose"
          stroke="#ef4444"
          strokeWidth={2.5}
          fillOpacity={1}
          fill="url(#colorGlucose)"
          activeDot={{ r: 6, stroke: "#ffffff", strokeWidth: 2, fill: "#ef4444" }}
        />
      </AreaChart>
    </ResponsiveContainer>
  );
};

export default GlucoseChart;
