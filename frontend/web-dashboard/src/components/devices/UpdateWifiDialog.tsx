import React, { useState, useEffect } from "react";
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  TextField,
  Stack,
  Typography,
  Alert,
  CircularProgress,
  IconButton,
  InputAdornment,
  Box,
  LinearProgress,
} from "@mui/material";
import {
  Visibility,
  VisibilityOff,
  Wifi as WifiIcon,
  CheckCircle as CheckCircleIcon,
} from "@mui/icons-material";
import { deviceConfigurationService } from "../../services/deviceConfigurationService";
import type { DeviceConfigurationResponse } from "../../services/deviceConfigurationService";
import type { Device } from "../../types/device";

interface UpdateWifiDialogProps {
  open: boolean;
  onClose: () => void;
  outerDevice: Device | null;
  onSuccess?: () => void;
}

const isWifiUpdateSuccessful = (
  status: DeviceConfigurationResponse | null
) =>
  status?.overallStatus === "SUCCEEDED" ||
  status?.configurationStatus === "APPLIED" ||
  (status?.outerUnitStatus === "APPLIED" &&
    status?.innerUnitStatus === "CONNECTED") ||
  (status?.outerUnitStatus === "CONNECTED" &&
    status?.innerUnitStatus === "CONNECTED") ||
  (status?.configurationStatus === "PUBLISHED" &&
    status?.outerUnitStatus === "PUBLISHED" &&
    status?.innerUnitStatus === "CONNECTED");

