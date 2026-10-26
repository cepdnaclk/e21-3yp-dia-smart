import {
  ComposedChart,
  Line,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  ResponsiveContainer,
  CartesianGrid,
  ReferenceLine,
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
    glucose?: number;
    dose?: number;
  }[];
  targetMin: number;
  targetMax: number;
}

const GlucoseDoseCorrelationChart = ({ data, targetMin, targetMax }: Props) => {
  return (
    <ResponsiveContainer width="100%" height={350}>
      <ComposedChart data={data} margin={{ top: 20, right: 0, left: -10, bottom: 0 }}>
        <defs>
          <linearGradient id="correlationDoseBar" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stopColor="#3ec1fa" stopOpacity={0.95} />
            <stop offset="100%" stopColor="#0284c7" stopOpacity={0.75} />
          </linearGradient>
        </defs>

        <CartesianGrid {...chartGridProps} />

        <XAxis
          dataKey="date"
          {...chartAxisProps}
          dy={10}
        />

        <YAxis
          yAxisId="left"
          {...chartAxisProps}
          dx={-6}
        />

        <YAxis
          yAxisId="right"
          orientation="right"
          {...chartAxisProps}
          dx={6}
        />

        <Tooltip
          contentStyle={chartTooltipContentStyle}
          labelStyle={chartTooltipLabelStyle}
          itemStyle={chartTooltipItemStyle}
          cursor={chartCursor}
        />

        <Legend
          verticalAlign="top"
          height={36}
          iconType="circle"
          iconSize={8}
          wrapperStyle={{ fontSize: 12, fontWeight: 600, color: "#475569" }}
        />

        {/* Horizontal Target Range boundaries */}
        <ReferenceLine
          yAxisId="left"
          y={targetMin}
          stroke="#d97706"
          strokeDasharray="5 5"
          label={{ value: `Min (${targetMin})`, fill: '#b45309', fontSize: 10, fontWeight: 600, position: 'insideBottomLeft' }}
        />
        <ReferenceLine
          yAxisId="left"
          y={targetMax}
          stroke="#d97706"
          strokeDasharray="5 5"
          label={{ value: `Max (${targetMax})`, fill: '#b45309', fontSize: 10, fontWeight: 600, position: 'insideTopLeft' }}
        />

        {/* Dose as columns */}
        <Bar yAxisId="right" dataKey="dose" name="Insulin Doses (Units)" fill="url(#correlationDoseBar)" radius={[6, 6, 0, 0]} maxBarSize={28} />

        {/* Glucose as smooth curve */}
        <Line
          yAxisId="left"
          type="monotone"
          dataKey="glucose"
          name="Avg Glucose (mg/dL)"
          stroke="#ef4444"
          strokeWidth={2.5}
          dot={{ r: 3.5, stroke: "#ffffff", strokeWidth: 1.5, fill: "#ef4444" }}
          activeDot={{ r: 6, stroke: "#ffffff", strokeWidth: 2, fill: "#ef4444" }}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
};

export default GlucoseDoseCorrelationChart;
