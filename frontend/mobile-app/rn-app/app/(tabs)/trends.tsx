import { AppScaffold } from "@/components/AppScaffold";
import { Sparkline } from "@/components/Sparkline";
import { sampleReadings } from "@/data/mockData";
import { colors } from "@/theme/colors";
import { StyleSheet, Text, View } from "react-native";

export default function TrendsTab() {
  const values = sampleReadings.map((item) => item.glucose_mg_dl);

  return (
    <AppScaffold title="Health Trends" subtitle="Glucose pattern over time">
      <View style={styles.card}>
        <Text style={styles.cardTitle}>Glucose Trend (mg/dL)</Text>
        <Sparkline data={values} />
      </View>

      <View style={styles.card}>
        <Text style={styles.cardTitle}>Daily Average Snapshot</Text>
        <View style={styles.bars}>
          {[130, 145, 152, 140, 128].map((value, index) => (
            <View key={index} style={styles.barCol}>
              <View style={[styles.bar, { height: value * 0.7 }]} />
              <Text style={styles.barLabel}>D{index + 1}</Text>
            </View>
          ))}
        </View>
      </View>
    </AppScaffold>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 14,
    gap: 10
  },
  cardTitle: {
    fontSize: 16,
    fontWeight: "700",
    color: colors.text
  },
  bars: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "flex-end",
    height: 140,
    paddingTop: 8
  },
  barCol: {
    alignItems: "center",
    gap: 6
  },
  bar: {
    width: 28,
    borderRadius: 8,
    backgroundColor: colors.accentSoft
  },
  barLabel: {
    color: colors.muted,
    fontSize: 12,
    fontWeight: "600"
  }
});
