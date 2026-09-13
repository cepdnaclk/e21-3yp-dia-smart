import { AppScaffold } from "@/components/AppScaffold";
import { StatCard } from "@/components/StatCard";
import { buildMetrics, sampleDashboardTelemetry, sampleReadings } from "@/data/mockData";
import {
  getGlucoseStatus,
  getInventoryStatus,
  getLastDoseStatus,
  getTemperatureStatus,
} from "@/utils/statusUtils";
import { colors } from "@/theme/colors";
import { Activity } from "lucide-react-native";
import { StyleSheet, Text, View } from "react-native";

export default function HomeTab() {
  const metrics = buildMetrics(sampleReadings);
  const telemetry = sampleDashboardTelemetry;

  const inventoryStatus = getInventoryStatus(
    telemetry.inventory,
    telemetry.estimatedRemainingPercent,
    telemetry.inventoryStatus
  );
  const temperatureStatus = getTemperatureStatus(
    telemetry.temperature,
    telemetry.temperatureStatus
  );
  const doseStatus = getLastDoseStatus(
    telemetry.lastDose,
    telemetry.lastDoseInjectedAt
  );
  const glucoseStatus = getGlucoseStatus(telemetry.glucose);

  return (
    <AppScaffold title="Patient Overview" subtitle="Live telemetry & patient dashboard">
      {/* Primary Storage & Dosage Telemetry Cards */}
      <View style={styles.row}>
        <StatCard
          label="Inventory"
          value={`${telemetry.inventory} g`}
          statusText={inventoryStatus.text}
          statusColor={inventoryStatus.color}
          accentColor="#f59e0b"
        />
        <StatCard
          label="Temperature"
          value={`${telemetry.temperature} °C`}
          statusText={temperatureStatus.text}
          statusColor={temperatureStatus.color}
          accentColor="#3ec1fa"
        />
      </View>

      <View style={styles.row}>
        <StatCard
          label="Last Dose"
          value={`${telemetry.lastDose} Units`}
          statusText={doseStatus.text}
          statusColor={doseStatus.color}
          accentColor="#10b981"
        />
        <StatCard
          label="Glucose"
          value={`${telemetry.glucose} mg/dL`}
          statusText={glucoseStatus.text}
          statusColor={glucoseStatus.color}
          accentColor="#ef4444"
        />
      </View>

      {/* Aggregate Dosing Metrics */}
      <View style={styles.row}>
        <StatCard
          label="Average Glucose"
          value={`${metrics.avg.toFixed(1)} mg/dL`}
          accentColor="#8b5cf6"
        />
        <StatCard
          label="Time In Range"
          value={`${metrics.inRangePct}%`}
          accentColor="#10b981"
        />
      </View>

      {/* Risk Zones Breakdown */}
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
