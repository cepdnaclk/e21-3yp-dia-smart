import React, { useState, useEffect } from "react";
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

interface Device {
  deviceId: number;
  patientId: number | null;
  deviceUid: string;
  deviceType: string;
  deviceName: string;
  status: string;
  online: boolean;
  active: boolean;
  batteryPercent: number | null;
  lastSeenAt: string | null;
  firmwareVersion: string | null;
}

interface Diagnostics {
  deviceId: number;
  online: boolean;
  batteryPercent: number | null;
  batteryVoltageV: number | null;
  wifiRssiDbm: number | null;
  bleRssiDbm: number | null;
  freeHeapBytes: number | null;
  lastMqttReceivedAt: string | null;
}

const DeviceHealthSection = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);
  const [diagnostics, setDiagnostics] = useState<Diagnostics | null>(null);
  const [loading, setLoading] = useState(true);
  const [diagLoading, setDiagLoading] = useState(false);

  useEffect(() => {
    fetchDevices();
  }, []);

  const fetchDevices = async () => {
    setLoading(true);
    try {
      const response = await api.get("/admin/devices");
      if (response.data && response.data.data) {
        // Only show active devices for telemetry
        const activeDevices = response.data.data.filter((d: Device) => d.active);
        setDevices(activeDevices);
      }
    } catch (error) {
      console.error("Failed to fetch devices", error);
    } finally {
      setLoading(false);
    }
  };

  const handleViewDetails = async (device: Device) => {
    setSelectedDevice(device);
    setDiagnostics(null);
    setDiagLoading(true);
    
    try {
      const response = await api.get(`/devices/${device.deviceId}/diagnostics`);
      if (response.data && response.data.data) {
        setDiagnostics(response.data.data);
      }
    } catch (error) {
      console.error("Failed to fetch diagnostics", error);
    } finally {
      setDiagLoading(false);
    }
  };

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Device Telemetry & Health
        </Typography>
        
        <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
          Monitor live connectivity, battery levels, and firmware status of all active devices.
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
                  <TableCell>Status</TableCell>
                  <TableCell>Online</TableCell>
                  <TableCell>Battery</TableCell>
                  <TableCell>Last Seen</TableCell>
                  <TableCell align="right">Action</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {devices.map((device) => (
                  <TableRow key={device.deviceId}>
                    <TableCell>{device.deviceUid}</TableCell>
                    <TableCell>{device.status}</TableCell>
                    <TableCell>
                      <Chip 
                        label={device.online ? "Online" : "Offline"} 
                        color={device.online ? "success" : "error"} 
                        size="small" 
                        variant="outlined"
                      />
                    </TableCell>
                    <TableCell>
                      {device.batteryPercent !== null && device.batteryPercent !== undefined 
                        ? `${device.batteryPercent}%` 
                        : "N/A"}
                    </TableCell>
                    <TableCell>
                      {device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : "Never"}
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" onClick={() => handleViewDetails(device)}>
                        View Details
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
                {devices.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={6} align="center">No active devices found.</TableCell>
                  </TableRow>
                )}
              </TableBody>
            </Table>
          </TableContainer>
        )}

        <Dialog open={!!selectedDevice} onClose={() => setSelectedDevice(null)} maxWidth="md" fullWidth>
          <DialogTitle>Device Diagnostics</DialogTitle>
          <DialogContent dividers>
            {selectedDevice && (
              <Box>
                <Typography variant="h6" gutterBottom>
                  {selectedDevice.deviceName || selectedDevice.deviceUid} ({selectedDevice.deviceType})
                </Typography>
                
                {diagLoading ? (
                  <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
                    <CircularProgress />
                  </Box>
                ) : diagnostics ? (
                  <Grid container spacing={3} sx={{ mt: 1 }}>
                    <Grid item xs={12} sm={6}>
                      <Typography variant="subtitle2" color="text.secondary">Connectivity</Typography>
                      <Typography variant="body1" sx={{ mb: 2 }}>
                        {diagnostics.online ? "🟢 Online" : "🔴 Offline"}
                      </Typography>
                      
                      <Typography variant="subtitle2" color="text.secondary">Last MQTT Message</Typography>
                      <Typography variant="body1" sx={{ mb: 2 }}>
                        {diagnostics.lastMqttReceivedAt ? new Date(diagnostics.lastMqttReceivedAt).toLocaleString() : "Never"}
                      </Typography>
                      
                      <Typography variant="subtitle2" color="text.secondary">WiFi RSSI (Signal Strength)</Typography>
                      <Typography variant="body1" sx={{ mb: 2 }}>
                        {diagnostics.wifiRssiDbm ? `${diagnostics.wifiRssiDbm} dBm` : "N/A"}
                      </Typography>
                    </Grid>
                    
                    <Grid item xs={12} sm={6}>
                      <Typography variant="subtitle2" color="text.secondary">Battery Level</Typography>
                      <Typography variant="body1" sx={{ mb: 2 }}>
                        {diagnostics.batteryPercent ? `${diagnostics.batteryPercent}%` : "N/A"}
                      </Typography>
                      
                      <Typography variant="subtitle2" color="text.secondary">Battery Voltage</Typography>
                      <Typography variant="body1" sx={{ mb: 2 }}>
                        {diagnostics.batteryVoltageV ? `${diagnostics.batteryVoltageV} V` : "N/A"}
                      </Typography>
                      
                      <Typography variant="subtitle2" color="text.secondary">Free Heap Memory</Typography>
                      <Typography variant="body1" sx={{ mb: 2 }}>
                        {diagnostics.freeHeapBytes ? `${diagnostics.freeHeapBytes} bytes` : "N/A"}
                      </Typography>
                    </Grid>
                  </Grid>
                ) : (
                  <Typography color="error">Failed to load diagnostic data.</Typography>
                )}
              </Box>
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

export default DeviceHealthSection;
