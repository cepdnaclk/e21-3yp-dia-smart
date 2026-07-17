import { useState, useEffect } from "react";
import type { SelectChangeEvent } from "@mui/material";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Grid,
  Alert,
  CircularProgress,
  MenuItem,
  FormControl,
  InputLabel,
  Select
} from "@mui/material";
import { adminService } from "../../services/adminService";
import { UserRole } from "../../types/roles";
import type { AdminUserRecord } from "../../types/admin";

interface AddUserModalProps {
  open: boolean;
  onClose: () => void;
  defaultRole: UserRole;
  onUserCreated: (user: AdminUserRecord) => void;
}

const AddUserModal: React.FC<AddUserModalProps> = ({
  open,
  onClose,
  defaultRole,
  onUserCreated
}) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [formData, setFormData] = useState({
    displayName: "",
    email: "",
    password: "",
    contactNumber: "",
    role: defaultRole as UserRole,
    active: true
  });

  // Keep form data role synced with defaultRole when modal opens
  useEffect(() => {
    if (open) {
      setFormData({
        displayName: "",
        email: "",
        password: "",
        contactNumber: "",
        role: defaultRole,
        active: true
      });
      setError(null);
      setSuccess(null);
    }
  }, [open, defaultRole]);

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData((prev) => ({
      ...prev,
      [e.target.name]: e.target.value
    }));
  };

  const handleRoleChange = (e: SelectChangeEvent<UserRole>) => {
    setFormData((prev) => ({
      ...prev,
      role: e.target.value as UserRole
    }));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    if (formData.password.length < 8) {
      setError("Password must be at least 8 characters long.");
      setLoading(false);
      return;
    }

    try {
      const createdUser = await adminService.createUser({
        displayName: formData.displayName,
        email: formData.email,
        password: formData.password,
        role: formData.role,
        contactNumber: formData.contactNumber || undefined,
        active: formData.active
      });

      setSuccess(`User account created successfully!`);
      
      // Delay closing to show success message briefly
      setTimeout(() => {
        onUserCreated(createdUser);
        onClose();
      }, 1500);
    } catch (err: any) {
      if (err.response?.data?.message) {
        setError(err.response.data.message);
      } else if (err.message) {
        setError(err.message);
      } else {
        setError("Failed to create user account. Please try again.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ fontWeight: "bold" }}>Add New User Account</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent dividers>
          {error && (
            <Alert severity="error" sx={{ mb: 3 }}>
              {error}
            </Alert>
          )}
          {success && (
            <Alert severity="success" sx={{ mb: 3 }}>
              {success}
            </Alert>
          )}

          <Grid container spacing={2}>
            <Grid size={12}>
              <TextField
                required
                fullWidth
                label="Full Name"
                name="displayName"
                value={formData.displayName}
                onChange={handleChange}
              />
            </Grid>
            <Grid size={12}>
              <TextField
                required
                fullWidth
                type="email"
                label="Email Address"
                name="email"
                value={formData.email}
                onChange={handleChange}
              />
            </Grid>
            <Grid size={12}>
              <TextField
                required
                fullWidth
                type="password"
                label="Password (min 8 chars)"
                name="password"
                value={formData.password}
                onChange={handleChange}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField
                fullWidth
                label="Contact Number"
                name="contactNumber"
                value={formData.contactNumber}
                onChange={handleChange}
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <FormControl fullWidth required>
                <InputLabel id="add-user-role-label">System Role</InputLabel>
                <Select
                  labelId="add-user-role-label"
                  label="System Role"
                  name="role"
                  value={formData.role}
                  onChange={handleRoleChange}
                >
                  <MenuItem value={UserRole.PATIENT}>Patient</MenuItem>
                  <MenuItem value={UserRole.DOCTOR}>Doctor</MenuItem>
                  <MenuItem value={UserRole.CAREGIVER}>Caregiver</MenuItem>
                  <MenuItem value={UserRole.ADMIN}>Administrator</MenuItem>
                </Select>
              </FormControl>
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions sx={{ px: 3, py: 2 }}>
          <Button onClick={onClose} disabled={loading} color="inherit">
            Cancel
          </Button>
          <Button type="submit" variant="contained" disabled={loading} color="primary">
            {loading ? <CircularProgress size={24} /> : "Create User"}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default AddUserModal;
