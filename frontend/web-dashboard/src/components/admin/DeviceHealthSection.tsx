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
  Chip
} from "@mui/material";

interface Buyer {
  fullName: string;
  nic: string;
  contactNumber: string;
  address: string;
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
  batteryPercent: number | null;
  lastSeenAt: string | null;
  firmwareVersion: string | null;
  hardwareVersion: string | null;
  mqttClientId: string | null;
  awsThingName: string | null;
  buyer: Buyer | null;
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

  useEffect(() => {
    fetch("/api/v1/devices", {
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success && data.data) {
          // Filter out NEW devices (only activated devices)
          const activatedDevices = data.data.filter((d: Device) => d.status !== 'NEW' && d.active);
          setDevices(activatedDevices);
        }
      });
  }, []);

  const handleViewDetails = (device: Device) => {
    setSelectedDevice(device);
    setDiagnostics(null);
    
    // Fetch live diagnostics for the device
    fetch(`/api/v1/devices/${device.deviceId}/diagnostics`, {
      headers: {
        Authorization: `Bearer ${localStorage.getItem("token")}`,
      },
    })
      .then((res) => res.json())
      .then((data) => {
        if (data.success && data.data) {
          setDiagnostics(data.data);
        }
      });
  };

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Device Telemetry & Health
        </Typography>
        
        <TableContainer component={Paper} variant="outlined">
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>Device ID</TableCell>
                <TableCell>Type</TableCell>
                <TableCell>Buyer</TableCell>
                <TableCell>Patient</TableCell>
                <TableCell>Online</TableCell>
                <TableCell>Battery</TableCell>
                <TableCell>Last Seen</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {devices.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={9} align="center">No activated devices found.</TableCell>
                </TableRow>
              ) : (
                devices.map((device, index) => (
                  <TableRow key={index}>
                    <TableCell>{device.deviceUid}</TableCell>
                    <TableCell>{device.deviceType}</TableCell>
                    <TableCell>{device.buyer?.fullName || "N/A"}</TableCell>
                    <TableCell>{device.patientId || "N/A"}</TableCell>
                    <TableCell>
                      <Chip label={device.online ? "Online" : "Offline"} size="small" color={device.online ? "success" : "error"} />
                    </TableCell>
                    <TableCell>{device.batteryPercent != null ? `${device.batteryPercent}%` : "N/A"}</TableCell>
                    <TableCell>{device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : "Never"}</TableCell>
                    <TableCell>{device.status}</TableCell>
                    <TableCell>
                      <Button size="small" onClick={() => handleViewDetails(device)}>View Details</Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>

        <Dialog open={!!selectedDevice} onClose={() => setSelectedDevice(null)} maxWidth="md" fullWidth>
          <DialogTitle>Device Full Details: {selectedDevice?.deviceUid}</DialogTitle>
          <DialogContent dividers>
            {selectedDevice && (
              <Grid container spacing={3}>
                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle1" fontWeight="bold">Device Information</Typography>
                  <Typography variant="body2" color="textSecondary">ID: {selectedDevice.deviceUid}</Typography>
                  <Typography variant="body2" color="textSecondary">Type: {selectedDevice.deviceType}</Typography>
                  <Typography variant="body2" color="textSecondary">Firmware: {selectedDevice.firmwareVersion || "N/A"}</Typography>
                  <Typography variant="body2" color="textSecondary">Hardware: {selectedDevice.hardwareVersion || "N/A"}</Typography>
                  <Typography variant="body2" color="textSecondary">MQTT Client: {selectedDevice.mqttClientId || "N/A"}</Typography>
                  <Typography variant="body2" color="textSecondary">AWS Thing Name: {selectedDevice.awsThingName || "N/A"}</Typography>
                </Grid>

                <Grid item xs={12} sm={6}>
                  <Typography variant="subtitle1" fontWeight="bold">Buyer Information</Typography>
                  <Typography variant="body2" color="textSecondary">Name: {selectedDevice.buyer?.fullName || "N/A"}</Typography>
                  <Typography variant="body2" color="textSecondary">NIC: {selectedDevice.buyer?.nic || "N/A"}</Typography>
                  <Typography variant="body2" color="textSecondary">Contact: {selectedDevice.buyer?.contactNumber || "N/A"}</Typography>
                  
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="subtitle1" fontWeight="bold">Patient Information</Typography>
                  <Typography variant="body2" color="textSecondary">Assigned Patient ID: {selectedDevice.patientId || "None"}</Typography>
                </Grid>

                <Grid item xs={12}>
                  <Divider sx={{ my: 1 }} />
                  <Typography variant="subtitle1" fontWeight="bold" sx={{ mb: 1 }}>Live Diagnostics</Typography>
                  {diagnostics ? (
                    <Grid container spacing={2}>
                      <Grid item xs={6} sm={3}><Typography variant="body2">Online Status:</Typography> <Chip label={diagnostics.online ? "Online" : "Offline"} size="small" color={diagnostics.online ? "success" : "error"} /></Grid>
                      <Grid item xs={6} sm={3}><Typography variant="body2">Battery %:</Typography> <Typography variant="body2" fontWeight="bold">{diagnostics.batteryPercent != null ? `${diagnostics.batteryPercent}%` : "N/A"}</Typography></Grid>
                      <Grid item xs={6} sm={3}><Typography variant="body2">Battery Voltage:</Typography> <Typography variant="body2" fontWeight="bold">{diagnostics.batteryVoltageV != null ? `${diagnostics.batteryVoltageV}V` : "N/A"}</Typography></Grid>
                      <Grid item xs={6} sm={3}><Typography variant="body2">WiFi RSSI:</Typography> <Typography variant="body2" fontWeight="bold">{diagnostics.wifiRssiDbm != null ? `${diagnostics.wifiRssiDbm} dBm` : "N/A"}</Typography></Grid>
                      <Grid item xs={6} sm={3}><Typography variant="body2">BLE RSSI:</Typography> <Typography variant="body2" fontWeight="bold">{diagnostics.bleRssiDbm != null ? `${diagnostics.bleRssiDbm} dBm` : "N/A"}</Typography></Grid>
                      <Grid item xs={6} sm={3}><Typography variant="body2">Free Heap:</Typography> <Typography variant="body2" fontWeight="bold">{diagnostics.freeHeapBytes != null ? `${diagnostics.freeHeapBytes} bytes` : "N/A"}</Typography></Grid>
                      <Grid item xs={12} sm={6}><Typography variant="body2">Last MQTT Time:</Typography> <Typography variant="body2" fontWeight="bold">{diagnostics.lastMqttReceivedAt ? new Date(diagnostics.lastMqttReceivedAt).toLocaleString() : "N/A"}</Typography></Grid>
                    </Grid>
                  ) : (
                    <Typography variant="body2" color="textSecondary">Loading diagnostics...</Typography>
                  )}
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

export default DeviceHealthSection;
