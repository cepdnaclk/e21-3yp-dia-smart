import React from "react";
import type {
  LocalProvisionStatusResponse,
  DeviceConfigurationResponse,
} from "../../services/deviceConfigurationService";
import {
  Box,
  TextField,
  Typography,
  Stack,
  Alert,
  CircularProgress,
  IconButton,
  InputAdornment,
  Grid,
  Card,
  LinearProgress,
  Divider,
  Button,
  Chip,
} from "@mui/material";
import {
  Visibility as VisibilityIcon,
  VisibilityOff as VisibilityOffIcon,
  Smartphone as PhoneIcon,
  CheckCircle as CheckCircleIcon,
  Wifi as WifiIcon,
  Autorenew as AutorenewIcon,
} from "@mui/icons-material";

/** Shared presentational styles for the wizard steps. */
const stepTitleSx = { fontWeight: 700, mb: 1, color: "primary.main" } as const;

const insetCardSx = {
  width: "100%",
  maxWidth: "450px",
  bgcolor: "#f8fafc",
  borderColor: "#e2e8f0",
  borderRadius: 3,
  boxShadow: "none",
} as const;

const heroIconSx = {
  width: 88,
  height: 88,
  borderRadius: "50%",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  bgcolor: "#e0f2fe",
  boxShadow: "0 0 0 8px #f0f9ff",
} as const;

// --- STEP 1: DEVICE REGISTRATION ---
interface DeviceRegistrationStepProps {
  outerUid: string;
  setOuterUid: (val: string) => void;
  innerUid: string;
  setInnerUid: (val: string) => void;
  penUid: string;
  setPenUid: (val: string) => void;
  glucoseMeterUid: string;
  setGlucoseMeterUid: (val: string) => void;
  disabled?: boolean;
}

export const DeviceRegistrationStep: React.FC<DeviceRegistrationStepProps> = ({
  outerUid,
  setOuterUid,
  innerUid,
  setInnerUid,
  penUid,
  setPenUid,
  glucoseMeterUid,
  setGlucoseMeterUid,
  disabled = false,
}) => {
  return (
    <Stack spacing={3}>
      <Box>
        <Typography sx={stepTitleSx} variant="h6">
          Register Your Kit Devices
        </Typography>
        <Typography color="text.secondary" variant="body2">
          Please enter the four printed device codes located on the back or packaging of your Dia-Smart kit.
        </Typography>
      </Box>

      <Grid container spacing={2}>
        <Grid size={{ xs: 12, md: 6 }}>
          <TextField
            fullWidth
            label="Outer Unit UID (Base Station)"
            placeholder="e.g. DiaSmart-0001"
            value={outerUid}
            onChange={(e) => setOuterUid(e.target.value)}
            disabled={disabled}
            slotProps={{
              htmlInput: { style: { minHeight: "48px" } },
            }}
          />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <TextField
            fullWidth
            label="Inner Unit UID (Enclosure)"
            placeholder="e.g. DiaSmart-Inner-0001"
            value={innerUid}
            onChange={(e) => setInnerUid(e.target.value)}
            disabled={disabled}
            slotProps={{
              htmlInput: { style: { minHeight: "48px" } },
            }}
          />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <TextField
            fullWidth
            label="Pen Unit UID (Cap)"
            placeholder="e.g. DiaSmart-Pen-0001"
            value={penUid}
            onChange={(e) => setPenUid(e.target.value)}
            disabled={disabled}
            slotProps={{
              htmlInput: { style: { minHeight: "48px" } },
            }}
          />
        </Grid>
        <Grid size={{ xs: 12, md: 6 }}>
          <TextField
            fullWidth
            label="Glucose Meter UID"
            placeholder="e.g. MegaCheck-0001"
            value={glucoseMeterUid}
            onChange={(e) => setGlucoseMeterUid(e.target.value)}
            disabled={disabled}
            slotProps={{
              htmlInput: { style: { minHeight: "48px" } },
            }}
          />
        </Grid>
      </Grid>

      <Alert severity="info" sx={{ mt: 2 }}>
        Make sure you enter correct codes. All four devices must belong to the same registered package.
      </Alert>
    </Stack>
  );
};

