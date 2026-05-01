import { useAuth } from "@/state/authContext";
import { colors } from "@/theme/colors";
import { router } from "expo-router";
import { useState } from "react";
import { Pressable, ScrollView, StyleSheet, Text, TextInput, View } from "react-native";
import { SafeAreaView } from "react-native-safe-area-context";

export default function SignupScreen() {
  const { signup } = useAuth();
  const [fullName, setFullName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [role, setRole] = useState<"caregiver" | "doctor" | "patient">("caregiver");
  const [error, setError] = useState("");

  const onSignup = async () => {
    if (password !== confirmPassword) {
      setError("Passwords do not match");
      return;
    }

    const result = await signup({ fullName, email, password, role });
    if (!result.ok) {
      setError(result.message || "Signup failed");
      return;
    }

    setError("");
    router.replace("/login");
  };

  return (
    <SafeAreaView style={styles.safe}>
      <ScrollView contentContainerStyle={styles.scroll}>
        <View style={styles.card}>
          <Text style={styles.brand}>Dia-Smart</Text>
          <Text style={styles.title}>Create Account</Text>

          <Text style={styles.label}>Full Name</Text>
          <TextInput value={fullName} onChangeText={setFullName} style={styles.input} placeholder="John Fernando" />

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
          <TextInput value={password} onChangeText={setPassword} secureTextEntry style={styles.input} placeholder="Minimum 8 characters" />

          <Text style={styles.label}>Confirm Password</Text>
          <TextInput value={confirmPassword} onChangeText={setConfirmPassword} secureTextEntry style={styles.input} placeholder="Repeat password" />

          {!!error && <Text style={styles.error}>{error}</Text>}

          <Pressable onPress={onSignup} style={styles.btn}>
            <Text style={styles.btnText}>Create Account</Text>
          </Pressable>

          <Pressable onPress={() => router.replace("/login")}>
            <Text style={styles.link}>Back to login</Text>
          </Pressable>
        </View>
      </ScrollView>
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: { flex: 1, backgroundColor: colors.bg },
  scroll: { padding: 16, justifyContent: "center", flexGrow: 1 },
  card: {
    backgroundColor: colors.surface,
    borderRadius: 20,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 18,
    gap: 10
  },
  brand: { color: colors.accent, fontWeight: "700", letterSpacing: 1 },
  title: { fontSize: 26, fontWeight: "700", color: colors.text, marginBottom: 6 },
  label: { color: colors.text, fontWeight: "600" },
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
