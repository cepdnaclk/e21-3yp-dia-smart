import AsyncStorage from "@react-native-async-storage/async-storage";

export const AUTH_USER_KEY = "diasmartCurrentUser";
export const CUSTOM_USERS_KEY = "diasmartCustomUsers";

export async function readFromStorage<T>(key: string, fallback: T): Promise<T> {
  try {
    const raw = await AsyncStorage.getItem(key);
    if (!raw) return fallback;
    return JSON.parse(raw) as T;
  } catch {
    return fallback;
  }
}

export async function writeToStorage<T>(key: string, value: T): Promise<void> {
  await AsyncStorage.setItem(key, JSON.stringify(value));
}

export async function removeFromStorage(key: string): Promise<void> {
  await AsyncStorage.removeItem(key);
}
