import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  Tooltip,
  ResponsiveContainer,
} from "recharts";

import type { DoseReading } from "../../types/analytics";

interface Props {
  data: DoseReading[];
}

const DoseHistoryChart = ({
  data,
}: Props) => {
  const chartData = data.map(
    (item) => ({
      time: new Date(
        item.injectedAt
      ).toLocaleDateString(),
      dose: item.doseUnits,
    })
  );

  return (
    <ResponsiveContainer
      width="100%"
      height={300}
    >
      <LineChart data={chartData}>
        <XAxis dataKey="time" />

        <YAxis />

        <Tooltip />

        <Line
          type="monotone"
          dataKey="dose"
          stroke="#4CAF50"
          strokeWidth={3}
        />
      </LineChart>
    </ResponsiveContainer>
  );
};

export default DoseHistoryChart;