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
  it("renders Dashboard menu", () => {
    render(<Sidebar />);

    expect(
      screen.getByText("Dashboard")
    ).toBeTruthy();
  });

  it("renders Alerts menu", () => {
    render(<Sidebar />);

    expect(
      screen.getByText("Alerts")
    ).toBeTruthy();
  });

  it("renders Devices menu", () => {
    render(<Sidebar />);

    expect(
      screen.getByText("Devices")
    ).toBeTruthy();
  });

  it("renders Care Team menu", () => {
    render(<Sidebar />);

    expect(
      screen.getByText("Care Team")
    ).toBeTruthy();
  });

  it("does not show removed Patient menu items", () => {
    render(<Sidebar />);

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
