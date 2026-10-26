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
  Chip,
  Box,
  CircularProgress,
  Alert
} from "@mui/material";
import MonitorHeartOutlinedIcon from "@mui/icons-material/MonitorHeartOutlined";
import SensorsOffOutlinedIcon from "@mui/icons-material/SensorsOffOutlined";
import api from "../../services/api";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

interface Device {
  deviceId: number;
  patientId: number | null;
  deviceUid: string;
  deviceType: string;
  deviceName: string;
  status: string;
  online: boolean;
  active: boolean;
  buyer: { fullName: string } | null;
  batteryPercent: number | null;
  lastSeenAt: string | null;
  firmwareVersion: string | null;
}

const diagTileSx = {
  p: 1.75,
  borderRadius: 2,
  border: "1px solid",
  borderColor: "divider",
  bgcolor: "#f8fafc",
} as const;

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

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const DeviceHealthSection = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] = useState<Device | null>(null);
  const [diagnostics, setDiagnostics] = useState<Diagnostics | null>(null);
  const [loading, setLoading] = useState(true);
  const [diagLoading, setDiagLoading] = useState(false);

  const fetchDevices = async (silent = false) => {
    if (!silent) {
      setLoading(true);
    }
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
      if (!silent) {
        setLoading(false);
      }
    }
  };

  useEffect(() => {
    fetchDevices(false);
  }, []);

  useAutoRefresh(() => fetchDevices(true), 5000);

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
    <Card sx={{ height: "100%" }}>
      <CardContent>
        <CardHeading
          title="Device Telemetry & Health"
          subtitle="Monitor live connectivity, battery levels, and firmware status of all active devices."
          icon={<MonitorHeartOutlinedIcon />}
          color="#10b981"
          sx={{ alignItems: "flex-start" }}
        />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", p: 3 }}>
            <CircularProgress size={36} thickness={4} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : (
          <TableContainer component={Paper} variant="outlined" sx={{ overflowX: "auto" }}>
            <Table size="small" sx={{ minWidth: 860 }}>
              <TableHead>
                <TableRow>
                  <TableCell>Device ID</TableCell>
                  <TableCell>Device Type</TableCell>
                  <TableCell>Buyer</TableCell>
                  <TableCell>Online</TableCell>
                  <TableCell>Battery</TableCell>
                  <TableCell>Last Seen</TableCell>
                  <TableCell>Firmware</TableCell>
                  <TableCell>Status</TableCell>
                  <TableCell align="right">Action</TableCell>
                </TableRow>
              </TableHead>
              <TableBody>
                {devices.map((device) => (
                  <TableRow key={device.deviceId} hover>
                    <TableCell sx={{ fontFamily: "monospace", fontSize: "0.8rem", whiteSpace: "nowrap" }}>
                      {device.deviceUid}
                    </TableCell>
                    <TableCell sx={{ whiteSpace: "nowrap" }}>{device.deviceType}</TableCell>
                    <TableCell>{device.buyer?.fullName || "N/A"}</TableCell>
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
                    <TableCell sx={{ whiteSpace: "nowrap", color: "text.secondary" }}>
                      {device.lastSeenAt ? new Date(device.lastSeenAt).toLocaleString() : "Never"}
                    </TableCell>
                    <TableCell>{device.firmwareVersion || "N/A"}</TableCell>
                    <TableCell>
                      <Chip label={device.status} size="small" variant="outlined" />
                    </TableCell>
                    <TableCell align="right">
                      <Button size="small" variant="outlined" onClick={() => handleViewDetails(device)} sx={{ whiteSpace: "nowrap" }}>
                        View Details
                      </Button>
                    </TableCell>
                  </TableRow>
                ))}
                {devices.length === 0 && (
                  <TableRow>
                    <TableCell colSpan={9} sx={{ py: 2 }}>
                      <EmptyState message="No active devices found." icon={<SensorsOffOutlinedIcon />} minHeight={120} />
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
                color: "#10b981",
                bgcolor: "#d1fae5",
              }}
            >
              <MonitorHeartOutlinedIcon fontSize="small" />
            </Box>
            Device Diagnostics
          </DialogTitle>
          <DialogContent dividers>
            {selectedDevice && (
              <Box>
                <Typography variant="subtitle1" component="h3" gutterBottom sx={{ fontWeight: 700, color: "primary.main", overflowWrap: "anywhere" }}>
                  {selectedDevice.deviceName || selectedDevice.deviceUid} ({selectedDevice.deviceType})
                </Typography>

                {diagLoading ? (
                  <Box sx={{ display: "flex", justifyContent: "center", p: 4 }}>
                    <CircularProgress size={32} thickness={4} sx={{ color: "secondary.dark" }} />
                  </Box>
                ) : diagnostics ? (
                  <Grid container spacing={2} sx={{ mt: 1 }}>
                    <Grid size={{ xs: 12, sm: 6 }} sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                      <Box sx={diagTileSx}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>Connectivity</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 600, color: "text.primary", mt: 0.25 }}>
                          {diagnostics.online ? "🟢 Online" : "🔴 Offline"}
                        </Typography>
                      </Box>

                      <Box sx={diagTileSx}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>Last MQTT Message</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 600, color: "text.primary", mt: 0.25 }}>
                          {diagnostics.lastMqttReceivedAt ? new Date(diagnostics.lastMqttReceivedAt).toLocaleString() : "Never"}
                        </Typography>
                      </Box>

                      <Box sx={diagTileSx}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>WiFi RSSI (Signal Strength)</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 600, color: "text.primary", mt: 0.25 }}>
                          {diagnostics.wifiRssiDbm ? `${diagnostics.wifiRssiDbm} dBm` : "N/A"}
                        </Typography>
                      </Box>
                    </Grid>

                    <Grid size={{ xs: 12, sm: 6 }} sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
                      <Box sx={diagTileSx}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>Battery Level</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 600, color: "text.primary", mt: 0.25 }}>
                          {diagnostics.batteryPercent ? `${diagnostics.batteryPercent}%` : "N/A"}
                        </Typography>
                      </Box>

                      <Box sx={diagTileSx}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>Battery Voltage</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 600, color: "text.primary", mt: 0.25 }}>
                          {diagnostics.batteryVoltageV ? `${diagnostics.batteryVoltageV} V` : "N/A"}
                        </Typography>
                      </Box>

                      <Box sx={diagTileSx}>
                        <Typography variant="caption" color="text.secondary" sx={{ fontWeight: 600, textTransform: "uppercase", letterSpacing: "0.04em" }}>Free Heap Memory</Typography>
                        <Typography variant="body1" sx={{ fontWeight: 600, color: "text.primary", mt: 0.25 }}>
                          {diagnostics.freeHeapBytes ? `${diagnostics.freeHeapBytes} bytes` : "N/A"}
                        </Typography>
                      </Box>
                    </Grid>
                  </Grid>
                ) : (
                  <Alert severity="error" sx={{ mt: 2 }}>Failed to load diagnostic data.</Alert>
                )}
              </Box>
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

export default DeviceHealthSection;
