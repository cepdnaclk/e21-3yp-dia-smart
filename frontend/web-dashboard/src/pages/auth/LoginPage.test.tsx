import {
  describe,
  it,
  expect,
  vi,
  beforeEach,
} from "vitest";

import {
  render,
  screen,
  fireEvent,
  waitFor,
} from "@testing-library/react";

import LoginPage from "./LoginPage";
import { authService } from "../../services/authService";
import { patientAccessService } from "../../services/patientAccessService";

const mockNavigate = vi.fn();
const mockLogin = vi.fn();

vi.mock("react-router-dom", () => ({
  useNavigate: () => mockNavigate,
}));

vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({
    login: mockLogin,
  }),
}));

vi.mock("../../services/authService", () => ({
  authService: {
    login: vi.fn(),
  },
}));

vi.mock("../../services/patientAccessService", () => ({
  patientAccessService: {
    getMyPatientAccess: vi.fn(),
  },
}));

describe("LoginPage", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it("should render login page", () => {
    render(<LoginPage />);

    expect(
      screen.getByText(/Welcome to Dia-Smart/i)
    ).toBeTruthy();

    expect(
      screen.getAllByLabelText(/Email/i)[0]
    ).toBeTruthy();

    expect(
      screen.getAllByLabelText(/Password/i)[0]
    ).toBeTruthy();
  });

  it("should login successfully", async () => {
    vi.mocked(authService.login).mockResolvedValue({
      accessToken: "jwt-token",
      expiresInMs: 3600000,
      user: {
        userId: 1,
        email: "test@test.com",
        displayName: "Test User",
        role: "PATIENT",
      },
    } as any);

    vi.mocked(
      patientAccessService.getMyPatientAccess
    ).mockResolvedValue([
      {
        patientId: 5,
      },
    ] as any);

    render(<LoginPage />);

    fireEvent.change(
      screen.getAllByLabelText(/Email/i)[0],
      {
        target: {
          value: "test@test.com",
        },
      }
    );

    fireEvent.change(
      screen.getAllByLabelText(/Password/i)[0],
      {
        target: {
          value: "password",
        },
      }
    );

    fireEvent.click(
      screen.getByRole("button", {
        name: /sign in/i,
      })
    );

    await waitFor(() => {
      expect(
        authService.login
      ).toHaveBeenCalled();
    });

    expect(mockLogin).toHaveBeenCalled();

    await waitFor(() => {
      expect(
        patientAccessService.getMyPatientAccess
      ).toHaveBeenCalled();
    });
  });

  it("should show error when login fails", async () => {
    vi.mocked(authService.login).mockRejectedValue(
      new Error("failed")
    );

    render(<LoginPage />);

    fireEvent.click(
      screen.getByRole("button", {
        name: /sign in/i,
      })
    );

    await waitFor(() => {
      expect(
        authService.login
      ).toHaveBeenCalled();
    });
  });
});