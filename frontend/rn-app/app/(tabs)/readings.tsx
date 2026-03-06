import { AppScaffold } from "@/components/AppScaffold";
import { sampleReadings, zoneFromValue } from "@/data/mockData";
import { colors } from "@/theme/colors";
import { StyleSheet, Text, View } from "react-native";

const zoneColorMap = {
  low: "#64A8FF",
  normal: colors.success,
  high: colors.warning,
  critical: colors.danger
};

export default function ReadingsTab() {
  const latestFirst = [...sampleReadings].reverse();

  return (
    <AppScaffold title="Recent Readings" subtitle="Structured reading list like app records">
      <View style={styles.tableCard}>
        <View style={styles.rowHeader}>
          <Text style={[styles.headCell, styles.idCell]}>ID</Text>
          <Text style={[styles.headCell, styles.timeCell]}>Date & Time</Text>
          <Text style={[styles.headCell, styles.valueCell]}>Value</Text>
        </View>

        {latestFirst.map((item) => {
          const zone = zoneFromValue(item.glucose_mg_dl);
          return (
            <View key={item.record_id} style={styles.row}>
              <Text style={[styles.cell, styles.idCell]}>{item.record_id}</Text>
              <Text style={[styles.cell, styles.timeCell]}>{item.datetime_sl}</Text>
              <View style={[styles.badge, { backgroundColor: `${zoneColorMap[zone]}22` }]}>
                <Text style={[styles.badgeText, { color: zoneColorMap[zone] }]}>{item.glucose_mg_dl}</Text>
              </View>
            </View>
          );
        })}
      </View>
    </AppScaffold>
  );
}

const styles = StyleSheet.create({
  tableCard: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.line,
    overflow: "hidden"
  },
  rowHeader: {
    flexDirection: "row",
    borderBottomWidth: 1,
    borderBottomColor: colors.line,
    paddingVertical: 10,
    paddingHorizontal: 10,
    backgroundColor: "#F8FAFF"
  },
  row: {
    flexDirection: "row",
    alignItems: "center",
    borderBottomWidth: 1,
    borderBottomColor: "#EEF2FA",
    paddingVertical: 10,
    paddingHorizontal: 10
  },
  headCell: {
    color: colors.muted,
    fontWeight: "700",
    fontSize: 12,
    textTransform: "uppercase"
  },
  cell: {
    color: colors.text,
    fontSize: 12
  },
  idCell: { width: 42 },
  timeCell: { flex: 1 },
  valueCell: { width: 52, textAlign: "right" },
  badge: {
    width: 52,
    borderRadius: 10,
    paddingVertical: 5,
    alignItems: "center"
  },
  badgeText: {
    fontWeight: "700"
  }
});
