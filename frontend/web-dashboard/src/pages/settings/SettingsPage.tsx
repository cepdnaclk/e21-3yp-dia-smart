import {
  Typography,
  Card,
  CardContent,
  FormControlLabel,
  Switch,
  Divider,
  Stack,
} from "@mui/material";

const SettingsPage = () => {
  return (
    <>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Settings
      </Typography>

      <Stack spacing={3}>
        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Notifications
            </Typography>

            <Divider sx={{ mb: 2 }} />

            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Inventory Alerts"
            />

            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Temperature Alerts"
            />

            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Missed Dose Alerts"
            />
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Account Preferences
            </Typography>

            <Divider sx={{ mb: 2 }} />

            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Email Notifications"
            />

            <FormControlLabel
              control={<Switch />}
              label="SMS Notifications"
            />
          </CardContent>
        </Card>

        <Card>
          <CardContent>
            <Typography variant="h6" gutterBottom>
              Security
            </Typography>

            <Divider sx={{ mb: 2 }} />

            <FormControlLabel
              control={<Switch defaultChecked />}
              label="Two-Factor Authentication"
            />
          </CardContent>
        </Card>
      </Stack>
    </>
  );
};

export default SettingsPage;