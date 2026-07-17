import {
  beforeEach,
  describe,
  it,
  expect,
  vi,
} from "vitest";
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
  beforeEach(() => {
    vi.clearAllMocks();
  });

  const renderTopbar = () => render(<Topbar onDrawerToggle={vi.fn()} />);

  it("renders application title", () => {
    renderTopbar();

    expect(
      screen.getByText("Dia-Smart")
    ).toBeTruthy();
  });

  it("renders Home button", () => {
    renderTopbar();

    expect(
      screen.getByText("Home")
    ).toBeTruthy();
  });

  it("renders Logout button", () => {
    renderTopbar();

    expect(
      screen.getByText("Logout")
    ).toBeTruthy();
  });

  it("logout button calls logout", () => {
    renderTopbar();

    fireEvent.click(
      screen.getByText("Logout")
    );

    expect(logoutMock)
      .toHaveBeenCalled();
  });

  it("settings icon navigates to settings", () => {
    renderTopbar();

    fireEvent.click(
      screen.getByLabelText("Settings")
    );

    expect(navigateMock)
      .toHaveBeenCalledWith("/settings");
  });

  it("profile avatar navigates to profile", () => {
    renderTopbar();

    fireEvent.click(
      screen.getByLabelText("Profile")
    );

    expect(navigateMock)
      .toHaveBeenCalledWith("/profile");
  });
});
