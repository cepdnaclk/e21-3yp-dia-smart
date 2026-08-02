import React, { useState } from "react";
import {
  Box,
  Button,
  Card,
  Typography,
  Stepper,
  Step,
  StepLabel,
  Stack,
  Divider,
  useTheme,
  useMediaQuery,
  LinearProgress,
  Alert,
  CircularProgress,
} from "@mui/material";
import {
  DeviceRegistrationStep,
  WifiConfigStep,
  ConnectHotspotStep,
  SendingCredentialsStep,
  ProvisioningProgressStep,
  ReconnectHomeWifiStep,
  CloudVerificationStep,
} from "./WizardSteps";
import { deviceService } from "../../services/deviceService";
import { getPatientId } from "../../utils/patient";
import { deviceConfigurationService } from "../../services/deviceConfigurationService";

const steps = [
  "Device Registration",
  "Wi-Fi Setup",
  "Connect to Hotspot",
  "Sending Credentials",
  "Provisioning Progress",
  "Reconnect Home Wi-Fi",
  "Cloud Verification",
];

interface ProvisioningWizardProps {
  onComplete: () => void;
}

export const ProvisioningWizard: React.FC<ProvisioningWizardProps> = ({
  onComplete,
}) => {
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("sm"));

  const [activeStep, setActiveStep] = useState<number>(0);
  const [showPassword, setShowPassword] = useState<boolean>(false);

  // Form States (held internally)
  const [outerUid, setOuterUid] = useState<string>("");
  const [innerUid, setInnerUid] = useState<string>("");
  const [penUid, setPenUid] = useState<string>("");
  const [glucoseMeterUid, setGlucoseMeterUid] = useState<string>("");

  const [wifiSsid, setWifiSsid] = useState<string>("");
  const [wifiPassword, setWifiPassword] = useState<string>("");

  // Loading & Error States for Step 1 Activation
  const [isActivating, setIsActivating] = useState<boolean>(false);
  const [activationError, setActivationError] = useState<string>("");

  // Loading & Error States for Step 2 Wi-Fi config saving
  const [isSavingConfig, setIsSavingConfig] = useState<boolean>(false);
  const [configError, setConfigError] = useState<string>("");

  // Validation checks for buttons
  const isStep1Valid =
    outerUid.trim() !== "" &&
    innerUid.trim() !== "" &&
    penUid.trim() !== "" &&
    glucoseMeterUid.trim() !== "";

  const isStep2Valid = wifiSsid.trim() !== "" && wifiPassword.length >= 8;

  const handleNext = async () => {
    if (activeStep === 0) {
      // Step 1: Device Registration
      setIsActivating(true);
      setActivationError("");
      try {
        const patientId = getPatientId();
        await deviceService.activateDeviceKit(patientId, {
          outerGatewayId: outerUid.trim(),
          innerUnitId: innerUid.trim(),
          penUnitId: penUid.trim(),
          glucoseMeterId: glucoseMeterUid.trim(),
        });
        setActiveStep(1); // Proceed to Step 2
      } catch (err: any) {
        const errMsg =
          err.response?.data?.message ||
          err.message ||
          "Failed to register devices. Please try again.";
        setActivationError(errMsg);
      } finally {
        setIsActivating(false);
      }
    } else if (activeStep === 1) {
      // Step 2: Wi-Fi Config
      setIsSavingConfig(true);
      setConfigError("");
      try {
        // Fetch patient devices to locate database ID mappings
        const patientDevices = await deviceService.getPatientDevices();
        const outerDevice = patientDevices.find(
          (d) => d.deviceUid === outerUid.trim()
        );
        const innerDevice = patientDevices.find(
          (d) => d.deviceUid === innerUid.trim()
        );
        const penDevice = patientDevices.find(
          (d) => d.deviceUid === penUid.trim()
        );
        const glucometerDevice = patientDevices.find(
          (d) => d.deviceUid === glucoseMeterUid.trim()
        );

        if (!outerDevice) {
          throw new Error(
            "Outer Station base device was not found in your connected list."
          );
        }

        // Save configuration to backend database. Fallback to PUT if already existing.
        try {
          await deviceConfigurationService.createConfiguration({
            outerDeviceId: outerDevice.deviceId,
            wifiSsid: wifiSsid.trim(),
            wifiPassword: wifiPassword,
            innerDeviceId: innerDevice?.deviceId,
            penDeviceId: penDevice?.deviceId,
            glucometerDeviceId: glucometerDevice?.deviceId,
          });
        } catch (err: any) {
          if (
            err.response?.status === 409 ||
            err.response?.data?.errorCode === "CONFIG_ALREADY_EXISTS"
          ) {
            await deviceConfigurationService.updateConfiguration(
              outerDevice.deviceId,
              {
                wifiSsid: wifiSsid.trim(),
                wifiPassword: wifiPassword,
                innerDeviceId: innerDevice?.deviceId,
                penDeviceId: penDevice?.deviceId,
                glucometerDeviceId: glucometerDevice?.deviceId,
              }
            );
          } else {
            throw err;
          }
        }

        setActiveStep(2); // Proceed to Step 3
      } catch (err: any) {
        const errMsg =
          err.response?.data?.message ||
          err.message ||
          "Failed to save Wi-Fi configuration. Please try again.";
        setConfigError(errMsg);
      } finally {
        setIsSavingConfig(false);
      }
    } else if (activeStep === steps.length - 1) {
      onComplete();
    } else {
      setActiveStep((prev) => Math.min(prev + 1, steps.length - 1));
    }
  };

  const handleBack = () => {
    setActiveStep((prev) => Math.max(prev - 1, 0));
  };

  const renderStepContent = () => {
    switch (activeStep) {
      case 0:
        return (
          <Stack spacing={2}>
            {activationError && (
              <Alert severity="error" sx={{ mb: 1 }}>
                {activationError}
              </Alert>
            )}
            <DeviceRegistrationStep
              outerUid={outerUid}
              setOuterUid={setOuterUid}
              innerUid={innerUid}
              setInnerUid={setInnerUid}
              penUid={penUid}
              setPenUid={setPenUid}
              glucoseMeterUid={glucoseMeterUid}
              setGlucoseMeterUid={setGlucoseMeterUid}
              disabled={isActivating}
            />
          </Stack>
        );
      case 1:
        return (
          <Stack spacing={2}>
            {configError && (
              <Alert severity="error" sx={{ mb: 1 }}>
                {configError}
              </Alert>
            )}
            <WifiConfigStep
              wifiSsid={wifiSsid}
              setWifiSsid={setWifiSsid}
              wifiPassword={wifiPassword}
              setWifiPassword={setWifiPassword}
              showPassword={showPassword}
              setShowPassword={setShowPassword}
              disabled={isSavingConfig}
            />
          </Stack>
        );
      case 2:
        return <ConnectHotspotStep outerUid={outerUid} />;
      case 3:
        return <SendingCredentialsStep />;
      case 4:
        return <ProvisioningProgressStep />;
      case 5:
        return <ReconnectHomeWifiStep />;
      case 6:
        return <CloudVerificationStep />;
      default:
        return null;
    }
  };

  const renderStepper = () => {
    if (isMobile) {
      return (
        <Box sx={{ py: 1, px: 2, bgcolor: "action.hover", borderRadius: 2 }}>
          <Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>
            Step {activeStep + 1} of {steps.length}: {steps[activeStep]}
          </Typography>
          <LinearProgress
            variant="determinate"
            value={((activeStep + 1) / steps.length) * 100}
            sx={{ mt: 1, height: 6, borderRadius: 3 }}
          />
        </Box>
      );
    }

    return (
      <Stepper activeStep={activeStep} alternativeLabel>
        {steps.map((label) => (
          <Step key={label}>
            <StepLabel>{label}</StepLabel>
          </Step>
        ))}
      </Stepper>
    );
  };

  const isWorking = isActivating || isSavingConfig;

  return (
    <Card sx={{ width: "100%", borderRadius: 3, boxShadow: 3, overflow: "hidden" }}>
      <Box sx={{ p: { xs: 2, md: 3 }, bgcolor: "primary.main", color: "primary.contrastText" }}>
        <Typography variant="h5" sx={{ fontWeight: 600 }}>
          Dia-Smart Setup Wizard
        </Typography>
        <Typography variant="body2" sx={{ color: "rgba(255,255,255,0.7)", mt: 0.5 }}>
          Follow these steps to connect and configure your IoT device kit.
        </Typography>
      </Box>

      <Box sx={{ p: { xs: 2, md: 3 } }}>
        {renderStepper()}

        <Box sx={{ my: 4, minHeight: "260px" }}>{renderStepContent()}</Box>

        <Divider sx={{ my: 2 }} />

        <Stack direction="row" spacing={2} sx={{ justifyContent: "space-between" }}>
          <Button
            disabled={activeStep === 0 || activeStep === steps.length - 1 || isWorking}
            onClick={handleBack}
            variant="outlined"
            size="large"
            sx={{ minHeight: "48px", minWidth: "100px" }}
          >
            Back
          </Button>

          <Button
            disabled={
              (activeStep === 0 && (!isStep1Valid || isWorking)) ||
              (activeStep === 1 && (!isStep2Valid || isWorking)) ||
              isWorking
            }
            onClick={handleNext}
            variant="contained"
            size="large"
            sx={{ minHeight: "48px", minWidth: "120px" }}
          >
            {isWorking ? (
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <CircularProgress size={20} color="inherit" />
                <span>Saving...</span>
              </Stack>
            ) : activeStep === steps.length - 1 ? (
              "Finish Setup"
            ) : activeStep === 1 ? (
              "Save Config"
            ) : (
              "Next"
            )}
          </Button>
        </Stack>
      </Box>
    </Card>
  );
};
