/* eslint-disable @typescript-eslint/no-unused-vars */
import { useEffect, useMemo, useState } from "react";
import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogContentText,
  DialogTitle,
  Grid,
  Stack,
  Typography,
} from "@mui/material";

import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import { deviceService } from "../../services/deviceService";
import type {
  Device,
  DeviceDiagnostics,
} from "../../types/device";
import { ProvisioningWizard } from "../../components/devices/ProvisioningWizard";
import { useAuth } from "../../context/AuthContext";

const requiredDeviceTypes = [
  {
    key: "OUTER_UNIT",
    label: "Outer Unit",
  },
  {
    key: "INNER_UNIT",
    label: "Inner Unit",
  },
  {
    key: "PEN_UNIT",
    label: "Pen Unit",
  },
  {
    key: "GLUCOMETER",
    label: "Glucose Meter",
  },
] as const;

const normalizeDeviceType = (
  deviceType?: string
) => {
  const normalized = (deviceType ?? "")
    .toUpperCase()
    .replace(/[-\s]/g, "_");

  if (normalized === "OUTER_GATEWAY") {
    return "OUTER_UNIT";
  }

  if (normalized === "DOSE_CAP") {
    return "PEN_UNIT";
  }

  return normalized;
};

const DeviceDetailsContent = ({
  selectedDevice,
  deviceDiagnostics,
}: {
  selectedDevice: Device;
  deviceDiagnostics: DeviceDiagnostics | null;
}) => (
  <Stack spacing={2}>
    <Typography sx={{ fontWeight: 600 }}>
      {selectedDevice.deviceName ?? selectedDevice.deviceUid}
    </Typography>

    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Device Name
        </Typography>
        <Typography>
          {selectedDevice.deviceName ?? "TODO: add display name"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Device Type
        </Typography>
        <Typography>
          {selectedDevice.deviceType ?? "TODO: expose from backend"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Device ID
        </Typography>
        <Typography>{selectedDevice.deviceUid}</Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Registration Date
        </Typography>
        <Typography>
          {selectedDevice.createdAt ?? "TODO: backend field"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Hardware Version
        </Typography>
        <Typography>
          {selectedDevice.hardwareVersion ?? "TODO: backend field"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Firmware Version
        </Typography>
        <Typography>
          {selectedDevice.firmwareVersion ?? "TODO: backend field"}
        </Typography>
      </Grid>
    </Grid>

    <Typography sx={{ fontWeight: 600 }}>Connection Information</Typography>
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Online / Offline
        </Typography>
        <Typography>
          {deviceDiagnostics?.online !== undefined
            ? deviceDiagnostics.online
                ? "Online"
                : "Offline"
            : selectedDevice.online
              ? "Online"
              : "Offline"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Last Synchronization
        </Typography>
        <Typography>
          {deviceDiagnostics?.lastMqttReceivedAt ?? "TODO: backend field"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Last Seen
        </Typography>
        <Typography>
          {selectedDevice.lastSeenAt ?? "TODO: backend field"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Battery Percentage
        </Typography>
        <Typography>
          {deviceDiagnostics?.batteryPercent ?? "TODO: backend field"}
        </Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Communication Status
        </Typography>
        <Typography>
          {selectedDevice.communicationType ?? "TODO: backend field"}
        </Typography>
      </Grid>
    </Grid>

    <Typography sx={{ fontWeight: 600 }}>Diagnostics</Typography>
    <Grid container spacing={2}>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Current Error Messages
        </Typography>
        <Typography>TODO: expose from diagnostics endpoint</Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Warning Messages
        </Typography>
        <Typography>TODO: expose from diagnostics endpoint</Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Sensor Status
        </Typography>
        <Typography>TODO: expose from diagnostics endpoint</Typography>
      </Grid>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Health Status
        </Typography>
        <Typography>TODO: expose from diagnostics endpoint</Typography>
      </Grid>
    </Grid>
  </Stack>
);

import { useAutoRefresh } from "../../hooks/useAutoRefresh";

const DevicesPage = () => {
  const { role } = useAuth();
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] =
    useState<Device | null>(null);
  const [deviceDiagnostics, setDeviceDiagnostics] =
    useState<DeviceDiagnostics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const [disconnectTarget, setDisconnectTarget] =
    useState<Device | null>(null);
  const [disconnecting, setDisconnecting] = useState(false);
  const [detailsDialogOpen, setDetailsDialogOpen] = useState(false);

  const loadDevices = async (silent = false) => {
    try {
      if (!silent) setLoading(true);
      setError("");
      const assignedDevices =
        await deviceService.getPatientDevices();
      setDevices(assignedDevices);

      if (assignedDevices.length > 0) {
        let nextSelected = assignedDevices[0];
        if (selectedDevice) {
          const stillExists = assignedDevices.find(
            (d) => d.deviceId === selectedDevice?.deviceId
          );
          if (stillExists) nextSelected = stillExists;
        }
        setSelectedDevice(nextSelected);
        if (role === "ADMIN") {
          await loadDiagnostics(nextSelected.deviceId);
        }
      } else {
        setSelectedDevice(null);
        setDeviceDiagnostics(null);
      }
    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load device assignments."
      );
    } finally {
      if (!silent) setLoading(false);
    }
  };


  const loadDiagnostics = async (deviceId: number) => {
    try {
      const diagnostics =
        await deviceService.getDeviceDiagnostics(deviceId);
      setDeviceDiagnostics(diagnostics);
    } catch (err) {
      setDeviceDiagnostics(null);
      console.warn("Unable to load device diagnostics:", err);
    }
  };

  useEffect(() => {
    void loadDevices();
  }, []);

  useAutoRefresh(() => loadDevices(true), 5000);

  const connectedTypes = useMemo(() => {
    return new Set(
      devices.map((device) =>
        normalizeDeviceType(device.deviceType)
      )
    );
  }, [devices]);

  const showSetupWizard =
    !loading && connectedTypes.size < requiredDeviceTypes.length;

  const summaryStats = useMemo(() => {
    const connected = devices.filter(
      (device) => device.online || device.status === "ONLINE"
    ).length;
    const offline = devices.filter(
      (device) => device.status === "OFFLINE"
    ).length;

    return {
      total: devices.length,
      connected,
      offline,
    };
  }, [devices]);



  const handleDisconnectConfirm = async () => {
    if (!disconnectTarget) {
      return;
    }

    setDisconnecting(true);

    try {
      await deviceService.disconnectDevice(disconnectTarget.deviceId);
      await loadDevices(true);
      setDisconnectTarget(null);

    } catch (err) {
      setError(
        err instanceof Error
          ? err.message
          : "Unable to disconnect this device."
      );
    } finally {
      setDisconnecting(false);
    }
  };

  if (loading) {
    return <PageLoading />;
  }

  if (error) {
    return <PageError message={error} />;
  }

  return (
    <>
      <PageTitle>Devices</PageTitle>

      {showSetupWizard ? (
        <Stack spacing={3}>
          <Alert severity="info">
            No devices are connected to your account. Please connect your Dia-Smart devices to begin monitoring.
          </Alert>

          <ProvisioningWizard
            onComplete={async () => {
              await loadDevices();
            }}
          />
        </Stack>
      ) : (
        <Stack spacing={3}>
          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 4 }}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" variant="body2">
                    Total Registered Devices
                  </Typography>
                  <Typography variant="h4">
                    {summaryStats.total}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" variant="body2">
                    Connected Devices
                  </Typography>
                  <Typography variant="h4">
                    {summaryStats.connected}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
            <Grid size={{ xs: 12, md: 4 }}>
              <Card>
                <CardContent>
                  <Typography color="text.secondary" variant="body2">
                    Offline Devices
                  </Typography>
                  <Typography variant="h4">
                    {summaryStats.offline}
                  </Typography>
                </CardContent>
              </Card>
            </Grid>
          </Grid>

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 5 }}>
              <Card>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 2 }}>
                    Registered Devices
                  </Typography>
                  <Stack spacing={2}>
                    {devices.map((device) => (
                      <Card
                        key={device.deviceId}
                        variant="outlined"
                        sx={{ cursor: "pointer" }}
                        onClick={() => {
                          setSelectedDevice(device);
                          if (role === "ADMIN") {
                            void loadDiagnostics(device.deviceId);
                          }
                        }}
                      >
                        <CardContent>
                          <Stack spacing={1}>
                            <Box sx={{ display: "flex", justifyContent: "space-between", gap: 2 }}>
                              <Typography sx={{ fontWeight: 600 }}>
                                {device.deviceName ?? device.deviceUid}
                              </Typography>
                              <Chip
                                color={device.online ? "success" : "default"}
                                label={device.status ?? "UNKNOWN"}
                                size="small"
                              />
                            </Box>
                            <Typography color="text.secondary" variant="body2">
                              Type: {device.deviceType ?? "Unknown"}
                            </Typography>
                            <Typography color="text.secondary" variant="body2">
                              Device ID: {device.deviceUid}
                            </Typography>
                            <Button
                              color="error"
                              size="small"
                              sx={{ alignSelf: "flex-start", mt: 1 }}
                              onClick={(event) => {
                                event.stopPropagation();
                                setDisconnectTarget(device);
                              }}
                            >
                              Disconnect
                            </Button>
                          </Stack>
                        </CardContent>
                      </Card>
                    ))}
                  </Stack>
                </CardContent>
              </Card>
            </Grid>

            <Grid size={{ xs: 12, md: 7 }}>
              <Card>
                <CardContent>
                  <Typography variant="h6" sx={{ mb: 2 }}>
                    Device Details
                  </Typography>

                  {selectedDevice ? (
                    <DeviceDetailsContent
                      selectedDevice={selectedDevice}
                      deviceDiagnostics={deviceDiagnostics}
                    />
                  ) : (
                    <Typography color="text.secondary">
                      Select a device to view detailed information.
                    </Typography>
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Stack>
      )}

      <Dialog open={Boolean(disconnectTarget)} onClose={() => setDisconnectTarget(null)}>
        <DialogTitle>Disconnect device</DialogTitle>
        <DialogContent>
          <DialogContentText>
            Disconnecting this device will stop healthcare monitoring for this patient.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDisconnectTarget(null)}>
            Cancel
          </Button>
          <Button color="error" onClick={handleDisconnectConfirm} disabled={disconnecting}>
            {disconnecting ? "Disconnecting..." : "Disconnect"}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog 
        open={detailsDialogOpen} 
        onClose={() => setDetailsDialogOpen(false)}
        maxWidth="md"
        fullWidth
      >
        <DialogTitle>
          Device Details
        </DialogTitle>
        <DialogContent dividers>
          {selectedDevice ? (
            <DeviceDetailsContent
              selectedDevice={selectedDevice}
              deviceDiagnostics={deviceDiagnostics}
            />
          ) : (
            <Typography>Loading details...</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setDetailsDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default DevicesPage;
