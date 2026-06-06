import { Grid, Typography } from "@mui/material";
import StatCard from "../../components/dashboard/StatCard";

const DashboardPage = () => {
  return (
    <>
      <Typography
        variant="h4"
        sx={{ mb: 3 }}
      >
        Dashboard
      </Typography>

      <Grid container spacing={3}>
        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Glucose"
            value="118 mg/dL"
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Inventory"
            value="41.8 Units"
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Temperature"
            value="5.4 °C"
          />
        </Grid>

        <Grid size={{ xs: 12, md: 6, lg: 3 }}>
          <StatCard
            title="Last Dose"
            value="10 Units"
          />
        </Grid>
      </Grid>
    </>
  );
};

export default DashboardPage;