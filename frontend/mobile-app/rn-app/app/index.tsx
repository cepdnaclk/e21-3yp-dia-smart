import { useAuth } from "@/state/authContext";
import { Redirect } from "expo-router";

export default function IndexScreen() {
  const { user } = useAuth();
  if (user) return <Redirect href="/(tabs)/home" />;
  return <Redirect href="/login" />;
}
