import { useState, useEffect } from "react";
import { Grid, Box, CircularProgress, Alert } from "@mui/material";

import PageTitle from "../../components/common/PageTitle";
import PatientsSection from "../../components/admin/PatientsSection";
import DoctorsSection from "../../components/admin/DoctorsSection";
import CaregiversSection from "../../components/admin/CaregiversSection";
import AdministratorsSection from "../../components/admin/AdministratorsSection";
import AddUserModal from "../../components/admin/AddUserModal";

import { adminService } from "../../services/adminService";
import { UserRole } from "../../types/roles";
import type { AdminUserRecord } from "../../types/admin";

const UsersPage = () => {
  const [users, setUsers] = useState<AdminUserRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [modalOpen, setModalOpen] = useState(false);
  const [modalDefaultRole, setModalDefaultRole] = useState<UserRole>(UserRole.PATIENT);

  const fetchUsers = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await adminService.getAllUsers();
      setUsers(data);
    } catch (err: any) {
      console.error(err);
      setError("Failed to retrieve user accounts from the database.");
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUsers();
  }, []);

  const handleToggleStatus = async (userId: number, currentActive: boolean) => {
    try {
      const updatedUser = await adminService.updateUserStatus(userId, !currentActive);
      setUsers((prev) =>
        prev.map((u) => (u.userId === userId ? updatedUser : u))
      );
    } catch (err: any) {
      console.error(err);
      alert(err.response?.data?.message || err.message || "Failed to update user status.");
    }
  };

  const handleOpenAddModal = (role: UserRole) => {
    setModalDefaultRole(role);
    setModalOpen(true);
  };

  const handleUserCreated = (newUser: AdminUserRecord) => {
    setUsers((prev) => [...prev, newUser]);
  };

  const patients = users.filter((u) => u.role === UserRole.PATIENT);
  const doctors = users.filter((u) => u.role === UserRole.DOCTOR);
  const caregivers = users.filter((u) => u.role === UserRole.CAREGIVER);
  const administrators = users.filter((u) => u.role === UserRole.ADMIN);

  if (loading && users.length === 0) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", minHeight: "60vh" }}>
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <PageTitle>User Management</PageTitle>

      {error && (
        <Alert severity="error" sx={{ mb: 3 }} onClose={() => setError(null)}>
          {error}
        </Alert>
      )}

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6 }}>
          <PatientsSection
            users={patients}
            onToggleStatus={handleToggleStatus}
            onAddClick={() => handleOpenAddModal(UserRole.PATIENT)}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <DoctorsSection
            users={doctors}
            onToggleStatus={handleToggleStatus}
            onAddClick={() => handleOpenAddModal(UserRole.DOCTOR)}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <CaregiversSection
            users={caregivers}
            onToggleStatus={handleToggleStatus}
            onAddClick={() => handleOpenAddModal(UserRole.CAREGIVER)}
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6 }}>
          <AdministratorsSection
            users={administrators}
            onToggleStatus={handleToggleStatus}
            onAddClick={() => handleOpenAddModal(UserRole.ADMIN)}
          />
        </Grid>
      </Grid>

      <AddUserModal
        open={modalOpen}
        onClose={() => setModalOpen(false)}
        defaultRole={modalDefaultRole}
        onUserCreated={handleUserCreated}
      />
    </Box>
  );
};

export default UsersPage;
