import { describe, it, expect, vi } from "vitest";
import {
  render,
  screen,
  waitFor,
} from "@testing-library/react";

import ProfilePage from "./ProfilePage";

vi.mock(
  "../../services/profileService",
  () => ({
    profileService: {
      getProfile: vi.fn(),
    },
  })
);

import { profileService } from "../../services/profileService";

describe("ProfilePage", () => {
  it("renders profile information", async () => {
    vi.mocked(
      profileService.getProfile
    ).mockResolvedValue({
      userId: 1,
      displayName: "John Doe",
      email: "john@test.com",
      role: "PATIENT",
      contactNumber: "123456789",
      active: true,
      lastLoginAt: "Today",
    });

    render(<ProfilePage />);

    await waitFor(() => {
      expect(
        screen.getByText("Profile")
      ).toBeTruthy();
    });

    expect(
      screen.getByText("John Doe")
    ).toBeTruthy();

    expect(
      screen.getByText("john@test.com")
    ).toBeTruthy();
  });

  it("shows error state", async () => {
    vi.mocked(
      profileService.getProfile
    ).mockRejectedValue(
      new Error("failed")
    );

    render(<ProfilePage />);

    await waitFor(() => {
      expect(
        screen.getByText(
          "Failed to load profile"
        )
      ).toBeTruthy();
    });
  });
});