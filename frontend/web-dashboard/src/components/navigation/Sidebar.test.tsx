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
  }),
}));

describe("Sidebar", () => {
  const renderSidebar = () => render(<Sidebar mobileOpen={false} onClose={vi.fn()} />);

  it("renders Dashboard menu", () => {
    renderSidebar();

    expect(
      screen.getByText("Dashboard")
    ).toBeTruthy();
  });

  it("renders Alerts menu", () => {
    renderSidebar();

    expect(
      screen.getByText("Alerts")
    ).toBeTruthy();
  });

  it("renders Devices menu", () => {
    renderSidebar();

    expect(
      screen.getByText("Devices")
    ).toBeTruthy();
  });

  it("renders Care Team menu", () => {
    renderSidebar();

    expect(
      screen.getByText("Care Team")
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

    expect(
      screen.queryByText("Settings")
    ).toBeNull();
  });
});
