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
import type { ReactNode } from "react";
import DevicesOtherRoundedIcon from "@mui/icons-material/DevicesOtherRounded";
import SensorsRoundedIcon from "@mui/icons-material/SensorsRounded";
import SensorsOffRoundedIcon from "@mui/icons-material/SensorsOffRounded";
import MemoryRoundedIcon from "@mui/icons-material/MemoryRounded";
import RouterRoundedIcon from "@mui/icons-material/RouterRounded";
import FactCheckRoundedIcon from "@mui/icons-material/FactCheckRounded";
import InfoOutlinedIcon from "@mui/icons-material/InfoOutlined";
import LinkOffRoundedIcon from "@mui/icons-material/LinkOffRounded";
import TouchAppRoundedIcon from "@mui/icons-material/TouchAppRounded";
import HelpOutlineRoundedIcon from "@mui/icons-material/HelpOutlineRounded";

import CardHeading from "../../components/common/CardHeading";
import EmptyState from "../../components/common/EmptyState";
import MetricCard from "../../components/common/MetricCard";
import PageError from "../../components/common/PageError";
import PageLoading from "../../components/common/PageLoading";
import PageTitle from "../../components/common/PageTitle";
import { deviceService } from "../../services/deviceService";
import type {
  Device,
  DeviceDiagnostics,
} from "../../types/device";
import { ProvisioningWizard } from "../../components/devices/ProvisioningWizard";
import { UpdateWifiDialog } from "../../components/devices/UpdateWifiDialog";
import { Wifi as WifiIcon } from "@mui/icons-material";
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

/** Shared tile styling for label/value pairs inside the details grid. */
const detailGridSx = {
  "& > .MuiGrid-root": {
    px: 1.75,
    py: 1.5,
    borderRadius: 2,
    border: "1px solid #e2e8f0",
    backgroundColor: "#f8fafc",
    minWidth: 0,
    overflowWrap: "anywhere",
  },
  "& > .MuiGrid-root > .MuiTypography-body2:first-of-type": {
    fontSize: "0.72rem",
    fontWeight: 700,
    textTransform: "uppercase",
    letterSpacing: "0.05em",
    mb: 0.5,
  },
  "& > .MuiGrid-root > .MuiTypography-body1": {
    fontWeight: 600,
    color: "#1e293b",
  },
} as const;

const DetailSectionTitle = ({ icon, children }: { icon: ReactNode; children: ReactNode }) => (
  <Box sx={{ display: "flex", alignItems: "center", gap: 1, pt: 1 }}>
    <Box
      aria-hidden
      sx={{
        display: "flex",
        color: "#0284c7",
        "& .MuiSvgIcon-root": { fontSize: 20 },
      }}
    >
      {icon}
    </Box>
    <Typography sx={{ fontWeight: 700, color: "primary.main" }}>{children}</Typography>
  </Box>
);

const DeviceStatusChip = ({ online, label }: { online?: boolean; label: string }) => {
  const upper = label.toUpperCase();
  const icon = online || upper === "ONLINE"
    ? <SensorsRoundedIcon />
    : upper === "OFFLINE"
      ? <SensorsOffRoundedIcon />
      : <HelpOutlineRoundedIcon />;
  return (
    <Chip
      color={online ? "success" : "default"}
      variant={online ? "filled" : "outlined"}
      icon={icon}
      label={label}
      size="small"
      sx={{ fontWeight: 700, flexShrink: 0 }}
    />
  );
};

