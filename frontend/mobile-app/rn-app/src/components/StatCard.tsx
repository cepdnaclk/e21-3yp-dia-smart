import { colors } from "@/theme/colors";
import { StyleSheet, Text, View } from "react-native";

export interface StatCardProps {
  label: string;
  value: string;
  statusText?: string;
  statusColor?: string;
  accentColor?: string;
  hint?: string;
}

export function StatCard({
  label,
  value,
  statusText,
  statusColor = "#10b981",
  accentColor,
  hint,
}: StatCardProps) {
  return (
    <View style={styles.card}>
      {!!accentColor && <View style={[styles.accentBar, { backgroundColor: accentColor }]} />}
      <View style={styles.cardContent}>
        <Text style={styles.label}>{label}</Text>
        <Text style={styles.value}>{value}</Text>
        {!!statusText && (
          <View style={styles.statusRow}>
            <View style={[styles.dot, { backgroundColor: statusColor }]} />
            <Text style={styles.statusText}>{statusText}</Text>
          </View>
        )}
        {!!hint && <Text style={styles.hint}>{hint}</Text>}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    flex: 1,
    minWidth: 140,
    backgroundColor: colors.surface,
    borderRadius: 16,
    borderWidth: 1,
    borderColor: colors.line,
    overflow: "hidden",
    shadowColor: "#000",
    shadowOffset: { width: 0, height: 2 },
    shadowOpacity: 0.05,
    shadowRadius: 6,
    elevation: 2,
  },
  accentBar: {
    height: 4,
    width: "100%",
  },
  cardContent: {
    padding: 14,
    gap: 6,
  },
  label: {
    fontSize: 12,
    fontWeight: "700",
    color: colors.muted,
    textTransform: "uppercase",
    letterSpacing: 0.5,
  },
  value: {
    fontSize: 24,
    fontWeight: "800",
    color: colors.text,
    letterSpacing: -0.5,
  },
  statusRow: {
    flexDirection: "row",
    alignItems: "center",
    gap: 6,
    marginTop: 2,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  statusText: {
    fontSize: 12,
    fontWeight: "600",
    color: colors.muted,
  },
  hint: {
    color: colors.muted,
    fontSize: 12,
  },
});
