import { describe, it, expect, vi } from "vitest";
import { render, screen } from "@testing-library/react";
import Sidebar from "./Sidebar";

const navigateMock = vi.fn();

vi.mock("react-router-dom", () => ({
  useNavigate: () => navigateMock,
  useLocation: () => ({
    pathname: "/dashboard",
  }),
}));

vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({
    role: "PATIENT",
    logout: vi.fn(),
  }),
}));

describe("Sidebar", () => {
  const renderSidebar = () => render(<Sidebar mobileOpen={false} onClose={vi.fn()} />);

  it("renders Dashboard menu", () => {
    renderSidebar();

    expect(
      screen.getAllByText("Dashboard")[0]
    ).toBeTruthy();
  });

  it("renders Alerts menu", () => {
    renderSidebar();

    expect(
      screen.getAllByText("Alerts")[0]
    ).toBeTruthy();
  });

  it("renders Devices menu", () => {
    renderSidebar();

    expect(
      screen.getAllByText("Devices")[0]
    ).toBeTruthy();
  });

  it("renders Care Team menu", () => {
    renderSidebar();

    expect(
      screen.getAllByText("Care Team")[0]
    ).toBeTruthy();
  });

  it("does not show removed Patient menu items", () => {
    renderSidebar();

    expect(
      screen.queryByText("Patients")
    ).toBeNull();

    expect(
      screen.queryByText("Profile")
    ).toBeNull();
  });
});