const DeviceDetailsContent = ({
  selectedDevice,
  deviceDiagnostics,
  onUpdateWifi,
}: {
  selectedDevice: Device;
  deviceDiagnostics: DeviceDiagnostics | null;
  onUpdateWifi?: (device: Device) => void;
}) => {
  const isOuterUnit =
    normalizeDeviceType(selectedDevice.deviceType) === "OUTER_UNIT";

  return (
  <Stack spacing={2}>
    <Box
      sx={{
        display: "flex",
        justifyContent: "space-between",
        alignItems: "center",
        flexWrap: "wrap",
        gap: 1.5,
        p: 1.75,
        borderRadius: 2,
        background: "linear-gradient(135deg, #f0f9ff 0%, #f8fafc 100%)",
        border: "1px solid #e0f2fe",
      }}
    >
      <Box sx={{ display: "flex", alignItems: "center", gap: 1.25, minWidth: 0 }}>
        <Box
          aria-hidden
          sx={{
            width: 36,
            height: 36,
            flexShrink: 0,
            borderRadius: 2,
            display: "flex",
            alignItems: "center",
            justifyContent: "center",
            bgcolor: "#ffffff",
            color: "#0284c7",
            boxShadow: "0 1px 3px rgba(15, 23, 42, 0.08)",
          }}
        >
          <MemoryRoundedIcon sx={{ fontSize: 20 }} />
        </Box>
        <Typography sx={{ fontWeight: 700, color: "primary.main", overflowWrap: "anywhere", minWidth: 0 }}>
          {selectedDevice.deviceName ?? selectedDevice.deviceUid}
        </Typography>
      </Box>
      {isOuterUnit && onUpdateWifi && (
        <Button
          variant="outlined"
          size="small"
          startIcon={<WifiIcon />}
          onClick={() => onUpdateWifi(selectedDevice)}
        >
          Update Wi-Fi
        </Button>
      )}
    </Box>

    <Grid container spacing={1.5} sx={detailGridSx}>
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

    <DetailSectionTitle icon={<RouterRoundedIcon />}>Connection Information</DetailSectionTitle>
    <Grid container spacing={1.5} sx={detailGridSx}>
      <Grid size={{ xs: 12, md: 6 }}>
        <Typography color="text.secondary" variant="body2">
          Online / Offline
        </Typography>
        <Box sx={{ pt: 0.25 }}>
          <DeviceStatusChip
            online={
              deviceDiagnostics?.online !== undefined
                ? deviceDiagnostics.online
                : selectedDevice.online
            }
            label={
              deviceDiagnostics?.online !== undefined
                ? deviceDiagnostics.online
                    ? "Online"
                    : "Offline"
                : selectedDevice.online
                  ? "Online"
                  : "Offline"
            }
          />
        </Box>
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

    <DetailSectionTitle icon={<FactCheckRoundedIcon />}>Diagnostics</DetailSectionTitle>
    <Grid container spacing={1.5} sx={detailGridSx}>
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
};

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
  const [wifiUpdateTarget, setWifiUpdateTarget] = useState<Device | null>(null);

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
          <Alert severity="info" icon={<InfoOutlinedIcon />}>
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
          <Grid container spacing={{ xs: 2, md: 3 }}>
            <Grid size={{ xs: 12, sm: 4 }}>
              <MetricCard
                label="Total Registered Devices"
                value={summaryStats.total}
                icon={<DevicesOtherRoundedIcon />}
                tone="info"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <MetricCard
                label="Connected Devices"
                value={summaryStats.connected}
                icon={<SensorsRoundedIcon />}
                tone="success"
              />
            </Grid>
            <Grid size={{ xs: 12, sm: 4 }}>
              <MetricCard
                label="Offline Devices"
                value={summaryStats.offline}
                icon={<SensorsOffRoundedIcon />}
                tone="warning"
              />
            </Grid>
          </Grid>

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 5 }}>
              <Card>
                <CardContent>
                  <CardHeading
                    title="Registered Devices"
                    icon={<DevicesOtherRoundedIcon />}
                  />
                  <Stack spacing={1.5}>
                    {devices.map((device) => (
                      <Card
                        key={device.deviceId}
                        variant="outlined"
                        sx={{
                          cursor: "pointer",
                          boxShadow: "none",
                          borderRadius: 3,
                          borderColor:
                            selectedDevice?.deviceId === device.deviceId
                              ? "#3ec1fa"
                              : "divider",
                          backgroundColor:
                            selectedDevice?.deviceId === device.deviceId
                              ? "#f0f9ff"
                              : "#ffffff",
                          transition: "border-color 0.2s ease, background-color 0.2s ease, box-shadow 0.2s ease",
                          "&:hover": {
                            borderColor: "#3ec1fa",
                            boxShadow: "0 6px 16px rgba(2, 132, 199, 0.08)",
                          },
                        }}
                        onClick={() => {
                          setSelectedDevice(device);
                          if (role === "ADMIN") {
                            void loadDiagnostics(device.deviceId);
                          }
                        }}
                      >
                        <CardContent sx={{ p: 2, "&:last-child": { pb: 2 }, "@media (min-width:600px)": { p: 2, "&:last-child": { pb: 2 } } }}>
                          <Stack spacing={0.75}>
                            <Box sx={{ display: "flex", justifyContent: "space-between", alignItems: "flex-start", gap: 1.5 }}>
                              <Typography sx={{ fontWeight: 700, color: "primary.main", minWidth: 0, overflowWrap: "anywhere" }}>
                                {device.deviceName ?? device.deviceUid}
                              </Typography>
                              <DeviceStatusChip
                                online={device.online}
                                label={device.status ?? "UNKNOWN"}
                              />
                            </Box>
                            <Typography color="text.secondary" variant="body2">
                              Type: {device.deviceType ?? "Unknown"}
                            </Typography>
                            <Typography color="text.secondary" variant="body2" sx={{ overflowWrap: "anywhere" }}>
                              Device ID: {device.deviceUid}
                            </Typography>
                            <Stack direction="row" spacing={1} useFlexGap sx={{ mt: 1, pt: 1.25, flexWrap: "wrap", borderTop: "1px solid #f1f5f9" }}>
                              {normalizeDeviceType(device.deviceType) === "OUTER_UNIT" && (
                                <Button
                                  variant="outlined"
                                  size="small"
                                  startIcon={<WifiIcon />}
                                  onClick={(event) => {
                                    event.stopPropagation();
                                    setWifiUpdateTarget(device);
                                  }}
                                >
                                  Update Wi-Fi
                                </Button>
                              )}
                              <Button
                                color="error"
                                size="small"
                                startIcon={<LinkOffRoundedIcon />}
                                onClick={(event) => {
                                  event.stopPropagation();
                                  setDisconnectTarget(device);
                                }}
                              >
                                Disconnect
                              </Button>
                            </Stack>
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
                  <CardHeading
                    title="Device Details"
                    icon={<MemoryRoundedIcon />}
                  />

                  {selectedDevice ? (
                    <DeviceDetailsContent
                      selectedDevice={selectedDevice}
                      deviceDiagnostics={deviceDiagnostics}
                      onUpdateWifi={(device) => setWifiUpdateTarget(device)}
                    />
                  ) : (
                    <EmptyState
                      message="Select a device to view detailed information."
                      icon={<TouchAppRoundedIcon />}
                    />
                  )}
                </CardContent>
              </Card>
            </Grid>
          </Grid>
        </Stack>
      )}

      <Dialog
        open={Boolean(disconnectTarget)}
        onClose={() => setDisconnectTarget(null)}
        maxWidth="xs"
        fullWidth
      >
        <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1.25 }}>
          <Box
            aria-hidden
            sx={{
              width: 36,
              height: 36,
              flexShrink: 0,
              borderRadius: 2,
              display: "flex",
              alignItems: "center",
              justifyContent: "center",
              bgcolor: "#fee2e2",
              color: "#b91c1c",
            }}
          >
            <LinkOffRoundedIcon sx={{ fontSize: 20 }} />
          </Box>
          Disconnect device
        </DialogTitle>
        <DialogContent>
          <DialogContentText>
            Disconnecting this device will stop healthcare monitoring for this patient.
          </DialogContentText>
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" color="inherit" onClick={() => setDisconnectTarget(null)}>
            Cancel
          </Button>
          <Button color="error" variant="contained" disableElevation onClick={handleDisconnectConfirm} disabled={disconnecting}>
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
              onUpdateWifi={(device) => setWifiUpdateTarget(device)}
            />
          ) : (
            <Typography>Loading details...</Typography>
          )}
        </DialogContent>
        <DialogActions>
          <Button variant="outlined" onClick={() => setDetailsDialogOpen(false)}>Close</Button>
        </DialogActions>
      </Dialog>

      <UpdateWifiDialog
        open={Boolean(wifiUpdateTarget)}
        onClose={() => setWifiUpdateTarget(null)}
        outerDevice={wifiUpdateTarget}
        onSuccess={() => loadDevices(true)}
      />
    </>
  );
};

export default DevicesPage;
