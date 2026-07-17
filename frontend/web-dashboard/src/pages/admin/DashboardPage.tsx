import { useState, useEffect } from "react";
import { Grid, Box, CircularProgress, Alert } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";

import TotalUsersCard from "../../components/admin/TotalUsersCard";
import RegisteredDevicesCard from "../../components/admin/RegisteredDevicesCard";
import ActivePatientsCard from "../../components/admin/ActivePatientsCard";
import SystemStatusCard from "../../components/admin/SystemStatusCard";
import RecentActivityCard from "../../components/admin/RecentActivityCard";

import { adminService } from "../../services/adminService";
import type { AuditLogRecord } from "../../types/admin";

const DashboardPage = () => {
  const [totalUsers, setTotalUsers] = useState(0);
  const [activePatients, setActivePatients] = useState(0);
  const [devicesCount, setDevicesCount] = useState(0);
  const [auditLogs, setAuditLogs] = useState<AuditLogRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchDashboardMetrics = async () => {
      setLoading(true);
      setError(null);
      try {
        const [users, logsResponse] = await Promise.all([
          adminService.getAllUsers(),
          adminService.getAuditLogs(0, 5)
        ]);

        setTotalUsers(users.length);
        const patients = users.filter((u) => u.role === "PATIENT");
        setActivePatients(patients.filter((p) => p.active).length);
        
        // Since backend has devices linked to patient users:
        setDevicesCount(patients.length);

        setAuditLogs(logsResponse?.content ?? []);
      } catch (err: any) {
        console.error(err);
        setError("Failed to load administration dashboard statistics.");
      } finally {
        setLoading(false);
      }
    };

    fetchDashboardMetrics();
  }, []);

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "60vh" }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>Admin Dashboard</PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <TotalUsersCard count={totalUsers} />
        </Grid>
        
        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <RegisteredDevicesCard count={devicesCount} />
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <ActivePatientsCard count={activePatients} />
        </Grid>

        <Grid size={{ xs: 12, sm: 6, md: 3 }}>
          <SystemStatusCard status="HEALTHY" />
        </Grid>

        <Grid size={{ xs: 12 }}>
          <RecentActivityCard logs={auditLogs} />
        </Grid>
      </Grid>
    </Box>
  );
};

export default DashboardPage;
