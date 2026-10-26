import { Card, CardContent, Typography, Box, List, ListItem, ListItemText } from "@mui/material";
import { alpha } from "@mui/material/styles";
import TimelineRoundedIcon from "@mui/icons-material/TimelineRounded";
import HistoryIcon from "@mui/icons-material/History";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
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
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column" }}>
        <CardHeading title="Recent Activity Timeline" icon={<TimelineRoundedIcon />} color="#0284c7" />

        <List sx={{ display: "flex", flexDirection: "column", p: 0 }}>
          {recentLogs.map((log, idx) => {
            const dotColor = log.status === "RESOLVED" ? "#10b981" : "#ef4444";
            const isLast = idx === recentLogs.length - 1;
            return (
              <ListItem key={log.alertId} sx={{ p: 0, alignItems: "stretch", gap: 1.5 }}>
                {/* Timeline rail */}
                <Box sx={{ display: "flex", flexDirection: "column", alignItems: "center", width: 12, flexShrink: 0 }}>
                  <Box
                    sx={{
                      width: 10,
                      height: 10,
                      borderRadius: "50%",
                      bgcolor: dotColor,
                      boxShadow: `0 0 0 3px ${alpha(dotColor, 0.18)}`,
                      mt: 0.75,
                      flexShrink: 0,
                    }}
                  />
                  {!isLast && <Box sx={{ width: 2, flexGrow: 1, bgcolor: "divider", mt: 0.75 }} />}
                </Box>
                <ListItemText
                  primary={
                    <Typography variant="body2" sx={{ fontWeight: 600, color: "text.primary", overflowWrap: "anywhere" }}>
                      {log.title}
                    </Typography>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 0.25, overflowWrap: "anywhere" }}>
                      {new Date(log.createdAt).toLocaleString()} • Status: {log.status}
                    </Typography>
                  }
                  sx={{ m: 0, pb: isLast ? 0 : 2, minWidth: 0 }}
                />
              </ListItem>
            );
          })}

          {recentLogs.length === 0 && (
            <EmptyState message="No recent notifications logged." icon={<HistoryIcon />} minHeight={140} />
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default RecentActivity;
