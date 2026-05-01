import { colors } from "@/theme/colors";
import { SafeAreaView } from "react-native-safe-area-context";
import { ScrollView, StyleSheet, Text, View } from "react-native";

export function AppScaffold({
  title,
  subtitle,
  children,
  scroll = true
}: {
  title: string;
  subtitle?: string;
  children: React.ReactNode;
  scroll?: boolean;
}) {
  const content = (
    <View style={styles.inner}>
      <View style={styles.header}>
        <Text style={styles.title}>{title}</Text>
        {!!subtitle && <Text style={styles.subtitle}>{subtitle}</Text>}
      </View>
      {children}
    </View>
  );

  return (
    <SafeAreaView style={styles.safe}>
      {scroll ? <ScrollView contentContainerStyle={styles.scroll}>{content}</ScrollView> : content}
    </SafeAreaView>
  );
}

const styles = StyleSheet.create({
  safe: {
    flex: 1,
    backgroundColor: colors.bg
  },
  scroll: {
    paddingBottom: 16
  },
  inner: {
    paddingHorizontal: 16,
    paddingTop: 12,
    gap: 12
  },
  header: {
    backgroundColor: colors.surface,
    borderRadius: 18,
    borderWidth: 1,
    borderColor: colors.line,
    padding: 16
  },
  title: {
    fontSize: 24,
    fontWeight: "700",
    color: colors.text
  },
  subtitle: {
    marginTop: 4,
    color: colors.muted,
    fontSize: 14
  }
});