// --- STEP 2: WI-FI CONFIGURATION ---
interface WifiConfigStepProps {
  wifiSsid: string;
  setWifiSsid: (val: string) => void;
  wifiPassword: string;
  setWifiPassword: (val: string) => void;
  showPassword: boolean;
  setShowPassword: (val: boolean) => void;
  disabled?: boolean;
}

export const WifiConfigStep: React.FC<WifiConfigStepProps> = ({
  wifiSsid,
  setWifiSsid,
  wifiPassword,
  setWifiPassword,
  showPassword,
  setShowPassword,
  disabled = false,
}) => {
  return (
    <Stack spacing={3}>
      <Box>
        <Typography sx={stepTitleSx} variant="h6">
          Configure Device Wi-Fi Connection
        </Typography>
        <Typography color="text.secondary" variant="body2">
          Dia-Smart requires your local Wi-Fi connection to sync monitoring data with your care guardians.
        </Typography>
      </Box>

      <Stack spacing={2} sx={{ maxWidth: { xs: "100%", md: "500px" } }}>
        <TextField
          fullWidth
          label="Wi-Fi SSID (Network Name)"
          placeholder="e.g. MyHomeNetwork"
          value={wifiSsid}
          onChange={(e) => setWifiSsid(e.target.value)}
          disabled={disabled}
          slotProps={{
            htmlInput: { style: { minHeight: "48px" } },
          }}
        />
        <TextField
          fullWidth
          label="Wi-Fi Password"
          type={showPassword ? "text" : "password"}
          placeholder="Min 8 characters"
          value={wifiPassword}
          onChange={(e) => setWifiPassword(e.target.value)}
          disabled={disabled}
          slotProps={{
            htmlInput: { style: { minHeight: "48px" } },
            input: {
              endAdornment: (
                <InputAdornment position="end">
                  <IconButton edge="end" onClick={() => setShowPassword(!showPassword)} disabled={disabled}>
                    {showPassword ? <VisibilityOffIcon /> : <VisibilityIcon />}
                  </IconButton>
                </InputAdornment>
              ),
            },
          }}
        />
      </Stack>

      <Alert severity="warning">
        Security Notice: Your Wi-Fi password is encrypted during transit and will not be saved locally or in web cache storage.
      </Alert>
    </Stack>
  );
};

// --- STEP 3: CONNECT TO DIASMART HOTSPOT ---
interface ConnectHotspotStepProps {
  outerUid: string;
}

export const ConnectHotspotStep: React.FC<ConnectHotspotStepProps> = ({ outerUid }) => {
  return (
    <Stack spacing={3} sx={{ alignItems: "center", textAlign: "center", py: 2 }}>
      <Box aria-hidden sx={heroIconSx}>
        <PhoneIcon sx={{ fontSize: 44, color: "#0284c7" }} />
      </Box>
      <Box>
        <Typography sx={stepTitleSx} variant="h6">
          Connect to Device Wi-Fi Hotspot
        </Typography>
        <Typography color="text.secondary" variant="body2" sx={{ maxWidth: "600px" }}>
          To transmit setup configurations directly, you must join the Outer base station's local configuration network.
        </Typography>
      </Box>

      <Card variant="outlined" sx={{ ...insetCardSx, p: 2.5, borderLeft: "4px solid #3ec1fa" }}>
        <Typography variant="subtitle2" sx={{ fontWeight: "bold", mb: 1, textTransform: "uppercase", letterSpacing: 1 }}>
          Instructions
        </Typography>
        <Typography variant="body2" sx={{ textAlign: "left", mb: 1 }}>
          1. Open your smartphone or computer's <b>Wi-Fi Settings</b>.
        </Typography>
        <Typography variant="body2" sx={{ textAlign: "left", mb: 1, wordBreak: "break-all" }}>
          2. Connect to the network named: <b><span style={{ color: "#0284c7" }}>DiaSmart-{outerUid || "XXXX"}</span></b>
        </Typography>
        <Typography variant="body2" sx={{ textAlign: "left", mb: 2 }}>
          3. Once connected, return here and click <b>Next</b>.
        </Typography>
      </Card>

      <Alert severity="info" sx={{ maxWidth: "450px" }}>
        The device setup hotspot does not require a password to connect.
      </Alert>
    </Stack>
  );
};

// --- STEP 4: SENDING CREDENTIALS ---
interface SendingCredentialsStepProps {
  isSending: boolean;
  error: string;
  onRetry: () => void;
}

