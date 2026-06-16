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

  it("renders Profile menu", () => {
    render(<Sidebar />);

    expect(
      screen.getByText("Profile")
    ).toBeTruthy();
  });

  it("does not show Patients for PATIENT role", () => {
    render(<Sidebar />);

    expect(
      screen.queryByText("Patients")
    ).toBeNull();
  });
});