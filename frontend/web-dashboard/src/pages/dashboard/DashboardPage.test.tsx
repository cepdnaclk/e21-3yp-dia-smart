import { describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import { BrowserRouter } from "react-router-dom";

import DashboardPage from "./DashboardPage";

vi.mock(
  "../../services/dashboardService",
  () => ({
    dashboardService: {
      getDashboardData: vi.fn(),
    },
  })
);

vi.mock(
  "../../services/analyticsService",
  () => ({
    analyticsService: {
      getGlucoseHistory: vi.fn().mockResolvedValue([]),
      getDoseHistory: vi.fn().mockResolvedValue([]),
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

    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

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

    render(
      <BrowserRouter>
        <DashboardPage />
      </BrowserRouter>
    );

    await waitFor(() => {
      expect(
        screen.getByText("Dashboard")
      ).toBeTruthy();
    });

    expect(
      screen.getByText("140 mg/dL")
    ).toBeTruthy();

    expect(
      screen.getByText("80 g")
    ).toBeTruthy();

    expect(
      screen.getByText("5 °C")
    ).toBeTruthy();
  });
});