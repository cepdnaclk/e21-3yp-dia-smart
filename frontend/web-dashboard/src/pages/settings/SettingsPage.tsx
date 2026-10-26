import { useState, useEffect } from "react";
import {
  Card,
  CardContent,
  FormControlLabel,
  Switch,
  Stack,
  Box,
  CircularProgress,
  Alert,
  Snackbar,
  Grid,
} from "@mui/material";
import NotificationsActiveOutlinedIcon from "@mui/icons-material/NotificationsActiveOutlined";
import ManageAccountsOutlinedIcon from "@mui/icons-material/ManageAccountsOutlined";
import ShieldOutlinedIcon from "@mui/icons-material/ShieldOutlined";

import CardHeading from "../../components/common/CardHeading";
import PageTitle from "../../components/common/PageTitle";

/** Presentational styling for the toggle rows: label left, switch right, separated rows. */
const toggleListSx = {
  "& .MuiFormControlLabel-root": {
    mx: 0,
    px: 1.5,
    py: 0.75,
    width: "100%",
    justifyContent: "space-between",
    gap: 2,
    borderRadius: 2,
    border: "1px solid #f1f5f9",
    transition: "background-color 0.15s ease, border-color 0.15s ease",
    "&:hover": { bgcolor: "#f8fafc", borderColor: "#e2e8f0" },
  },
  "& .MuiFormControlLabel-label": {
    fontWeight: 600,
    color: "#1e293b",
    fontSize: "0.95rem",
  },
} as const;
import { settingsService, type UserSettings } from "../../services/settingsService";

const SettingsPage = () => {
  const [settings, setSettings] = useState<UserSettings | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [snackbarOpen, setSnackbarOpen] = useState(false);

  useEffect(() => {
    const fetchSettings = async () => {
      try {
        const data = await settingsService.getSettings();
        setSettings(data);
      } catch (err) {
        console.error("Failed to load user settings:", err);
        setError("Failed to load user settings.");
      } finally {
        setLoading(false);
      }
    };
    fetchSettings();
  }, []);

  const handleToggle = (key: keyof UserSettings) => async (event: React.ChangeEvent<HTMLInputElement>) => {
    if (!settings) return;
    const updatedVal = event.target.checked;
    
    // Optimistically update the UI state
    const newSettings = { ...settings, [key]: updatedVal };
    setSettings(newSettings);

    try {
      await settingsService.updateSettings(newSettings);
      setSnackbarOpen(true);
    } catch (err) {
      console.error("Failed to update setting:", err);
      // Revert state on failure
      setSettings({ ...settings, [key]: !updatedVal });
      alert("Failed to save settings changes. Please try again.");
    }
  };

  if (loading) {
    return (
      <Box sx={{ display: "flex", justifyContent: "center", py: 8 }}>
        <CircularProgress sx={{ color: "#0284c7" }} />
      </Box>
    );
  }

  if (error) {
    return (
      <Box sx={{ p: 3 }}>
        <Alert severity="error">{error}</Alert>
      </Box>
    );
  }

  if (!settings) return null;

  return (
    <>
      <PageTitle>Settings</PageTitle>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, lg: 6 }}>
        <Card sx={{ height: "100%" }}>
          <CardContent>
            <CardHeading
              title="Notifications"
              icon={<NotificationsActiveOutlinedIcon />}
            />

            <Stack spacing={1} sx={toggleListSx}>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.inventoryAlerts}
                    onChange={handleToggle("inventoryAlerts")}
                    color="primary"
                  />
                }
                label="Inventory Alerts"
                labelPlacement="start"
              />

              <FormControlLabel
                control={
                  <Switch
                    checked={settings.temperatureAlerts}
                    onChange={handleToggle("temperatureAlerts")}
                    color="primary"
                  />
                }
                label="Temperature Alerts"
                labelPlacement="start"
              />

              <FormControlLabel
                control={
                  <Switch
                    checked={settings.missedDoseAlerts}
                    onChange={handleToggle("missedDoseAlerts")}
                    color="primary"
                  />
                }
                label="Missed Dose Alerts"
                labelPlacement="start"
              />
            </Stack>
          </CardContent>
        </Card>
        </Grid>

        <Grid size={{ xs: 12, lg: 6 }}>
        <Stack spacing={3} sx={{ height: "100%" }}>
        <Card>
          <CardContent>
            <CardHeading
              title="Account Preferences"
              icon={<ManageAccountsOutlinedIcon />}
            />

            <Stack spacing={1} sx={toggleListSx}>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.emailNotifications}
                    onChange={handleToggle("emailNotifications")}
                    color="primary"
                  />
                }
                label="Email Notifications"
                labelPlacement="start"
              />

              <FormControlLabel
                control={
                  <Switch
                    checked={settings.smsNotifications}
                    onChange={handleToggle("smsNotifications")}
                    color="primary"
                  />
                }
                label="SMS Notifications"
                labelPlacement="start"
              />
            </Stack>
          </CardContent>
        </Card>

        <Card sx={{ flexGrow: 1 }}>
          <CardContent>
            <CardHeading
              title="Security"
              icon={<ShieldOutlinedIcon />}
              color="#10b981"
            />

            <Box sx={toggleListSx}>
            <FormControlLabel
              control={
                <Switch
                  checked={settings.twoFactorAuth}
                  onChange={handleToggle("twoFactorAuth")}
                  color="primary"
                />
              }
              label="Two-Factor Authentication"
              labelPlacement="start"
            />
            </Box>
          </CardContent>
        </Card>
        </Stack>
        </Grid>
      </Grid>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={3000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert onClose={() => setSnackbarOpen(false)} severity="success" variant="filled" sx={{ width: "100%", fontWeight: 600, boxShadow: "0 12px 32px rgba(15, 23, 42, 0.18)" }}>
          Settings saved successfully!
        </Alert>
      </Snackbar>
    </>
  );
};

export default SettingsPage;
