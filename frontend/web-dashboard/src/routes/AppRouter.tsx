import {
  BrowserRouter,
  Routes,
  Route,
} from "react-router-dom";

import DashboardLayout from "../layouts/DashboardLayout/DashboardLayout";

import DashboardPage from "../pages/dashboard/DashboardPage";
import AlertsPage from "../pages/alerts/AlertsPage";
import AnalyticsPage from "../pages/analytics/AnalyticsPage";
import PatientsPage from "../pages/patients/PatientsPage";
import SettingsPage from "../pages/settings/SettingsPage";
import PrescriptionsPage from "../pages/prescriptions/PrescriptionsPage";
import ProfilePage from "../pages/profile/ProfilePage";
import DevicesPage from "../pages/devices/DevicesPage";
import CareTeamPage from "../pages/care-team/CareTeamPage";

import LoginPage from "../pages/auth/LoginPage";
import ForgotPasswordPage from "../pages/auth/ForgotPasswordPage";

import LandingPage from "../pages/landing/LandingPage";

import ProtectedRoute from "./ProtectedRoute";
import RegisterPage from "../pages/auth/RegisterPage";

// Doctor Pages
import DoctorDashboardPage from "../pages/doctor/DashboardPage";
import AssignedPatientsPage from "../pages/doctor/AssignedPatientsPage";
import ReportsPage from "../pages/doctor/ReportsPage";

// Caregiver Pages
import CaregiverDashboardPage from "../pages/caregiver/DashboardPage";
import CaregiverAssignedPatientsPage from "../pages/caregiver/AssignedPatientsPage";

// Admin Pages
import AdminDashboardPage from "../pages/admin/DashboardPage";
import AdminUsersPage from "../pages/admin/UsersPage";
import AdminDevicesPage from "../pages/admin/DevicesPage";
import AdminAssignmentsPage from "../pages/admin/AssignmentsPage";
import AdminSystemPage from "../pages/admin/SystemPage";
import AdminReportsPage from "../pages/admin/ReportsPage";

// Shared Workspace Pages
import PatientWorkspacePage from "../pages/workspace/PatientWorkspacePage";
import { useHardwareBackButton } from "../hooks/useHardwareBackButton";

const BackButtonHandler = () => {
  useHardwareBackButton();
  return null;
};

const AppRouter = () => {
  return (
    <BrowserRouter>
      <BackButtonHandler />
      <Routes>

        {/* Landing Page */}
        <Route
          path="/"
          element={<LandingPage />}
        />

        {/* Public */}
        <Route
          path="/login"
          element={<LoginPage />}
        />

        <Route
          path="/register"
          element={<RegisterPage />}
        />

        <Route
          path="/forgot-password"
          element={
            <ForgotPasswordPage />
          }
        />

        {/* Protected */}
        {/* TODO: Split protected route access by role in Milestone 4 without changing existing patient routes. */}
        <Route
          element={
            <ProtectedRoute>
              <DashboardLayout />
            </ProtectedRoute>
          }
        >
          <Route
            path="/dashboard"
            element={<DashboardPage />}
          />

          {/* Shared Workspace Routes */}
          <Route
            path="/workspace/:patientId"
            element={<PatientWorkspacePage />}
          />

          {/* Doctor Routes */}
          <Route
            path="/doctor/dashboard"
            element={<DoctorDashboardPage />}
          />

          <Route
            path="/doctor/patients"
            element={<AssignedPatientsPage />}
          />

          <Route
            path="/doctor/reports"
            element={<ReportsPage />}
          />

          {/* Caregiver Routes */}
          <Route
            path="/caregiver/dashboard"
            element={<CaregiverDashboardPage />}
          />

          <Route
            path="/caregiver/patients"
            element={<CaregiverAssignedPatientsPage />}
          />

          {/* Admin Routes */}
          <Route
            path="/admin/dashboard"
            element={<AdminDashboardPage />}
          />

          <Route
            path="/admin/users"
            element={<AdminUsersPage />}
          />

          <Route
            path="/admin/devices"
            element={<AdminDevicesPage />}
          />

          <Route
            path="/admin/assignments"
            element={<AdminAssignmentsPage />}
          />

          <Route
            path="/admin/system"
            element={<AdminSystemPage />}
          />

          <Route
            path="/admin/reports"
            element={<AdminReportsPage />}
          />

          <Route
            path="/alerts"
            element={<AlertsPage />}
          />

          <Route
            path="/analytics"
            element={<AnalyticsPage />}
          />

          <Route
            path="/patients"
            element={<PatientsPage />}
          />

          <Route
            path="/prescriptions"
            element={
              <PrescriptionsPage />
            }
          />

          <Route
            path="/devices"
            element={<DevicesPage />}
          />

          <Route
            path="/care-team"
            element={<CareTeamPage />}
          />

          <Route
            path="/profile"
            element={<ProfilePage />}
          />

          <Route
            path="/settings"
            element={<SettingsPage />}
          />
        </Route>

      </Routes>
    </BrowserRouter>
  );
};

export default AppRouter;
