import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress, List, ListItem, ListItemText, ListItemIcon } from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import TaskAltIcon from "@mui/icons-material/TaskAlt";
import TimelineOutlinedIcon from "@mui/icons-material/TimelineOutlined";
import HistoryIcon from "@mui/icons-material/History";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

import { caregiverService } from "../../services/caregiverService";
import type { Alert } from "../../types/alert";

interface TimelineCardProps {
  patientId: number;
  refreshTrigger?: number;
}

const TimelineCard = ({ patientId, refreshTrigger }: TimelineCardProps) => {
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState<any[]>([]);

  useEffect(() => {
    if (!patientId) return;
    const fetchActivities = async (silent = false) => {
      try {
        if (!silent) setLoading(true);
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
        if (!silent) setLoading(false);
      }
    };

    const isInitial = !refreshTrigger || refreshTrigger === 0;
    fetchActivities(!isInitial);
  }, [patientId, refreshTrigger]);

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <CardHeading title="Patient Activity Timeline" icon={<TimelineOutlinedIcon />} color="#12233b" />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", py: 4, flexGrow: 1, alignItems: "center" }}>
            <CircularProgress size={28} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : activities.length === 0 ? (
          <EmptyState message="No recent activity logged for this patient." icon={<HistoryIcon />} />
        ) : (
          <List disablePadding>
            {activities.map((act) => (
              <ListItem key={act.id} disableGutters sx={{ py: 1.25, alignItems: "flex-start", borderBottom: "1px solid", borderColor: "divider", "&:last-child": { borderBottom: 0 } }}>
                <ListItemIcon sx={{ minWidth: 44, mt: 0.25 }}>
                  <Box
                    sx={{
                      width: 32,
                      height: 32,
                      borderRadius: "50%",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      bgcolor: "#f8fafc",
                      border: "1px solid",
                      borderColor: "divider",
                      "& .MuiSvgIcon-root": { fontSize: 18 },
                    }}
                  >
                    {act.icon}
                  </Box>
                </ListItemIcon>
                <ListItemText
                  sx={{ m: 0, minWidth: 0 }}
                  primary={<Typography variant="body2" sx={{ fontWeight: 700, color: "#12233b", overflowWrap: "anywhere" }}>{act.primary}</Typography>}
                  secondary={<Typography variant="caption" color="text.secondary" sx={{ overflowWrap: "anywhere" }}>{act.secondary} • {act.timeText}</Typography>}
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
