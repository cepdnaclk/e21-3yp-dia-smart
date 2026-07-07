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
  TextField,
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

  return normalized;
};

const DevicesPage = () => {
  const [devices, setDevices] = useState<Device[]>([]);
  const [selectedDevice, setSelectedDevice] =
    useState<Device | null>(null);
  const [deviceDiagnostics, setDeviceDiagnostics] =
    useState<DeviceDiagnostics | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [setupValues, setSetupValues] = useState<Record<string, string>>({});
  const [setupStatuses, setSetupStatuses] = useState<Record<string, { status: "idle" | "connecting" | "success" | "error"; message: string }>>({});
  const [disconnectTarget, setDisconnectTarget] =
    useState<Device | null>(null);
  const [disconnecting, setDisconnecting] = useState(false);

  const loadDevices = async () => {
    try {
      setLoading(true);
      setError("");
      const assignedDevices =
        await deviceService.getPatientDevices();
      setDevices(assignedDevices);

      if (assignedDevices.length > 0) {
        const firstDevice = assignedDevices[0];
        setSelectedDevice(firstDevice);
        await loadDiagnostics(firstDevice.deviceId);
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
      setLoading(false);
    }
  };

  const loadDiagnostics = async (deviceId: number) => {
    try {
      const diagnostics =
        await deviceService.getDeviceDiagnostics(deviceId);
      setDeviceDiagnostics(diagnostics);
    } catch (err) {
      setDeviceDiagnostics(null);
      setError(
        err instanceof Error
          ? err.message
          : "Unable to load device diagnostics."
      );
    }
  };

  useEffect(() => {
    void loadDevices();
  }, []);

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

  const handleSetupChange = (deviceKey: string, value: string) => {
    setSetupValues((current) => ({
      ...current,
      [deviceKey]: value,
    }));
  };

  const handleConnectDevice = async (
    deviceKey: string,
    label: string
  ) => {
    const reference = setupValues[deviceKey]?.trim();

    if (!reference) {
      setSetupStatuses((current) => ({
        ...current,
        [deviceKey]: {
          status: "error",
          message: "Please enter a device ID before connecting.",
        },
      }));
      return;
    }

    setSetupStatuses((current) => ({
      ...current,
      [deviceKey]: {
        status: "connecting",
        message: `Connecting ${label}...`,
      },
    }));

    try {
      await deviceService.connectDevice(reference);
      await loadDevices();
      setSetupStatuses((current) => ({
        ...current,
        [deviceKey]: {
          status: "success",
          message: `${label} connected successfully.`,
        },
      }));
    } catch (err) {
      setSetupStatuses((current) => ({
        ...current,
        [deviceKey]: {
          status: "error",
          message:
            err instanceof Error
              ? err.message
              : "Unable to connect the device.",
        },
      }));
    }
  };

  const handleDisconnectConfirm = async () => {
    if (!disconnectTarget) {
      return;
    }

    setDisconnecting(true);

    try {
      await deviceService.disconnectDevice(disconnectTarget.deviceId);
      await loadDevices();
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

          <Grid container spacing={3}>
            {requiredDeviceTypes.map((deviceType) => {
              const setupStatus = setupStatuses[deviceType.key];
              const connectedDevice = devices.find(
                (device) =>
                  normalizeDeviceType(device.deviceType) ===
                  deviceType.key
              );

              return (
                <Grid
                  key={deviceType.key}
                  size={{ xs: 12, md: 6 }}
                >
                  <Card>
                    <CardContent>
                      <Stack spacing={2}>
                        <Box>
                          <Typography variant="h6">
                            {deviceType.label}
                          </Typography>
                          <Typography color="text.secondary" variant="body2">
                            Enter the device ID to connect this component to your account.
                          </Typography>
                        </Box>

                        <TextField
                          label="Device ID"
                          value={setupValues[deviceType.key] ?? ""}
                          onChange={(event) =>
                            handleSetupChange(
                              deviceType.key,
                              event.target.value
                            )
                          }
                          fullWidth
                          size="small"
                        />

                        <Button
                          variant="contained"
                          onClick={() =>
                            void handleConnectDevice(
                              deviceType.key,
                              deviceType.label
                            )
                          }
                        >
                          Connect
                        </Button>

                        <Box>
                          <Typography sx={{ fontWeight: 600 }} variant="body2">
                            Connection status
                          </Typography>
                          {connectedDevice ? (
                            <Chip label="Connected" color="success" size="small" />
                          ) : (
                            <Chip label="Pending" size="small" />
                          )}
                        </Box>

                        {setupStatus && (
                          <Alert severity={setupStatus.status === "success" ? "success" : setupStatus.status === "error" ? "error" : "info"}>
                            {setupStatus.message}
                          </Alert>
                        )}
                      </Stack>
                    </CardContent>
                  </Card>
                </Grid>
              );
            })}
          </Grid>
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
                          void loadDiagnostics(device.deviceId);
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
    </>
  );
};

export default DevicesPage;
