import { describe, it, expect, vi } from "vitest";
import type { ReactNode } from "react";
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
      acknowledgeAlert: vi.fn(),
      resolveAlert: vi.fn(),
    },
  })
);

vi.mock(
  "../../components/alerts/AlertCard",
  () => ({
    default: ({
      title,
      action,
    }: {
      title: string;
      action?: ReactNode;
    }) => (
      <div>
        {title}
        {action}
      </div>
    ),
  })
);

import { alertsService } from "../../services/alertsService";

describe("AlertsPage", () => {
  it("shows empty state", async () => {
    vi.mocked(
      alertsService.getAlerts
    ).mockResolvedValue({
      content: [],
      number: 0,
      size: 20,
      totalElements: 0,
      totalPages: 0,
    });

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
    ).mockResolvedValue({
      content: [
        {
          alertId: 1,
          severity: "HIGH",
          title: "Temperature Alert",
          message: "Too hot",
          status: "OPEN",
          alertType: "TEMP_HIGH",
          createdAt:
            "2026-06-17T00:00:00Z",
        },
      ],
      number: 0,
      size: 20,
      totalElements: 1,
      totalPages: 1,
    });

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
