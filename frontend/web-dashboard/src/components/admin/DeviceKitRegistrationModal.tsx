import React, { useState } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Grid,
  Typography,
  Divider,
  Alert,
  CircularProgress
} from "@mui/material";
import api from "../../services/api";

interface DeviceKitRegistrationModalProps {
  open: boolean;
  onClose: () => void;
}

const DeviceKitRegistrationModal: React.FC<DeviceKitRegistrationModalProps> = ({ open, onClose }) => {
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);

  const [formData, setFormData] = useState({
    buyerFullName: "",
    nic: "",
    contactNumber: "",
    address: "",
    purchaseDate: new Date().toISOString().split("T")[0],
    outerGatewayId: "",
    innerUnitId: "",
    penUnitId: "",
    glucoseMeterId: ""
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setFormData({
      ...formData,
      [e.target.name]: e.target.value
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setLoading(true);
    setError(null);
    setSuccess(null);

    // Validate that at least one device ID is provided
    if (!formData.outerGatewayId && !formData.innerUnitId && !formData.penUnitId && !formData.glucoseMeterId) {
      setError("Please provide at least one Device ID to register.");
      setLoading(false);
      return;
    }

    try {
      const response = await api.post("/admin/devices/register-kit", formData);

      if (response.data?.error) {
        throw new Error(response.data.error.message || "Failed to register kit");
      }

      setSuccess("Device kit registered successfully!");
      setTimeout(() => {
        onClose();
        setSuccess(null);
        setFormData({
          buyerFullName: "",
          nic: "",
          contactNumber: "",
          address: "",
          purchaseDate: new Date().toISOString().split("T")[0],
          outerGatewayId: "",
          innerUnitId: "",
          penUnitId: "",
          glucoseMeterId: ""
        });
      }, 2000);
    } catch (err: any) {
      if (err.response && err.response.data) {
        // Handle custom API error format if present
        if (err.response.data.error && err.response.data.error.message) {
          setError(err.response.data.error.message);
        } else if (err.response.data.message) {
          setError(err.response.data.message);
        } else {
          setError(`Request failed: ${err.response.statusText}`);
        }
      } else {
        setError(err.message || "An unexpected error occurred.");
      }
    } finally {
      setLoading(false);
    }
  };

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>Register New Device Kit</DialogTitle>
      <form onSubmit={handleSubmit}>
        <DialogContent dividers>
          {error && <Alert severity="error" sx={{ mb: 2 }}>{error}</Alert>}
          {success && <Alert severity="success" sx={{ mb: 2 }}>{success}</Alert>}

          <Typography variant="h6" gutterBottom>Buyer Information</Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField required fullWidth label="Full Name" name="buyerFullName" value={formData.buyerFullName} onChange={handleChange} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField required fullWidth label="NIC / Passport" name="nic" value={formData.nic} onChange={handleChange} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField required fullWidth label="Contact Number" name="contactNumber" value={formData.contactNumber} onChange={handleChange} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField required fullWidth type="date" label="Purchase Date" InputLabelProps={{ shrink: true }} name="purchaseDate" value={formData.purchaseDate} onChange={handleChange} />
            </Grid>
            <Grid size={{ xs: 12 }}>
              <TextField fullWidth label="Address (Optional)" name="address" value={formData.address} onChange={handleChange} />
            </Grid>
          </Grid>

          <Divider sx={{ my: 3 }} />

          <Typography variant="h6" gutterBottom>Device Information</Typography>
          <Grid container spacing={2}>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField fullWidth label="Outer Gateway Device ID" name="outerGatewayId" value={formData.outerGatewayId} onChange={handleChange} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField fullWidth label="Inner Unit Device ID" name="innerUnitId" value={formData.innerUnitId} onChange={handleChange} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField fullWidth label="Pen Unit Device ID" name="penUnitId" value={formData.penUnitId} onChange={handleChange} />
            </Grid>
            <Grid size={{ xs: 12, sm: 6 }}>
              <TextField fullWidth label="Glucose Meter Device ID" name="glucoseMeterId" value={formData.glucoseMeterId} onChange={handleChange} />
            </Grid>
          </Grid>
        </DialogContent>
        <DialogActions>
          <Button onClick={onClose} disabled={loading}>Cancel</Button>
          <Button type="submit" variant="contained" disabled={loading}>
            {loading ? <CircularProgress size={24} /> : "Register Kit"}
          </Button>
        </DialogActions>
      </form>
    </Dialog>
  );
};

export default DeviceKitRegistrationModal;