export const SendingCredentialsStep: React.FC<SendingCredentialsStepProps> = ({
  isSending,
  error,
  onRetry,
}) => {
  return (
    <Stack spacing={3} sx={{ alignItems: "center", textAlign: "center", py: 2 }}>
      {isSending && <CircularProgress size={50} sx={{ mb: 1 }} />}
      
      <Box>
        <Typography sx={stepTitleSx} variant="h6">
          {isSending ? "Sending Wi-Fi Credentials..." : error ? "Transmission Failed" : "Sent"}
        </Typography>
        <Typography color="text.secondary" variant="body2" sx={{ maxWidth: "500px" }}>
          {isSending 
            ? "Uploading the new network credentials directly to the Outer Base Station. This process takes a few seconds."
            : error 
            ? "We were unable to deliver the Wi-Fi configuration details to the local station. Please verify your connection to the hotspot."
            : "Credentials delivered successfully."
          }
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ width: "100%", maxWidth: "450px" }}>
          {error}
        </Alert>
      )}

      <Card variant="outlined" sx={{ ...insetCardSx, p: 3 }}>
        <Typography variant="body2" color="text.primary" sx={{ mb: 1, fontWeight: "bold" }}>
          Transmission Progress
        </Typography>
        <LinearProgress variant={isSending ? "indeterminate" : "determinate"} value={error ? 0 : 100} color={error ? "error" : "primary"} sx={{ height: 10, borderRadius: 5, mb: 1, bgcolor: "#e2e8f0" }} />
        <Typography variant="caption" color="text.secondary">
          {isSending ? "Sending SSID and Password bundle safely over local network..." : error ? "Connection timed out." : "Credentials received by hardware."}
        </Typography>
      </Card>

      {error && (
        <Button variant="contained" size="large" disableElevation onClick={onRetry} sx={{ minHeight: "48px", mt: 1 }}>
          Re-enter Wi-Fi Credentials
        </Button>
      )}
    </Stack>
  );
};

// --- STEP 5: PROVISIONING PROGRESS ---
interface ProvisioningProgressStepProps {
  status: LocalProvisionStatusResponse | null;
  error: string;
}

export const ProvisioningProgressStep: React.FC<ProvisioningProgressStepProps> = ({
  status,
  error,
}) => {
  const outerState = status?.outerStatus || "PENDING";
  const innerState = status?.innerStatus || "PENDING";

  const getProgressValue = (state: string) => {
    switch (state) {
      case "CONNECTED":
        return 100;
      case "CONNECTING":
        return 50;
      case "PENDING":
        return 10;
      default:
        return 0;
    }
  };

  const getChipProps = (state: string) => {
    switch (state) {
      case "CONNECTED":
        return { label: "Connected", color: "success" as const };
      case "CONNECTING":
        return { label: "Connecting", color: "warning" as const, icon: <AutorenewIcon className="rotating" /> };
      case "FAILED":
        return { label: "Failed", color: "error" as const };
      default:
        return { label: "Waiting", color: "default" as const };
    }
  };

  return (
    <Stack spacing={3}>
      <Box>
        <Typography sx={stepTitleSx} variant="h6">
          Provisioning In Progress
        </Typography>
        <Typography color="text.secondary" variant="body2">
          We are waiting for Outer base station to propagate credentials and verify connection status.
        </Typography>
      </Box>

      {error && (
        <Alert severity="error" sx={{ my: 1 }}>
          {error}
        </Alert>
      )}

      <Card variant="outlined" sx={{ p: { xs: 2, sm: 3 }, bgcolor: "#f8fafc", borderColor: "#e2e8f0", borderRadius: 3, boxShadow: "none" }}>
        <Stack spacing={3}>
          <Box>
            <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 1, mb: 1 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>
                Outer Unit Connection Stage
              </Typography>
              <Chip size="small" sx={{ fontWeight: 700 }} {...getChipProps(outerState)} />
            </Stack>
            <LinearProgress variant={outerState === "CONNECTING" ? "indeterminate" : "determinate"} value={getProgressValue(outerState)} sx={{ height: 8, borderRadius: 4, bgcolor: "#e2e8f0" }} />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: "block" }}>
              {status?.message || "Staging Wi-Fi configuration and verifying ESP32 memory..."}
            </Typography>
          </Box>

          <Divider />

          <Box>
            <Stack direction="row" sx={{ justifyContent: "space-between", alignItems: "center", flexWrap: "wrap", gap: 1, mb: 1 }}>
              <Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>
                Inner Unit Connection Stage
              </Typography>
              <Chip size="small" sx={{ fontWeight: 700 }} {...getChipProps(innerState)} />
            </Stack>
            <LinearProgress variant={innerState === "CONNECTING" ? "indeterminate" : "determinate"} value={getProgressValue(innerState)} sx={{ height: 8, borderRadius: 4, bgcolor: "#e2e8f0" }} />
            <Typography variant="caption" color="text.secondary" sx={{ mt: 0.5, display: "block" }}>
              Waiting for Outer station to broadcast ESP-NOW credentials...
            </Typography>
          </Box>
        </Stack>
      </Card>
    </Stack>
  );
};

