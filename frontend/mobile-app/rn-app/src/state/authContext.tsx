import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { AppUser, demoUsers } from "@/data/mockData";
import { AUTH_USER_KEY, CUSTOM_USERS_KEY, readFromStorage, removeFromStorage, writeToStorage } from "@/storage/authStorage";

type SignupPayload = {
  fullName: string;
  email: string;
  password: string;
  role: AppUser["role"];
};

type AuthContextType = {
  user: AppUser | null;
  loading: boolean;
  demoUsers: AppUser[];
  login: (email: string, password: string, role: AppUser["role"]) => Promise<{ ok: boolean; message?: string }>;
  signup: (payload: SignupPayload) => Promise<{ ok: boolean; message?: string }>;
  logout: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<AppUser | null>(null);
  const [loading, setLoading] = useState(true);
  const [customUsers, setCustomUsers] = useState<AppUser[]>([]);

  useEffect(() => {
    (async () => {
      const savedUser = await readFromStorage<AppUser | null>(AUTH_USER_KEY, null);
      const savedCustomUsers = await readFromStorage<AppUser[]>(CUSTOM_USERS_KEY, []);
      setUser(savedUser);
      setCustomUsers(savedCustomUsers);
      setLoading(false);
    })();
  }, []);

  const allUsers = useMemo(() => [...demoUsers, ...customUsers], [customUsers]);

  const login: AuthContextType["login"] = async (email, password, role) => {
    const normalized = email.trim().toLowerCase();
    const found = allUsers.find((item) => item.email.toLowerCase() === normalized && item.password === password && item.role === role);

    if (!found) {
      return { ok: false, message: "Invalid credentials" };
    }

    setUser(found);
    await writeToStorage(AUTH_USER_KEY, found);
    return { ok: true };
  };

  const signup: AuthContextType["signup"] = async ({ fullName, email, password, role }) => {
    const normalized = email.trim().toLowerCase();
    if (allUsers.some((item) => item.email.toLowerCase() === normalized)) {
      return { ok: false, message: "User already exists" };
    }

    const newUser: AppUser = { fullName: fullName.trim(), email: normalized, password, role };
    const updated = [...customUsers, newUser];
    setCustomUsers(updated);
    await writeToStorage(CUSTOM_USERS_KEY, updated);
    return { ok: true };
  };

  const logout = async () => {
    setUser(null);
    await removeFromStorage(AUTH_USER_KEY);
  };

  return (
    <AuthContext.Provider value={{ user, loading, demoUsers, login, signup, logout }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) {
    throw new Error("useAuth must be used within AuthProvider");
  }
  return ctx;
}
