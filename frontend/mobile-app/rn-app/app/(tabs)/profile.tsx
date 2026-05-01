import { AppScaffold } from "@/components/AppScaffold";
import { useAuth } from "@/state/authContext";
import { colors } from "@/theme/colors";
import { router } from "expo-router";
import { Pressable, StyleSheet, Text, View } from "react-native";

export default function ProfileTab() {
  const { user, demoUsers, logout } = useAuth();

  const onLogout = async () => {
    await logout();
    router.replace("/login");
  };

  return (
    <AppScaffold title="Profile" subtitle="Account and quick test credentials">
      <View style={styles.card}>
        <Text style={styles.name}>{user?.fullName || "User"}</Text>
        <Text style={styles.meta}>{user?.email}</Text>
        <Text style={styles.meta}>Role: {user?.role}</Text>

        <Pressable style={styles.logoutBtn} onPress={onLogout}>
          <Text style={styles.logoutText}>Sign Out</Text>
        </Pressable>
      </View>

      <View style={styles.card}>
        <Text style={styles.section}>Demo Accounts</Text>
        {demoUsers.map((item) => (
          <View key={item.email} style={styles.demoRow}>
            <Text style={styles.demoRole}>{item.role}</Text>
            <Text style={styles.demoCred}>{item.email} / {item.password}</Text>
          </View>
        ))}
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
    gap: 8
  },
  name: { fontSize: 24, fontWeight: "700", color: colors.text },
  meta: { color: colors.muted, fontSize: 14 },
  section: { fontSize: 16, fontWeight: "700", color: colors.text, marginBottom: 6 },
  demoRow: { gap: 2, paddingVertical: 5 },
  demoRole: { textTransform: "capitalize", color: colors.accent, fontWeight: "700" },
  demoCred: { color: colors.muted, fontSize: 13 },
  logoutBtn: {
    marginTop: 8,
    backgroundColor: colors.danger,
    borderRadius: 12,
    paddingVertical: 11,
    alignItems: "center"
  },
  logoutText: { color: "#fff", fontWeight: "700" }
});
