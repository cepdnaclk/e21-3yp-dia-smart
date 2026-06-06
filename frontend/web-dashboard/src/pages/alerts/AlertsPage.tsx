import {
  Typography,
  Stack,
} from "@mui/material";

import AlertCard from "../../components/alerts/AlertCard";

const AlertsPage = () => {
  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        Alerts
      </Typography>

      <Stack spacing={2}>
        <AlertCard
          severity="error"
          title="Inventory Low"
          description="Insulin inventory dropped below 20 units."
        />

        <AlertCard
          severity="warning"
          title="Temperature Warning"
          description="Refrigerator temperature exceeded 8°C."
        />

        <AlertCard
          severity="warning"
          title="Missed Dose"
          description="Patient missed scheduled insulin dose."
        />

        <AlertCard
          severity="info"
          title="Glucose Reading Received"
          description="New BLE glucose reading synced successfully."
        />
      </Stack>
    </>
  );
};

export default AlertsPage;