import { describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  fireEvent,
} from "@testing-library/react";

import Topbar from "./Topbar";

const navigateMock = vi.fn();
const logoutMock = vi.fn();

vi.mock("react-router-dom", () => ({
  useNavigate: () => navigateMock,
}));

vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({
    role: "PATIENT",
    logout: logoutMock,
  }),
}));

vi.mock(
  "../../assets/logo/diasmart-logo.png",
  () => ({
    default: "logo.png",
  })
);

describe("Topbar", () => {
  it("renders application title", () => {
    render(<Topbar />);

    expect(
      screen.getByText("Dia-Smart")
    ).toBeTruthy();
  });

  it("renders Home button", () => {
    render(<Topbar />);

    expect(
      screen.getByText("Home")
    ).toBeTruthy();
  });

  it("renders Logout button", () => {
    render(<Topbar />);

    expect(
      screen.getByText("Logout")
    ).toBeTruthy();
  });

  it("logout button calls logout", () => {
    render(<Topbar />);

    fireEvent.click(
      screen.getByText("Logout")
    );

    expect(logoutMock)
      .toHaveBeenCalled();
  });
});