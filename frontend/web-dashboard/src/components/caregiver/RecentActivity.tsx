import { Card, CardContent, Typography, Box, List, ListItem, ListItemText } from "@mui/material";
import type { Alert } from "../../types/alert";

interface RecentActivityProps {
  alerts: Alert[];
}

const RecentActivity: React.FC<RecentActivityProps> = ({ alerts }) => {
  // Sort alerts chronologically (latest first) and limit to top 5
  const recentLogs = [...alerts]
    .sort((a, b) => new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime())
    .slice(0, 5);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          Recent Activity Timeline
        </Typography>

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.5, p: 0 }}>
          {recentLogs.map((log) => (
            <ListItem key={log.alertId} sx={{ p: 0, alignItems: "flex-start", gap: 1.5 }}>
              <Box
                sx={{
                  width: 8,
                  height: 8,
                  borderRadius: "50%",
                  bgcolor: log.status === "RESOLVED" ? "success.main" : "error.main",
                  mt: 0.8
                }}
              />
              <ListItemText
                primary={
                  <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                    {log.title}
                  </Typography>
                }
                secondary={
                  <Typography variant="caption" color="text.secondary">
                    {new Date(log.createdAt).toLocaleString()} • Status: {log.status}
                  </Typography>
                }
                sx={{ m: 0 }}
              />
            </ListItem>
          ))}

          {recentLogs.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ py: 2, textAlign: "center" }}>
              No recent notifications logged.
            </Typography>
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default RecentActivity;