// --- STEP 6: RECONNECT TO HOME WI-FI ---
export const ReconnectHomeWifiStep: React.FC = () => {
  return (
    <Stack spacing={3} sx={{ alignItems: "center", textAlign: "center", py: 2 }}>
      <Box aria-hidden sx={heroIconSx}>
        <WifiIcon sx={{ fontSize: 44, color: "#0284c7" }} />
      </Box>
      <Box>
        <Typography sx={stepTitleSx} variant="h6">
          Reconnect to Your Home Wi-Fi
        </Typography>
        <Typography color="text.secondary" variant="body2" sx={{ maxWidth: "500px" }}>
          The Outer unit has received credentials and is switching off its setup hotspot. Please reconnect your phone to your home network or mobile data to finish verification.
        </Typography>
      </Box>

      <Card variant="outlined" sx={{ ...insetCardSx, p: 2.5, borderLeft: "4px solid #3ec1fa" }}>
        <Typography variant="subtitle2" sx={{ fontWeight: "bold", mb: 1, textTransform: "uppercase", letterSpacing: 1 }}>
          Instructions
        </Typography>
        <Typography variant="body2" sx={{ textAlign: "left", mb: 1 }}>
          1. Go to your phone's <b>Wi-Fi Settings</b>.
        </Typography>
        <Typography variant="body2" sx={{ textAlign: "left", mb: 1 }}>
          2. Disconnect from `DiaSmart-XXXX` if still connected.
        </Typography>
        <Typography variant="body2" sx={{ textAlign: "left", mb: 2 }}>
          3. Reconnect to your <b>primary internet connection</b>, then return here.
        </Typography>
      </Card>
    </Stack>
  );
};

// --- STEP 7: CLOUD VERIFICATION ---
import { ErrorOutlined as ErrorOutlinedIcon } from "@mui/icons-material";

interface CloudVerificationStepProps {
  status: DeviceConfigurationResponse | null;
  error: string;
  isFailed: boolean;
  onRetry: () => void;
}

