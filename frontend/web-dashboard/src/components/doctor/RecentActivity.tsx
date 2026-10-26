import { useState, useEffect } from "react";
import { Card, CardContent, Typography, Box, CircularProgress, List, ListItem, ListItemText, ListItemIcon } from "@mui/material";
import NotificationsIcon from "@mui/icons-material/Notifications";
import CheckCircleIcon from "@mui/icons-material/CheckCircle";
import TaskAltIcon from "@mui/icons-material/TaskAlt";
import HistoryIcon from "@mui/icons-material/History";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import { doctorService } from "../../services/doctorService";
import type { Alert } from "../../types/alert";

const RecentActivity = () => {
  const [loading, setLoading] = useState(true);
  const [activities, setActivities] = useState<any[]>([]);

  useEffect(() => {
    const fetchActivities = async () => {
      try {
        const [patients, alerts] = await Promise.all([
          doctorService.getAssignedPatients(),
          doctorService.getAlerts()
        ]);

        // Map alerts to activities
        const mappedActivities = alerts
          .slice(0, 5) // Show top 5 recent activities
          .map((alert: Alert) => {
            const patient = patients.find((p) => p.patientId === alert.patientId);
            const patientName = patient ? patient.patientName : (alert.patientId ? `Patient ID: ${alert.patientId}` : "Unknown Patient");

            let icon = <NotificationsIcon color="warning" />;
            let primaryText = `${alert.title} alert raised for ${patientName}`;
            if (alert.status === "ACKNOWLEDGED") {
              icon = <CheckCircleIcon color="info" />;
              primaryText = `${patientName}'s alert acknowledged`;
            } else if (alert.status === "RESOLVED") {
              icon = <TaskAltIcon color="success" />;
              primaryText = `${patientName}'s alert resolved`;
            }

            return {
              id: alert.alertId,
              primary: primaryText,
              secondary: `${alert.message} • ${new Date(alert.createdAt).toLocaleString()}`,
              icon,
              time: new Date(alert.createdAt)
            };
          });

        setActivities(mappedActivities);
      } catch (err) {
        console.error("Failed to load activity logs", err);
      } finally {
        setLoading(false);
      }
    };

    fetchActivities();
  }, []);

  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", height: "100%" }}>
        <CardHeading title="Recent Activity" icon={<HistoryIcon />} color="#0284c7" />

        {loading ? (
          <Box sx={{ display: "flex", justifyContent: "center", alignItems: "center", py: 4, flexGrow: 1 }}>
            <CircularProgress size={32} sx={{ color: "secondary.dark" }} />
          </Box>
        ) : activities.length === 0 ? (
          <EmptyState message="No recent patient activities logged" icon={<HistoryIcon />} />
        ) : (
          <List disablePadding>
            {activities.map((act) => (
              <ListItem
                key={act.id}
                disableGutters
                sx={{
                  py: 1.25,
                  alignItems: "flex-start",
                  borderBottom: "1px solid",
                  borderColor: "divider",
                  "&:last-child": { borderBottom: 0 },
                }}
              >
                <ListItemIcon sx={{ minWidth: 0, mr: 1.5, mt: 0.25 }}>
                  <Box
                    sx={{
                      width: 34,
                      height: 34,
                      borderRadius: 2,
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "center",
                      bgcolor: "#f1f5f9",
                      "& .MuiSvgIcon-root": { fontSize: 18 },
                    }}
                  >
                    {act.icon}
                  </Box>
                </ListItemIcon>
                <ListItemText
                  sx={{ my: 0, minWidth: 0 }}
                  primary={<Typography variant="body2" sx={{ fontWeight: 600, color: "text.primary", overflowWrap: "anywhere" }}>{act.primary}</Typography>}
                  secondary={<Typography variant="caption" color="text.secondary" sx={{ display: "block", mt: 0.25, overflowWrap: "anywhere" }}>{act.secondary}</Typography>}
                />
              </ListItem>
            ))}
          </List>
        )}
      </CardContent>
    </Card>
  );
};

export default RecentActivity;
