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
  userId: number | null;
  login: (
    token: string,
    role: UserRole,
    userId: number
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
      sessionStorage.getItem("token")
    );

  const [userId, setUserId] = useState<number | null>(
    sessionStorage.getItem("userId")
      ? Number(sessionStorage.getItem("userId"))
      : null
  );

  // TODO: Extend role handling for Milestone 4 when doctor, caregiver, and admin sessions need role-specific capabilities.
  const [role, setRole] =
    useState<UserRole>(
      (sessionStorage.getItem(
        "role"
      ) as UserRole) ||
        UserRole.PATIENT
    );

  const [isAuthenticated, setIsAuthenticated] =
    useState(!!sessionStorage.getItem("token"));

  const login = (
    jwtToken: string,
    userRole: UserRole,
    userNumericId: number
  ) => {
    sessionStorage.setItem(
      "token",
      jwtToken
    );

    sessionStorage.setItem(
      "role",
      userRole
    );

    sessionStorage.setItem(
      "userId",
      userNumericId.toString()
    );

    setToken(jwtToken);
    setRole(userRole);
    setUserId(userNumericId);
    setIsAuthenticated(true);
  };

  const logout = () => {
    sessionStorage.removeItem("token");
    sessionStorage.removeItem("role");
    sessionStorage.removeItem("userId");

    setToken(null);
    setUserId(null);
    setIsAuthenticated(false);
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        role,
        token,
        userId,
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
