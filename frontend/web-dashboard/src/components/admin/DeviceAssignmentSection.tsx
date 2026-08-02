import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  Typography,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableRow,
  Paper,
  Button,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Grid,
  Divider,
  Chip,
  Box,
  CircularProgress
} from "@mui/material";
import api from "../../services/api";

interface Buyer {
  fullName: string;
  nic: string;
  contactNumber: string;
  address: string;
  purchaseDate: string;
}

interface Device {
  deviceId: number;
  patientId: number | null;
  deviceUid: string;
  deviceType: string;
  deviceName: string;
  status: string;
  online: boolean;
  active: boolean;
  buyer: Buyer | null;
  patientDisplayName: string | null;
}

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const DeviceAssignmentSection = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);
  const [loading, setLoading] = useState(true);

  const fetchDevices = async (silent = false) => {
    if (!silent) {
      setLoading(true);
    }
    try {
      const response = await api.get("/admin/devices");
      setDevices(response.data.data || []);
    } catch (error) {
      console.error("Failed to fetch devices", error);
    } finally {
      if (!silent) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    fetchDevices(false);
  }, []);

  useAutoRefresh(() => fetchDevices(true), 5000);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Device Provisioning & Assignments
        </Typography>
        
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Track which devices are assigned to which patients and view original purchase information.
        </Typography>
        
        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
            <CircularProgress />
          </Box>
        ) : (
          <TableContainer component={Paper} variant="outlined">
            <Table size="small">
              <TableHead>
                <TableRow>
                  <TableCell>Device ID</TableCell>
                  <TableCell>Type</TableCell>
                  <TableCell>Buyer</TableCell>
                  <TableCell>Assigned Patient</TableCell>
                  <TableCell>Status</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {devices.map((device) => (
                  <TableRow key={device.deviceId}>
                    <TableCell>{device.deviceUid}</TableCell>
                    <TableCell>{device.deviceType}</TableCell>
                    <TableCell>{device.buyer?.fullName || "Unknown"}</TableCell>
                    <TableCell>{device.patientDisplayName || "Not Assigned"}</TableCell>
                    <TableCell>
                      <Chip 
                        label={device.patientId ? "Assigned" : "Available"} 
                        color={device.patientId ? "primary" : "default"} 
                        size="small" 
                        variant="outlined"
                      />
                    </TableCell>
                  </TableRow>
                ))}
                {devices.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} align="center">No devices found.</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        <Dialog open={!!selectedDevice} onClose={() => setSelectedDevice(null)} maxWidth="md" fullWidth>
          <DialogTitle>Device Assignment Details</DialogTitle>
          <DialogContent dividers>
            {selectedDevice && (
              <Grid container spacing={4}>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography variant="h6" color="primary" gutterBottom>
                    Buyer Information
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  {selectedDevice.buyer ? (
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
                      <Typography><strong>Name:</strong> {selectedDevice.buyer.fullName}</Typography>
                      <Typography><strong>NIC / Passport:</strong> {selectedDevice.buyer.nic}</Typography>
                      <Typography><strong>Contact:</strong> {selectedDevice.buyer.contactNumber}</Typography>
                      <Typography><strong>Address:</strong> {selectedDevice.buyer.address}</Typography>
                      <Typography><strong>Purchase Date:</strong> {selectedDevice.buyer.purchaseDate}</Typography>
                    </Box>
                  ) : (
                    <Typography color="text.secondary">No buyer information available.</Typography>
                  )}
                </Grid>
                
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography variant="h6" color="primary" gutterBottom>
                    Patient Information
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Box sx={{ display: "flex", flexDirection: "column", gap: 1 }}>
                    <Typography>
                      <strong>Assigned Patient:</strong> {selectedDevice.patientDisplayName ? selectedDevice.patientDisplayName : "Not Assigned"}
                    </Typography>
                    <Typography>
                      <strong>Device Status:</strong> {selectedDevice.status}
                    </Typography>
                  </Box>
                </Grid>
              </Grid>
            )}
          </DialogContent>
          <DialogActions>
            <Button onClick={() => setSelectedDevice(null)}>Close</Button>
          </DialogActions>
        </Dialog>
      </CardContent>
    </Card>
  );
};

export default DeviceAssignmentSection;
