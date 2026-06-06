import {
  createContext,
  useContext,
  useState,
} from "react";

import { UserRole } from "../types/roles";

interface AuthContextType {
  isAuthenticated: boolean;
  role: UserRole;

  login: (role: UserRole) => void;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | null>(
  null
);

export const AuthProvider = ({
  children,
}: {
  children: React.ReactNode;
}) => {
  const [isAuthenticated, setIsAuthenticated] =
    useState(true);

  const [role, setRole] =
    useState<UserRole>(UserRole.ADMIN);

  const login = (userRole: UserRole) => {
    setRole(userRole);
    setIsAuthenticated(true);
  };

  const logout = () => {
    setIsAuthenticated(false);
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        role,
        login,
        logout,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);

  if (!context) {
    throw new Error(
      "useAuth must be used inside AuthProvider"
    );
  }

  return context;
};