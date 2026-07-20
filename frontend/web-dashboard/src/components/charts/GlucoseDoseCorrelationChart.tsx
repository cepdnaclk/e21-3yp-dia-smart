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
      <ComposedChart data={data} margin={{ top: 20, right: 20, left: -20, bottom: 0 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" vertical={false} />
        
        <XAxis
          dataKey="date"
          stroke="#94a3b8"
          fontSize={11}
          tickLine={false}
          axisLine={false}
          dy={10}
        />
        
        <YAxis
          yAxisId="left"
          stroke="#94a3b8"
          fontSize={11}
          tickLine={false}
          axisLine={false}
          dx={-10}
        />
        
        <YAxis
          yAxisId="right"
          orientation="right"
          stroke="#94a3b8"
          fontSize={11}
          tickLine={false}
          axisLine={false}
          dx={10}
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
        
        <Legend verticalAlign="top" height={36} iconType="circle" />
        
        {/* Horizontal Target Range boundaries */}
        <ReferenceLine 
          yAxisId="left" 
          y={targetMin} 
          stroke="#eab308" 
          strokeDasharray="5 5" 
          label={{ value: `Min (${targetMin})`, fill: '#eab308', fontSize: 10, position: 'insideBottomLeft' }} 
        />
        <ReferenceLine 
          yAxisId="left" 
          y={targetMax} 
          stroke="#eab308" 
          strokeDasharray="5 5" 
          label={{ value: `Max (${targetMax})`, fill: '#eab308', fontSize: 10, position: 'insideTopLeft' }} 
        />
        
        {/* Dose as columns */}
        <Bar yAxisId="right" dataKey="dose" name="Insulin Doses (Units)" fill="#3ec1fa" radius={[4, 4, 0, 0]} maxBarSize={30} opacity={0.8} />
        
        {/* Glucose as smooth curve */}
        <Line
          yAxisId="left"
          type="monotone"
          dataKey="glucose"
          name="Avg Glucose (mg/dL)"
          stroke="#ef4444"
          strokeWidth={3}
          dot={{ r: 4, stroke: "#ffffff", strokeWidth: 1.5, fill: "#ef4444" }}
          activeDot={{ r: 6, stroke: "#ffffff", strokeWidth: 2, fill: "#ef4444" }}
        />
      </ComposedChart>
    </ResponsiveContainer>
  );
};

export default GlucoseDoseCorrelationChart;
