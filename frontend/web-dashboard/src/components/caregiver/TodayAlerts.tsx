import { Card, CardContent, Typography, Box, List, ListItem, Chip } from "@mui/material";
import { alpha } from "@mui/material/styles";
import NotificationImportantOutlinedIcon from "@mui/icons-material/NotificationImportantOutlined";
import ErrorOutlineRoundedIcon from "@mui/icons-material/ErrorOutlineRounded";
import VerifiedOutlinedIcon from "@mui/icons-material/VerifiedOutlined";

import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";
import type { Alert } from "../../types/alert";

interface TodayAlertsProps {
  alerts: Alert[];
}

const ACCENT = "#ef4444";

const TodayAlerts: React.FC<TodayAlertsProps> = ({ alerts }) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column" }}>
        <CardHeading title="Today's Alerts" icon={<NotificationImportantOutlinedIcon />} color={ACCENT} />

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.25, p: 0 }}>
          {alerts.map((alert) => (
            <ListItem
              key={alert.alertId}
              sx={{
                p: 1.5,
                bgcolor: alpha(ACCENT, 0.05),
                border: "1px solid",
                borderColor: alpha(ACCENT, 0.18),
                borderLeft: `3px solid ${ACCENT}`,
                borderRadius: 2,
                alignItems: "flex-start",
                gap: 1.25,
              }}
            >
              <ErrorOutlineRoundedIcon sx={{ color: ACCENT, fontSize: 20, mt: 0.25, flexShrink: 0 }} />
              <Box sx={{ display: "flex", flexDirection: "column", gap: 0.5, minWidth: 0, flexGrow: 1 }}>
                <Box sx={{ display: "flex", justifyContent: "space-between", width: "100%", alignItems: "center", gap: 1, flexWrap: "wrap" }}>
                  <Typography variant="subtitle2" sx={{ fontWeight: 700, color: "text.primary", overflowWrap: "anywhere" }}>
                    {alert.title}
                  </Typography>
                  <Chip
                    label={alert.severity}
                    size="small"
                    color="error"
                    sx={{ fontSize: 10, fontWeight: 700, height: 20 }}
                  />
                </Box>
                <Typography variant="caption" sx={{ color: "text.secondary", overflowWrap: "anywhere" }}>
                  {alert.message}
                </Typography>
              </Box>
            </ListItem>
          ))}

          {alerts.length === 0 && (
            <EmptyState
              message="No critical patient warnings registered today."
              icon={<VerifiedOutlinedIcon sx={{ color: "#10b981" }} />}
              minHeight={140}
            />
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default TodayAlerts;
