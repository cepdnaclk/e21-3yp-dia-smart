import { describe, it, expect, beforeEach } from "vitest";
import { render, screen, fireEvent } from "@testing-library/react";
import { AuthProvider, useAuth } from "./AuthContext";
import { UserRole } from "../types/roles";

const TestComponent = () => {
  const auth = useAuth();

  return (
    <>
      <div data-testid="authenticated">
        {String(auth.isAuthenticated)}
      </div>

      <div data-testid="role">
        {auth.role}
      </div>

      <button
        onClick={() =>
          auth.login(
            "jwt-token",
            UserRole.PATIENT,
            1
          )
        }
      >
        login
      </button>

      <button
        onClick={() => auth.logout()}
      >
        logout
      </button>
    </>
  );
};

describe("AuthContext", () => {
  beforeEach(() => {
    sessionStorage.clear();
  });

  it("should initialize from sessionStorage", () => {
    sessionStorage.setItem(
      "token",
      "existing-token"
    );

    sessionStorage.setItem(
      "role",
      UserRole.PATIENT
    );

    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    expect(
      screen.getByTestId(
        "authenticated"
      ).textContent
    ).toBe("true");
  });

  it("should login successfully", () => {
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    fireEvent.click(
      screen.getByText("login")
    );

    expect(
      sessionStorage.getItem("token")
    ).toBe("jwt-token");

    expect(
      screen.getByTestId(
        "authenticated"
      ).textContent
    ).toBe("true");
  });

  it("should logout successfully", () => {
    render(
      <AuthProvider>
        <TestComponent />
      </AuthProvider>
    );

    fireEvent.click(
      screen.getByText("login")
    );

    fireEvent.click(
      screen.getByText("logout")
    );

    expect(
      sessionStorage.getItem("token")
    ).toBeNull();

    expect(
      screen.getByTestId(
        "authenticated"
      ).textContent
    ).toBe("false");
  });
});