export const CloudVerificationStep: React.FC<CloudVerificationStepProps> = ({
  status,
  error,
  isFailed,
  onRetry,
}) => {
  const configStatus = status?.configurationStatus || "PENDING";
  const outerStatus = status?.outerUnitStatus || "PENDING";
  const innerStatus = status?.innerUnitStatus || "PENDING";

  const isSuccess = configStatus === "PUBLISHED" && outerStatus === "PUBLISHED" && innerStatus === "CONNECTED";

  return (
    <Stack spacing={3} sx={{ alignItems: "center", py: 2 }}>
      {isSuccess ? (
        <>
          <Box aria-hidden sx={{ ...heroIconSx, bgcolor: "#d1fae5", boxShadow: "0 0 0 8px #ecfdf5" }}>
            <CheckCircleIcon color="success" sx={{ fontSize: 52 }} />
          </Box>
          <Box sx={{ textAlign: "center" }}>
            <Typography sx={stepTitleSx} variant="h5">
              Kit Setup Successful!
            </Typography>
            <Typography color="text.secondary" variant="body2" sx={{ maxWidth: "500px" }}>
              All devices are successfully registered and connected to the Dia-Smart Cloud. Your guardians can now view your logs in real-time.
            </Typography>
          </Box>

          <Card variant="outlined" sx={{ ...insetCardSx, p: 3, bgcolor: "#ecfdf5", borderColor: "#a7f3d0", color: "#065f46" }}>
            <Typography variant="subtitle2" sx={{ fontWeight: "bold", mb: 1 }}>
              Connection Validated
            </Typography>
            <Typography variant="body2">
              Cloud heartbeat check has passed. Outer base station and Inner storage units are officially connected online to AWS IoT Core.
            </Typography>
          </Card>
        </>
      ) : isFailed ? (
        <>
          <Box aria-hidden sx={{ ...heroIconSx, bgcolor: "#fee2e2", boxShadow: "0 0 0 8px #fef2f2" }}>
            <ErrorOutlinedIcon color="error" sx={{ fontSize: 52 }} />
          </Box>
          <Box sx={{ textAlign: "center" }}>
            <Typography sx={stepTitleSx} variant="h5">
              Cloud Sync Failed
            </Typography>
            <Typography color="text.secondary" variant="body2" sx={{ maxWidth: "500px" }}>
              The cloud configuration validation process has failed. {error || "One or more devices were unable to establish contact with cloud services."}
            </Typography>
          </Box>

          <Alert severity="error" sx={{ width: "100%", maxWidth: "450px" }}>
            {error || "Verification timeout or backend synchronization error occurred."}
          </Alert>

          <Button variant="contained" size="large" disableElevation onClick={onRetry} sx={{ minHeight: "48px", mt: 1 }}>
            Retry Verification
          </Button>
        </>
      ) : (
        <>
          <CircularProgress size={50} sx={{ mb: 1 }} />
          <Box sx={{ textAlign: "center" }}>
            <Typography sx={stepTitleSx} variant="h6">
              Verifying Cloud Synchronization...
            </Typography>
            <Typography color="text.secondary" variant="body2" sx={{ maxWidth: "500px" }}>
              We are waiting for AWS IoT and our cloud services to detect device heartbeats. This may take up to 30 seconds.
            </Typography>
          </Box>

          <Card variant="outlined" sx={{ ...insetCardSx, p: 3 }}>
            <Stack spacing={2}>
              <Box>
                <Stack direction="row" sx={{ justifyContent: "space-between", flexWrap: "wrap", gap: 1, mb: 0.5 }}>
                  <Typography variant="body2" sx={{ fontWeight: "bold" }}>Configuration State</Typography>
                  <Typography variant="body2" color={configStatus === "PUBLISHED" ? "success.main" : "warning.main"}>{configStatus}</Typography>
                </Stack>
                <LinearProgress variant={configStatus === "PENDING" ? "indeterminate" : "determinate"} value={configStatus === "PUBLISHED" ? 100 : 20} sx={{ height: 6, borderRadius: 3, bgcolor: "#e2e8f0" }} />
              </Box>

              <Box>
                <Stack direction="row" sx={{ justifyContent: "space-between", flexWrap: "wrap", gap: 1, mb: 0.5 }}>
                  <Typography variant="body2" sx={{ fontWeight: "bold" }}>Outer Base Station</Typography>
                  <Typography variant="body2" color={outerStatus === "PUBLISHED" ? "success.main" : "warning.main"}>{outerStatus}</Typography>
                </Stack>
                <LinearProgress variant={outerStatus === "PENDING" ? "indeterminate" : "determinate"} value={outerStatus === "PUBLISHED" ? 100 : 20} sx={{ height: 6, borderRadius: 3, bgcolor: "#e2e8f0" }} />
              </Box>

              <Box>
                <Stack direction="row" sx={{ justifyContent: "space-between", flexWrap: "wrap", gap: 1, mb: 0.5 }}>
                  <Typography variant="body2" sx={{ fontWeight: "bold" }}>Inner Enclosure Unit</Typography>
                  <Typography variant="body2" color={innerStatus === "CONNECTED" ? "success.main" : "warning.main"}>{innerStatus}</Typography>
                </Stack>
                <LinearProgress variant={innerStatus === "PENDING" ? "indeterminate" : "determinate"} value={innerStatus === "CONNECTED" ? 100 : 20} sx={{ height: 6, borderRadius: 3, bgcolor: "#e2e8f0" }} />
              </Box>
            </Stack>
          </Card>
        </>
      )}
    </Stack>
  );
};


