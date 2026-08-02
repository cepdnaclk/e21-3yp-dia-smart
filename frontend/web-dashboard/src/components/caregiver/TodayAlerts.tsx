import { Card, CardContent, Typography, Box, List, ListItem, Chip } from "@mui/material";
import type { Alert } from "../../types/alert";

interface TodayAlertsProps {
  alerts: Alert[];
}

const TodayAlerts: React.FC<TodayAlertsProps> = ({ alerts }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          Today's Alerts
        </Typography>

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.5, p: 0 }}>
          {alerts.map((alert) => (
            <ListItem
              key={alert.alertId}
              sx={{
                p: 1.5,
                bgcolor: "error.light",
                color: "error.dark",
                borderRadius: 2,
                flexDirection: "column",
                alignItems: "flex-start",
                gap: 0.5,
                opacity: 0.95
              }}
            >
              <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%", alignItems: "center" }}>
                <Typography variant="subtitle2" sx={{ fontWeight: "bold" }}>
                  {alert.title}
                </Typography>
                <Chip
                  label={alert.severity}
                  size="small"
                  color="error"
                  sx={{ fontSize: 10, fontWeight: "bold", height: 20 }}
                />
              </Box>
              <Typography variant="caption" sx={{ color: "error.dark" }}>
                {alert.message}
              </Typography>
            </ListItem>
          ))}

          {alerts.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ textAlign: "center", py: 3 }}>
              No critical patient warnings registered today.
            </Typography>
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default TodayAlerts;
