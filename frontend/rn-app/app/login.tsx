import { useAuth } from "@/state/authContext";
import { colors } from "@/theme/colors";
import { Redirect, router } from "expo-router";
import { useState } from "react";
import { Pressable, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

export default function LoginScreen() {
  const { user, login } = useAuth();
  const [role, setRole] = useState<"caregiver" | "doctor" | "patient">("caregiver");
  const [email, setEmail] = useState("caregiver@diasmart.com");
  const [password, setPassword] = useState("Care1234");
  const [error, setError] = useState("");

  if (user) {
    return <Redirect href="/(tabs)/home" />;
  }

  const onLogin = async () => {
    const result = await login(email, password, role);
    if (!result.ok) {
      setError(result.message || "Login failed");
      return;
    }
    setError("");
    router.replace("/(tabs)/home");
  };

  return (
    <SafeAreaView style={styles.safe}>
      <View style={styles.card}>
        <Text style={styles.brand}>Dia-Smart</Text>
        <Text style={styles.title}>Welcome Back</Text>
        <Text style={styles.subtitle}>Sign in to continue</Text>

        <Text style={styles.label}>Role</Text>
        <View style={styles.roleRow}>
          {["caregiver", "doctor", "patient"].map((item) => (
            <Pressable
              key={item}
              onPress={() => setRole(item as typeof role)}
              style={[styles.rolePill, role === item && styles.rolePillActive]}
            >
              <Text style={[styles.roleText, role === item && styles.roleTextActive]}>{item}</Text>
            </Pressable>
          ))}
        </View>

        <Text style={styles.label}>Email</Text>
        <TextInput value={email} onChangeText={setEmail} autoCapitalize="none" style={styles.input} placeholder="you@example.com" />

        <Text style={styles.label}>Password</Text>
        <TextInput value={password} onChangeText={setPassword} secureTextEntry style={styles.input} placeholder="Password" />

        {!!error && <Text style={styles.error}>{error}</Text>}

        <Pressable onPress={onLogin} style={styles.btn}>
          <Text style={styles.btnText}>Login Securely</Text>
        </Pressable>

        <Pressable onPress={() => router.push("/signup")}>
          <Text style={styles.link}>Need an account? Create one</Text>
        </Pressable>
      </View>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg, justifyContent: "center", padding: 16 },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 18,
    gap: 10
  },
  brand: { color: colors.accent, fontWeight: "700", letterSpacing: 1 },
  title: { fontSize: 28, fontWeight: "700", color: colors.text },
  subtitle: { color: colors.muted, marginBottom: 8 },
  label: { color: colors.text, fontWeight: "600", marginTop: 2 },
  input: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 12,
    backgroundColor: "#FAFBFF",
    paddingHorizontal: 12,
    paddingVertical: 10
  },
  roleRow: { flexDirection: "row", gap: 8 },
  rolePill: {
    borderWidth: 1,
    borderColor: colors.line,
    borderRadius: 999,
    paddingVertical: 7,
    paddingHorizontal: 12,
    backgroundColor: colors.surfaceAlt
  },
  rolePillActive: { backgroundColor: colors.accent, borderColor: colors.accent },
  roleText: { color: colors.muted, textTransform: "capitalize", fontWeight: "600" },
  roleTextActive: { color: "#fff" },
  btn: {
    marginTop: 8,
    backgroundColor: colors.accent,
    borderRadius: 12,
    alignItems: "center",
    paddingVertical: 12
  },
  btnText: { color: "#fff", fontWeight: "700" },
  link: { marginTop: 10, textAlign: "center", color: colors.accent, fontWeight: "600" },
  error: { color: colors.danger, fontWeight: "600" }
});
