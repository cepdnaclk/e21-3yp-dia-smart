import { describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  waitFor,
} from "@testing-library/react";

import AlertsPage from "./AlertsPage";

vi.mock(
  "../../services/alertsService",
  () => ({
    alertsService: {
      getAlerts: vi.fn(),
    },
  })
);

vi.mock(
  "../../components/alerts/AlertCard",
  () => ({
    default: ({
      title,
    }: {
      title: string;
    }) => <div>{title}</div>,
  })
);

import { alertsService } from "../../services/alertsService";

describe("AlertsPage", () => {
  it("shows empty state", async () => {
    vi.mocked(
      alertsService.getAlerts
    ).mockResolvedValue([]);

    render(<AlertsPage />);

    await waitFor(() => {
      expect(
        screen.getByText(
          "No alerts available."
        )
      ).toBeTruthy();
    });
  });

  it("renders alerts", async () => {
    vi.mocked(
      alertsService.getAlerts
    ).mockResolvedValue([
      {
        alertId: 1,
        severity: "HIGH",
        title: "Temperature Alert",
        message: "Too hot",
      },
    ] as any);

    render(<AlertsPage />);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Temperature Alert"
        )
      ).toBeTruthy();
    });
  });

  it("shows error state", async () => {
    vi.mocked(
      alertsService.getAlerts
    ).mockRejectedValue(
      new Error("failed")
    );

    render(<AlertsPage />);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Failed to load alerts"
        )
      ).toBeTruthy();
    });
  });
});