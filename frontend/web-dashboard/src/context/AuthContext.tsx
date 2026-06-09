import {
  createContext,
  useContext,
  useState,
} from "react";

import { UserRole } from "../types/roles";

interface AuthContextType {
  isAuthenticated: boolean;
  role: UserRole;

  token: string | null;

  login: (
    token: string,
    role: UserRole
  ) => void;

  logout: () => void;
}

const AuthContext =
  createContext<AuthContextType | null>(
    null
  );

export const AuthProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [token, setToken] =
    useState<string | null>(
      localStorage.getItem("token")
    );

  const [role, setRole] =
    useState<UserRole>(
      (localStorage.getItem(
        "role"
      ) as UserRole) ||
        UserRole.PATIENT
    );

  const [isAuthenticated, setIsAuthenticated] =
    useState(!!localStorage.getItem("token"));

  const login = (
    jwtToken: string,
    userRole: UserRole
  ) => {
    localStorage.setItem(
      "token",
      jwtToken
    );

    localStorage.setItem(
      "role",
      userRole
    );

    setToken(jwtToken);
    setRole(userRole);
    setIsAuthenticated(true);
  };

  const logout = () => {
    localStorage.removeItem("token");
    localStorage.removeItem("role");

    setToken(null);
    setIsAuthenticated(false);
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        role,
        token,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context =
    useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside AuthProvider"
    );
  }

  return context;
};