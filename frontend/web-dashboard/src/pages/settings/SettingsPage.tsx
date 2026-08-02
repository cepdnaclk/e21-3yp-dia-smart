import { useState, useEffect } from "react";
import {
  Typography,
  Card,
  CardContent,
  FormControlLabel,
  Switch,
  Divider,
  Stack,
  Box,
  CircularProgress,
  Alert,
  Snackbar,
} from "@mui/material";

import PageTitle from "../../components/common/PageTitle";
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
        <CircularProgress />
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

      <Stack spacing={3}>
        <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: "bold", mb: 1, color: "#12233b" }}>
              Notifications
            </Typography>

            <Divider sx={{ mb: 2 }} />

            <Stack spacing={1}>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.inventoryAlerts}
                    onChange={handleToggle("inventoryAlerts")}
                    color="primary"
                  />
                }
                label="Inventory Alerts"
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
              />
            </Stack>
          </CardContent>
        </Card>

        <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: "bold", mb: 1, color: "#12233b" }}>
              Account Preferences
            </Typography>

            <Divider sx={{ mb: 2 }} />

            <Stack spacing={1}>
              <FormControlLabel
                control={
                  <Switch
                    checked={settings.emailNotifications}
                    onChange={handleToggle("emailNotifications")}
                    color="primary"
                  />
                }
                label="Email Notifications"
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
              />
            </Stack>
          </CardContent>
        </Card>

        <Card sx={{ borderRadius: 3, border: "1px solid #e2e8f0" }}>
          <CardContent>
            <Typography variant="h6" sx={{ fontWeight: "bold", mb: 1, color: "#12233b" }}>
              Security
            </Typography>

            <Divider sx={{ mb: 2 }} />

            <FormControlLabel
              control={
                <Switch
                  checked={settings.twoFactorAuth}
                  onChange={handleToggle("twoFactorAuth")}
                  color="primary"
                />
              }
              label="Two-Factor Authentication"
            />
          </CardContent>
        </Card>
      </Stack>

      <Snackbar
        open={snackbarOpen}
        autoHideDuration={3000}
        onClose={() => setSnackbarOpen(false)}
        anchorOrigin={{ vertical: "bottom", horizontal: "right" }}
      >
        <Alert onClose={() => setSnackbarOpen(false)} severity="success" sx={{ width: "100%", borderRadius: 2 }}>
          Settings saved successfully!
        </Alert>
      </Snackbar>
    </>
  );
};

export default SettingsPage;
