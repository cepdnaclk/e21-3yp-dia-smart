import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress, List, ListItem, ListItemText, ListItemIcon } from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import TaskAltIcon from "@mui/icons-material/TaskAlt";

import { caregiverService } from "../../services/caregiverService";
import type { Alert } from "../../types/alert";

interface TimelineCardProps {
  patientId: number;
}

const TimelineCard = ({ patientId }: TimelineCardProps) => {
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState<any[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchActivities = async () => {
      try {
        const alerts = await caregiverService.getAlerts();
        const patientAlerts = alerts
          .filter((a) => a.patientId === patientId)
          .slice(0, 5) // Show top 5 recent events
          .map((alert: Alert) => {
            let icon = <NotificationsIcon color="warning" />;
            let statusText = `Alert Raised`;
            if (alert.status === "ACKNOWLEDGED") {
              icon = <CheckCircleIcon color="info" />;
              statusText = `Alert Acknowledged`;
            } else if (alert.status === "RESOLVED") {
              icon = <TaskAltIcon color="success" />;
              statusText = `Alert Resolved`;
            }

            return {
              id: alert.alertId,
              primary: alert.title,
              secondary: `${alert.message} • ${statusText}`,
              timeText: new Date(alert.createdAt).toLocaleString(),
              icon
            };
          });

        setActivities(patientAlerts);
      } catch (err) {
        console.error("Failed to load timeline feed", err);
      } finally {
        setLoading(false);
      }
    };
    fetchActivities();
  }, [patientId]);

  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Patient Activity Timeline
        </Typography>

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4, flexGrow: 1, alignItems: "center" }}>
            <CircularProgress size={32} />
          </Box>
        ) : activities.length === 0 ? (
          <Typography color="text.secondary" variant="body2" sx={{ py: 2 }}>
            No recent activity logged for this patient.
          </Typography>
        ) : (
          <List disablePadding>
            {activities.map((act) => (
              <ListItem key={act.id} disableGutters sx={{ py: 1, borderBottom: "1px solid", borderColor: "divider", "&:last-child": { borderBottom: 0 } }}>
                <ListItemIcon sx={{ minWidth: 40 }}>
                  {act.icon}
                </ListItemIcon>
                <ListItemText
                  primary={<Typography variant="body2" sx={{ fontWeight: "medium" }}>{act.primary}</Typography>}
                  secondary={<Typography variant="caption" color="text.secondary">{act.secondary} • {act.timeText}</Typography>}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
};

export default TimelineCard;
