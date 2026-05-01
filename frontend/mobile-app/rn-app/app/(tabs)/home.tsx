import { AppScaffold } from "@/components/AppScaffold";
import { StatCard } from "@/components/StatCard";
import { buildMetrics, sampleReadings } from "@/data/mockData";
import { colors } from "@/theme/colors";
import { Activity } from "lucide-react-native";
import { StyleSheet, Text, View } from "react-native";

export default function HomeTab() {
  const metrics = buildMetrics(sampleReadings);

  return (
    <AppScaffold title="Patient Overview" subtitle="App-style dashboard view">
      <View style={styles.row}>
        <StatCard label="Average" value={`${metrics.avg.toFixed(1)} mg/dL`} />
        <StatCard label="Latest" value={`${metrics.latest?.glucose_mg_dl ?? "-"} mg/dL`} />
      </View>

      <View style={styles.row}>
        <StatCard label="Readings" value={`${metrics.total}`} />
        <StatCard label="In Range" value={`${metrics.inRangePct}%`} />
      </View>

      <View style={styles.card}>
        <View style={styles.headRow}>
          <Activity color={colors.accent} size={18} />
          <Text style={styles.head}>Risk Zones</Text>
        </View>

        {[
          { key: "low", label: "Low", color: "#64A8FF" },
          { key: "normal", label: "Normal", color: colors.success },
          { key: "high", label: "High", color: colors.warning },
          { key: "critical", label: "Critical", color: colors.danger }
        ].map((zone) => {
          const value = metrics.zones[zone.key as keyof typeof metrics.zones] ?? 0;
          const width: `${number}%` = metrics.total
            ? `${Math.max(4, Math.round((value / metrics.total) * 100))}%`
            : "4%";
          return (
            <View key={zone.key} style={styles.zoneRow}>
              <Text style={styles.zoneLabel}>{zone.label}</Text>
              <View style={styles.track}>
                <View style={[styles.fill, { width, backgroundColor: zone.color }]} />
              </View>
              <Text style={styles.zoneCount}>{value}</Text>
            </View>
          );
        })}
      </View>
    </AppScaffold>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: "row",
    gap: 10
  },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 14,
    gap: 10
  },
  headRow: { flexDirection: "row", alignItems: "center", gap: 8 },
  head: { fontSize: 17, fontWeight: "700", color: colors.text },
  zoneRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 8
  },
  zoneLabel: { width: 58, color: colors.muted, fontWeight: "600" },
  zoneCount: { width: 22, color: colors.text, fontWeight: "700", textAlign: "right" },
  track: {
    flex: 1,
    height: 12,
    borderRadius: 99,
    backgroundColor: "#EDF1F8",
    overflow: "hidden"
  },
  fill: {
    height: "100%",
    borderRadius: 99
  }
});
