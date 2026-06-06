import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";

import DashboardLayout from "../layouts/DashboardLayout/DashboardLayout";

import DashboardPage from "../pages/dashboard/DashboardPage";
import AlertsPage from "../pages/alerts/AlertsPage";
import AnalyticsPage from "../pages/analytics/AnalyticsPage";
import PatientsPage from "../pages/patients/PatientsPage";
import SettingsPage from "../pages/settings/SettingsPage";
import PrescriptionsPage from "../pages/prescriptions/PrescriptionsPage";

const AppRouter = () => {
  return (
    <BrowserRouter>
      <DashboardLayout>
        <Routes>
          <Route path="/" element={<Navigate to="/dashboard" replace />} />

          <Route path="/dashboard" element={<DashboardPage />} />
          <Route path="/alerts" element={<AlertsPage />} />
          <Route path="/analytics" element={<AnalyticsPage />} />
          <Route path="/patients" element={<PatientsPage />} />
          <Route path="/settings" element={<SettingsPage />} />
          <Route path="/prescriptions" element={<PrescriptionsPage />}
/>
        </Routes>
      </DashboardLayout>
    </BrowserRouter>
  );
};

export default AppRouter;