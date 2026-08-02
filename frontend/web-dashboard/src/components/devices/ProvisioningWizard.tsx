import React, { useState, useEffect } from "react";
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
import type {
  LocalProvisionStatusResponse,
  DeviceConfigurationResponse,
} from "../../services/deviceConfigurationService";

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

  // Store resolved database primary key of Outer gateway
  const [outerDeviceId, setOuterDeviceId] = useState<number | null>(null);

  // Loading & Error States for Step 1 Activation
  const [isActivating, setIsActivating] = useState<boolean>(false);
  const [activationError, setActivationError] = useState<string>("");

  // Loading & Error States for Step 2 Wi-Fi config saving
  const [isSavingConfig, setIsSavingConfig] = useState<boolean>(false);
  const [configError, setConfigError] = useState<string>("");

  // Loading & Error States for Step 4 Local Provisioning
  const [isSendingLocal, setIsSendingLocal] = useState<boolean>(false);
  const [localSendError, setLocalSendError] = useState<string>("");

  // Polling States for Step 5 Local connection progress status
  const [localProvisionStatus, setLocalProvisionStatus] =
    useState<LocalProvisionStatusResponse | null>(null);
  const [localProvisionError, setLocalProvisionError] = useState<string>("");

  // Polling States for Step 7 Cloud Synchronization status checking
  const [cloudConfigStatus, setCloudConfigStatus] =
    useState<DeviceConfigurationResponse | null>(null);
  const [cloudVerifyError, setCloudVerifyError] = useState<string>("");
  const [cloudVerifyFailed, setCloudVerifyFailed] = useState<boolean>(false);

  // Validation checks for buttons
  const isStep1Valid =
    outerUid.trim() !== "" &&
    innerUid.trim() !== "" &&
    penUid.trim() !== "" &&
    glucoseMeterUid.trim() !== "";

  const isStep2Valid = wifiSsid.trim() !== "" && wifiPassword.length >= 8;

  const sendCredentialsLocally = async () => {
    if (!wifiPassword) {
      setLocalSendError(
        "Wi-Fi password is missing in memory. Please go back to Wi-Fi Setup and re-enter credentials."
      );
      return;
    }

    setIsSendingLocal(true);
    setLocalSendError("");

    try {
      await deviceConfigurationService.provisionLocalDevice({
        ssid: wifiSsid.trim(),
        password: wifiPassword,
      });

      setWifiPassword("");
      setActiveStep(4); // Advance to Step 5 (Provisioning Progress)
    } catch (err: any) {
      setWifiPassword("");
      const errMsg =
        err.message ||
        "Failed to deliver configurations. Please verify your connection to the hotspot and try again.";
      setLocalSendError(errMsg);
    } finally {
      setIsSendingLocal(false);
    }
  };

  // Automatically dispatch credentials when entering Step 4 (index 3)
  useEffect(() => {
    if (activeStep === 3) {
      void sendCredentialsLocally();
    }
  }, [activeStep]);

  // Polling Step 5 (index 4) - Local status polling
  useEffect(() => {
    let intervalId: ReturnType<typeof setInterval> | null = null;

    if (activeStep === 4) {
      setLocalProvisionStatus(null);
      setLocalProvisionError("");

      const pollLocalStatus = async () => {
        try {
          const res = await deviceConfigurationService.getLocalProvisionStatus();
          setLocalProvisionStatus(res);

          if (res.status === "success") {
            if (intervalId) clearInterval(intervalId);
            setActiveStep(5); // Auto-advance to Step 6 (Reconnect home wifi)
          } else if (res.status === "error") {
            if (intervalId) clearInterval(intervalId);
            setLocalProvisionError(
              res.message || "Wi-Fi credentials rejected by local gateway base station."
            );
          }
        } catch (err: any) {
          consecutiveLocalFailures++;
          if (consecutiveLocalFailures >= 5) {
            if (intervalId) clearInterval(intervalId);
            setLocalProvisionError(
              "Lost contact with base station hotspot. Please verify connection and try again."
            );
          }
        }
      };

      let consecutiveLocalFailures = 0;
      void pollLocalStatus();
      intervalId = setInterval(pollLocalStatus, 2000);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [activeStep]);

  // Polling Step 7 (index 6) - Cloud validation polling
  useEffect(() => {
    let intervalId: ReturnType<typeof setInterval> | null = null;

    if (activeStep === 6 && outerDeviceId !== null) {
      setCloudConfigStatus(null);
      setCloudVerifyError("");
      setCloudVerifyFailed(false);

      const pollCloud = async () => {
        try {
          const res = await deviceConfigurationService.getConfigurationStatus(
            outerDeviceId
          );
          setCloudConfigStatus(res);

          const isSuccess =
            res.configurationStatus === "PUBLISHED" &&
            res.outerUnitStatus === "PUBLISHED" &&
            res.innerUnitStatus === "CONNECTED";

          const isFailed =
            res.configurationStatus === "FAILED" ||
            res.outerUnitStatus === "FAILED" ||
            res.innerUnitStatus === "FAILED";

          if (isSuccess) {
            if (intervalId) clearInterval(intervalId);
          } else if (isFailed) {
            if (intervalId) clearInterval(intervalId);
            setCloudVerifyFailed(true);
            setCloudVerifyError(
              res.innerUnitMessage || "Verification failed on backend IoT validation."
            );
          }
        } catch (err: any) {
          consecutiveCloudFailures++;
          if (consecutiveCloudFailures >= 5) {
            if (intervalId) clearInterval(intervalId);
            setCloudVerifyFailed(true);
            setCloudVerifyError(
              "AWS IoT backend validation link failed to report heartbeat signals."
            );
          }
        }
      };

      let consecutiveCloudFailures = 0;
      void pollCloud();
      intervalId = setInterval(pollCloud, 2000);
    }

    return () => {
      if (intervalId) clearInterval(intervalId);
    };
  }, [activeStep, outerDeviceId]);

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
        setActiveStep(1);
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
      // Step 2: Wi-Fi Config Saving
      setIsSavingConfig(true);
      setConfigError("");
      try {
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

        setOuterDeviceId(outerDevice.deviceId); // Cache DB identifier

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

        setActiveStep(2);
      } catch (err: any) {
        const errMsg =
          err.response?.data?.message ||
          err.message ||
          "Failed to save Wi-Fi configuration. Please try again.";
        setConfigError(errMsg);
      } finally {
        setIsSavingConfig(false);
      }
    } else if (activeStep === 2) {
      // Step 3: Hotspot confirmation
      setActiveStep(3); // Moves to Step 4 (Automated provisioning call)
    } else if (activeStep === 5) {
      // Step 6: Home connection confirmation
      setActiveStep(6); // Moves to Step 7 (Cloud verification polling)
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
        return (
          <SendingCredentialsStep
            isSending={isSendingLocal}
            error={localSendError}
            onRetry={() => {
              setActiveStep(1);
            }}
          />
        );
      case 4:
        return (
          <ProvisioningProgressStep
            status={localProvisionStatus}
            error={localProvisionError}
          />
        );
      case 5:
        return <ReconnectHomeWifiStep />;
      case 6:
        return (
          <CloudVerificationStep
            status={cloudConfigStatus}
            error={cloudVerifyError}
            isFailed={cloudVerifyFailed}
            onRetry={() => {
              setActiveStep(5); // Go back to step 6 to allow verification restart
            }}
          />
        );
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

  const isWorking = isActivating || isSavingConfig || isSendingLocal;

  const isCloudSuccess =
    cloudConfigStatus?.configurationStatus === "PUBLISHED" &&
    cloudConfigStatus?.outerUnitStatus === "PUBLISHED" &&
    cloudConfigStatus?.innerUnitStatus === "CONNECTED";

  const isNextDisabled =
    (activeStep === 0 && (!isStep1Valid || isWorking)) ||
    (activeStep === 1 && (!isStep2Valid || isWorking)) ||
    (activeStep === 2 && isWorking) ||
    activeStep === 3 || // Automated sending
    activeStep === 4 || // Automated polling
    (activeStep === 5 && isWorking) ||
    (activeStep === 6 && !isCloudSuccess) || // Disabled until fully verified
    isWorking;

  const isBackDisabled =
    activeStep === 0 ||
    activeStep === steps.length - 1 ||
    activeStep === 3 || // Automated step locks navigation
    activeStep === 4 || // Automated step locks navigation
    activeStep === 6 || // Prevent going back while verifying or after sync success
    isWorking;

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
            disabled={isBackDisabled}
            onClick={handleBack}
            variant="outlined"
            size="large"
            sx={{ minHeight: "48px", minWidth: "100px" }}
          >
            Back
          </Button>

          <Button
            disabled={isNextDisabled}
            onClick={handleNext}
            variant="contained"
            size="large"
            sx={{ minHeight: "48px", minWidth: "120px" }}
          >
            {isWorking ? (
              <Stack direction="row" spacing={1} sx={{ alignItems: "center" }}>
                <CircularProgress size={20} color="inherit" />
                <span>Processing...</span>
              </Stack>
            ) : activeStep === steps.length - 1 ? (
              "Finish Setup"
            ) : activeStep === 5 ? (
              "I've Reconnected"
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
