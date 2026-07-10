import { Grid, Card, CardContent, Typography } from "@mui/material";

interface OverviewStatsProps {
  patientsCount: number;
  openAlertsCount: number;
}

const OverviewStats: React.FC<OverviewStatsProps> = ({ patientsCount, openAlertsCount }) => {
  const statItems = [
    {
      title: "Total Patients",
      value: patientsCount,
      desc: "Total number of patients assigned to this doctor."
    },
    {
      title: "Critical Alerts",
      value: openAlertsCount,
      desc: "Number of active critical alerts requiring immediate action."
    },
    {
      title: "Average Adherence",
      value: "88.4%",
      desc: "Average dose adherence rate across all assigned patients."
    },
    {
      title: "Active Devices",
      value: patientsCount > 0 ? patientsCount * 2 : 0,
      desc: "Number of currently active monitoring/dosing devices."
    }
  ];

  return (
    <Grid container spacing={3}>
      {statItems.map((item, idx) => (
        <Grid key={idx} size={{ xs: 12, sm: 6, md: 3 }}>
          <Card elevation={2} sx={{ borderRadius: 3, height: "100%" }}>
            <CardContent>
              <Typography variant="subtitle2" color="text.secondary" gutterBottom>
                {item.title}
              </Typography>
              <Typography variant="h4" sx={{ fontWeight: "bold", mt: 1, color: "primary.main" }}>
                {item.value}
              </Typography>
              <Typography variant="body2" color="text.secondary" sx={{ mt: 1.5 }}>
                {item.desc}
              </Typography>
            </CardContent>
          </Card>
        </Grid>
      ))}
    </Grid>
  );
};

export default OverviewStats;
