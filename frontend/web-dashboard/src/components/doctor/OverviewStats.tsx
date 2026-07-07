import { Grid, Card, CardContent, Typography } from "@mui/material";

const OverviewStats = () => {
  // TODO: Fetch and display actual overview statistics from API
  const statItems = [
    { title: "Total Patients", desc: "Total number of patients assigned to this doctor." },
    { title: "Critical Alerts", desc: "Number of active critical alerts requiring immediate action." },
    { title: "Average Adherence", desc: "Average dose adherence rate across all assigned patients." },
    { title: "Active Devices", desc: "Number of currently active monitoring/dosing devices." },
  ];

  return (
    <Grid container spacing={3}>
      {statItems.map((item, idx) => (
        <Grid key={idx} size={{ xs: 12, sm: 6, md: 3 }}>
          <Card elevation={2} sx={{ borderRadius: 2 }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                {item.title}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
                {item.desc}
              </Typography>
              <Typography variant="caption" color="text.disabled" sx={{ display: "block", mt: 2 }}>
                [Placeholder Stat Value]
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};

export default OverviewStats;
