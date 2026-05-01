import { colors } from "@/theme/colors";
import Svg, { Circle, Polyline } from "react-native-svg";
import { View } from "react-native";

export function Sparkline({ data, color = colors.accent, height = 120 }: { data: number[]; color?: string; height?: number }) {
  const w = 320;
  const h = 100;
  const max = Math.max(...data);
  const min = Math.min(...data);
  const range = max - min || 1;

  const points = data
    .map((d, i) => {
      const x = (i / (data.length - 1)) * w;
      const y = h - ((d - min) / range) * h;
      return `${x},${y}`;
    })
    .join(" ");

  return (
    <View>
      <Svg width="100%" height={height} viewBox={`0 0 ${w} ${h}`}>
        <Polyline points={points} fill="none" stroke={color} strokeWidth={3} strokeLinecap="round" strokeLinejoin="round" />
        {data.map((d, i) => {
          const x = (i / (data.length - 1)) * w;
          const y = h - ((d - min) / range) * h;
          return <Circle key={i} cx={x} cy={y} r={3} fill={color} />;
        })}
      </Svg>
    </View>
  );
}
