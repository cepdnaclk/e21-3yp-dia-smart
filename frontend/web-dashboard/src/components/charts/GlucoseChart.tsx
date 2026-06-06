import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

const data = [
  { day: "Mon", glucose: 112 },
  { day: "Tue", glucose: 118 },
  { day: "Wed", glucose: 115 },
  { day: "Thu", glucose: 121 },
  { day: "Fri", glucose: 117 },
  { day: "Sat", glucose: 114 },
  { day: "Sun", glucose: 118 },
];

const GlucoseChart = () => {
  return (
    <ResponsiveContainer width="100%" height={300}>
      <LineChart data={data}>
        <XAxis dataKey="day" />
        <YAxis />
        <Tooltip />
        <Line
          type="monotone"
          dataKey="glucose"
          stroke="#4CB5E8"
          strokeWidth={3}
        />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default GlucoseChart;