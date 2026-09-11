import "@testing-library/jest-dom/vitest";
import { describe, it, expect, vi, beforeEach } from "vitest";
import { render, screen, waitFor } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import PatientWorkspacePage from "./PatientWorkspacePage";
import { UserRole } from "../../types/roles";

vi.mock("../../context/AuthContext", () => ({
  useAuth: () => ({
    role: UserRole.DOCTOR,
    user: { userId: 1, role: UserRole.DOCTOR, email: "doctor@diasmart.com" },
  }),
}));

vi.mock("../../services/patientsService", () => ({
  patientsService: {
    getPatientProfile: vi.fn().mockResolvedValue({
      patientId: 42,
      fullName: "Nimal Perera",
      dateOfBirth: "1954-03-15",
      gender: "Male",
      diabetesType: "Type 2",
      contactNumber: "+94771234567",
      emergencyContactNumber: "+94779876543",
    }),
  },
}));

vi.mock("../../services/alertsService", () => ({
  alertsService: {
    getAlerts: vi.fn().mockResolvedValue({ content: [] }),
  },
}));

vi.mock("../../services/prescriptionsService", () => ({
  prescriptionsService: {
    getPrescriptions: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock("../../services/doseScheduleService", () => ({
  doseScheduleService: {
    getDoseSchedules: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock("../../services/analyticsService", () => ({
  analyticsService: {
    getAnalytics: vi.fn().mockResolvedValue({
      timeInRange: 75,
      averageGlucose: 120,
      readingsCount: 50,
      dailyStats: [],
    }),
    getGlucoseHistory: vi.fn().mockResolvedValue([]),
    getDoseHistory: vi.fn().mockResolvedValue([]),
    getStorageAlerts: vi.fn().mockResolvedValue([]),
  },
}));

vi.mock("../../services/aiService", () => ({
  aiService: {
    getAiClinicalSummary: vi.fn(),
  },
}));

describe("PatientWorkspacePage - AI Clinical Summary Integration", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("renders the patient workspace with the AI Clinical Summary Card mounted", async () => {
    render(
      <MemoryRouter initialEntries={["/workspace/patients/42"]}>
        <Routes>
          <Route path="/workspace/patients/:patientId" element={<PatientWorkspacePage />} />
        </Routes>
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(screen.getByText("Nimal Perera")).toBeInTheDocument();
    });

    // Verify AI Clinical Summary Card is rendered in the workspace
    expect(screen.getByRole("heading", { name: /AI-Assisted Clinical Insight/i })).toBeInTheDocument();
    expect(screen.getByRole("button", { name: /Generate clinical insight/i })).toBeInTheDocument();

    // Verify other workspace components are also mounted
    expect(screen.getByRole("heading", { name: "Active Alerts" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "Patient Details" })).toBeInTheDocument();
  });
});
