import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import ProtectedRoute from "./ProtectedRoute";

const mockUseAuth = vi.fn();

vi.mock("../context/AuthContext", () => ({
  useAuth: () => mockUseAuth(),
}));

vi.mock("react-router-dom", () => ({
  Navigate: ({
    to,
  }: {
    to: string;
  }) => (
    <div>
      Redirect:{to}
    </div>
  ),
}));

describe("ProtectedRoute", () => {
  it("should redirect unauthenticated users", () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: false,
    });

    render(
      <ProtectedRoute>
        <div>Protected</div>
      </ProtectedRoute>
    );

    expect(
      screen.getByText(
        "Redirect:/login"
      )
    ).toBeTruthy();
  });

  it("should render children when authenticated", () => {
    mockUseAuth.mockReturnValue({
      isAuthenticated: true,
    });

    render(
      <ProtectedRoute>
        <div>Protected Content</div>
      </ProtectedRoute>
    );

    expect(
      screen.getByText(
        "Protected Content"
      )
    ).toBeTruthy();
  });
});