export const UpdateWifiDialog: React.FC<UpdateWifiDialogProps> = ({
  open,
  onClose,
  outerDevice,
  onSuccess,
}) => {
  const [wifiSsid, setWifiSsid] = useState<string>("");
  const [wifiPassword, setWifiPassword] = useState<string>("");
  const [showPassword, setShowPassword] = useState<boolean>(false);

  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);
  const [submitError, setSubmitError] = useState<string>("");

  // Polling state
  const [isPolling, setIsPolling] = useState<boolean>(false);
  const [configStatus, setConfigStatus] =
    useState<DeviceConfigurationResponse | null>(null);
  const [pollError, setPollError] = useState<string>("");

  // Reset form state when dialog opens
  useEffect(() => {
    if (open) {
      setWifiSsid("");
      setWifiPassword("");
      setShowPassword(false);
      setIsSubmitting(false);
      setSubmitError("");
      setIsPolling(false);
      setConfigStatus(null);
      setPollError("");
    }
  }, [open]);

  // Polling effect for cloud delivery status after submit
  useEffect(() => {
    let intervalId: ReturnType<typeof setInterval> | null = null;

    if (isPolling && outerDevice?.deviceId) {
      let consecutiveFailures = 0;

      const checkStatus = async () => {
        try {
          const res = await deviceConfigurationService.getConfigurationStatus(
            outerDevice.deviceId
          );
          setConfigStatus(res);

          const isSuccess = isWifiUpdateSuccessful(res);

          const failureStatuses = [
            "FAILED",
            "TIMED_OUT",
            "ROLLED_BACK",
            "SUPERSEDED",
            "STALE",
            "REJECTED",
            "EXPIRED",
          ];

          const isTimedOut =
            res.overallStatus === "TIMED_OUT" ||
            res.configurationStatus === "TIMED_OUT";

          const isFailed =
            isTimedOut ||
            failureStatuses.includes(res.overallStatus || "") ||
            failureStatuses.includes(res.configurationStatus || "") ||
            failureStatuses.includes(res.outerUnitStatus || "") ||
            failureStatuses.includes(res.innerUnitStatus || "");

          if (isSuccess) {
            setIsPolling(false);
            if (onSuccess) onSuccess();
          } else if (isFailed) {
            setIsPolling(false);
            if (isTimedOut) {
              setPollError(
                "Device connection timed out. Please ensure both your Outer Unit Base Station and Inner Unit are powered ON and in active range, then try again."
              );
            } else {
              setPollError(
                res.innerUnitMessage ||
                  res.lastErrorMessage ||
                  "Failed to apply new Wi-Fi credentials on device."
              );
            }
          }
        } catch (err: any) {
          consecutiveFailures++;
          if (consecutiveFailures >= 5) {
            setIsPolling(false);
            setPollError(
              "Lost contact with backend status service. Please check device connection later."
            );
          }
        }
      };

      void checkStatus();
      intervalId = setInterval(checkStatus, 2500);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [isPolling, outerDevice, onSuccess]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!outerDevice?.deviceId) {
      setSubmitError("No Outer Unit device selected.");
      return;
    }

    if (!wifiSsid.trim()) {
      setSubmitError("Wi-Fi SSID (Network Name) is required.");
      return;
    }

    if (wifiPassword.length < 8) {
      setSubmitError("Wi-Fi Password must be at least 8 characters.");
      return;
    }

    setIsSubmitting(true);
    setSubmitError("");

    try {
      await deviceConfigurationService.updateConfiguration(
        outerDevice.deviceId,
        {
          wifiSsid: wifiSsid.trim(),
          wifiPassword: wifiPassword,
        }
      );

      setWifiPassword("");
      setIsSubmitting(false);
      setIsPolling(true);
    } catch (err: any) {
      setIsSubmitting(false);
      const errMsg =
        err.response?.data?.message ||
        err.message ||
        "Failed to dispatch Wi-Fi configuration command. Please try again.";
      setSubmitError(errMsg);
    }
  };

  const isSuccess = isWifiUpdateSuccessful(configStatus);

  const isFormValid = wifiSsid.trim() !== "" && wifiPassword.length >= 8;

  return (
    <Dialog open={open} onClose={isPolling || isSubmitting ? undefined : onClose} maxWidth="sm" fullWidth>
      <DialogTitle sx={{ display: "flex", alignItems: "center", gap: 1 }}>
        <WifiIcon color="primary" />
        Update Wi-Fi Credentials
      </DialogTitle>

      <DialogContent dividers>
        <Stack spacing={2.5}>
          {outerDevice && (
            <Typography variant="body2" color="text.secondary">
              Updating Wi-Fi network settings for Outer Unit Base Station:{" "}
              <strong>{outerDevice.deviceName ?? outerDevice.deviceUid}</strong>
            </Typography>
          )}

          {submitError && <Alert severity="error">{submitError}</Alert>}
          {pollError && <Alert severity="error">{pollError}</Alert>}

          {!isPolling && !isSuccess && (
            <Box component="form" onSubmit={handleSubmit} id="update-wifi-form">
              <Stack spacing={2}>
                <TextField
                  label="Wi-Fi SSID (Network Name)"
                  value={wifiSsid}
                  onChange={(e) => setWifiSsid(e.target.value)}
                  fullWidth
                  required
                  disabled={isSubmitting}
                  placeholder="e.g. Home_WiFi_5G"
                />

                <TextField
                  label="Wi-Fi Password"
                  type={showPassword ? "text" : "password"}
                  value={wifiPassword}
                  onChange={(e) => setWifiPassword(e.target.value)}
                  fullWidth
                  required
                  disabled={isSubmitting}
                  helperText="Must be at least 8 characters"
                  slotProps={{
                    input: {
                      endAdornment: (
                        <InputAdornment position="end">
                          <IconButton
                            onClick={() => setShowPassword(!showPassword)}
                            edge="end"
                          >
                            {showPassword ? <VisibilityOff /> : <Visibility />}
                          </IconButton>
                        </InputAdornment>
                      ),
                    },
                  }}
                />
              </Stack>
            </Box>
          )}

          {isPolling && (
            <Stack spacing={2} sx={{ py: 3, alignItems: "center" }}>
              <CircularProgress size={48} />
              <Typography variant="h6">Dispatching & Connecting...</Typography>
              <Typography variant="body2" color="text.secondary" align="center">
                The updated Wi-Fi credentials have been sent over AWS IoT MQTT. The Outer Unit and Inner Unit are now connecting to the new network.
              </Typography>

              <Box sx={{ width: "100%", mt: 1 }}>
                <LinearProgress />
              </Box>

              {configStatus && (
                <Stack spacing={0.5} sx={{ width: "100%", bgcolor: "action.hover", p: 2, borderRadius: 2 }}>
                  <Typography variant="caption" color="text.secondary">
                    Outer Unit Status: <strong>{configStatus.outerUnitStatus}</strong>
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Inner Unit Status: <strong>{configStatus.innerUnitStatus}</strong>
                  </Typography>
                  <Typography variant="caption" color="text.secondary">
                    Overall Status: <strong>{configStatus.overallStatus ?? configStatus.configurationStatus}</strong>
                  </Typography>
                </Stack>
              )}
            </Stack>
          )}

          {isSuccess && (
            <Stack spacing={2} sx={{ py: 3, alignItems: "center" }}>
              <CheckCircleIcon color="success" sx={{ fontSize: 64 }} />
              <Typography variant="h6" color="success.main">
                Wi-Fi Updated Successfully!
              </Typography>
              <Typography variant="body2" color="text.secondary" align="center">
                Both Outer Unit and Inner Unit have connected to the new Wi-Fi network and confirmed active synchronization with the cloud.
              </Typography>
            </Stack>
          )}
        </Stack>
      </DialogContent>

      <DialogActions>
        {isSuccess ? (
          <Button onClick={onClose} variant="contained" color="success">
            Done
          </Button>
        ) : (
          <>
            <Button onClick={onClose} disabled={isSubmitting || isPolling}>
              Cancel
            </Button>
            {!isPolling && (
              <Button
                type="submit"
                form="update-wifi-form"
                variant="contained"
                disabled={!isFormValid || isSubmitting}
                startIcon={isSubmitting ? <CircularProgress size={20} color="inherit" /> : <WifiIcon />}
              >
                {isSubmitting ? "Dispatching..." : "Update Wi-Fi"}
              </Button>
            )}
          </>
        )}
      </DialogActions>
    </Dialog>
  );
};
