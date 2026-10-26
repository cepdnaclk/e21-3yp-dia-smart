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
import AssignmentIndOutlinedIcon from "@mui/icons-material/AssignmentIndOutlined";
import DevicesOtherOutlinedIcon from "@mui/icons-material/DevicesOtherOutlined";
import api from "../../services/api";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

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
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading
          title="Device Provisioning & Assignments"
          subtitle="Track which devices are assigned to which patients and view original purchase information."
          icon={<AssignmentIndOutlinedIcon />}
          color="#0ea5e9"
          sx={{ alignItems: "flex-start" }}
        />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
            <CircularProgress size={36} thickness={4} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : (
          <TableContainer component={Paper} variant="outlined" sx={{ overflowX: "auto" }}>
            <Table size="small" sx={{ minWidth: 560 }}>
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
                  <TableRow key={device.deviceId} hover>
                    <TableCell sx={{ fontFamily: "monospace", fontSize: "0.8rem", whiteSpace: "nowrap" }}>
                      {device.deviceUid}
                    </TableCell>
                    <TableCell sx={{ whiteSpace: "nowrap" }}>{device.deviceType}</TableCell>
                    <TableCell>{device.buyer?.fullName || "Unknown"}</TableCell>
                    <TableCell>{device.patientDisplayName || "Not Assigned"}</TableCell>
                    <TableCell>
                      <Chip
                        label={device.patientId ? "Assigned" : "Available"}
                        color={device.patientId ? "info" : "success"}
                        size="small"
                        variant="outlined"
                      />
                    </TableCell>
                  </TableRow>
                ))}
                {devices.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={5} sx={{ py: 2 }}>
                      <EmptyState message="No devices found." icon={<DevicesOtherOutlinedIcon />} minHeight={120} />
                    </TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        <Dialog open={!!selectedDevice} onClose={() => setSelectedDevice(null)} maxWidth="md" fullWidth>
          <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1.5 }}>
            <Box
              aria-hidden
              sx={{
                width: 38,
                height: 38,
                borderRadius: 2,
                flexShrink: 0,
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                color: "#0ea5e9",
                bgcolor: "#e0f2fe",
              }}
            >
              <AssignmentIndOutlinedIcon fontSize="small" />
            </Box>
            Device Assignment Details
          </DialogTitle>
          <DialogContent dividers>
            {selectedDevice && (
              <Grid container spacing={{ xs: 3, md: 4 }}>
                <Grid size={{ xs: 12, md: 6 }}>
                  <Typography variant="subtitle1" component="h3" color="primary" gutterBottom sx={{ fontWeight: 700 }}>
                    Buyer Information
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  {selectedDevice.buyer ? (
                    <Box sx={{ display: "flex", flexDirection: "column", gap: 1, "& strong": { color: "text.primary", fontWeight: 600 }, overflowWrap: "anywhere" }}>
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
                  <Typography variant="subtitle1" component="h3" color="primary" gutterBottom sx={{ fontWeight: 700 }}>
                    Patient Information
                  </Typography>
                  <Divider sx={{ mb: 2 }} />
                  <Box sx={{ display: "flex", flexDirection: "column", gap: 1, "& strong": { color: "text.primary", fontWeight: 600 }, overflowWrap: "anywhere" }}>
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
            <Button onClick={() => setSelectedDevice(null)} variant="contained">Close</Button>
          </DialogActions>
        </Dialog>
      </CardContent>
    </Card>
  );
};

export default DeviceAssignmentSection;
