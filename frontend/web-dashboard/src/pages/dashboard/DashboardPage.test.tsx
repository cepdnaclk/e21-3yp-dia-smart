import { describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  waitFor,
} from "@testing-library/react";

import DashboardPage from "./DashboardPage";

vi.mock(
  "../../services/dashboardService",
  () => ({
    dashboardService: {
      getDashboardData: vi.fn(),
    },
  })
);

import { dashboardService } from "../../services/dashboardService";

describe("DashboardPage", () => {
  it("shows loading state", () => {
    vi.mocked(
      dashboardService.getDashboardData
    ).mockReturnValue(
      new Promise(() => {})
    );

    render(<DashboardPage />);

    expect(
      screen.getByText("Loading...")
    ).toBeTruthy();
  });

  it("renders dashboard data", async () => {
    vi.mocked(
      dashboardService.getDashboardData
    ).mockResolvedValue({
      glucose: 140,
      inventory: 80,
      temperature: 5,
      lastDose: 8,
    });

    render(<DashboardPage />);

    await waitFor(() => {
      expect(
        screen.getByText("Dashboard")
      ).toBeTruthy();
    });

    expect(
      screen.getByText("140 mg/dL")
    ).toBeTruthy();

    expect(
      screen.getByText("80 Units")
    ).toBeTruthy();

    expect(
      screen.getByText("5 °C")
    ).toBeTruthy();
  });
});