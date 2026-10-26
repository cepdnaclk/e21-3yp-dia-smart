import { Card, CardContent, Typography, List, ListItem, Box, Chip } from "@mui/material";
import HistoryIcon from "@mui/icons-material/History";
import type { AuditLogRecord } from "../../types/admin";
import CardHeading from "../common/CardHeading";
import EmptyState from "../common/EmptyState";

interface RecentActivityCardProps {
  logs: AuditLogRecord[];
}

const RecentActivityCard: React.FC<RecentActivityCardProps> = ({ logs }) => {
  return (
    <Card sx={{ height: "100%" }}>
      <CardContent sx={{ display: "flex", flexDirection: "column" }}>
        <CardHeading
          title="Recent Activity & Audit Logs"
          icon={<HistoryIcon />}
          color="#12233b"
        />

        <List sx={{ display: "flex", flexDirection: "column", p: 0 }}>
          {logs.map((log, index) => {
            const isLast = index === logs.length - 1;
            return (
              <ListItem
                key={log.auditLogId}
                sx={{ p: 0, alignItems: "stretch", gap: { xs: 1.5, sm: 2 } }}
              >
                {/* Timeline rail */}
                <Box
                  aria-hidden
                  sx={{
                    display: "flex",
                    flexDirection: "column",
                    alignItems: "center",
                    pt: 0.75,
                    flexShrink: 0,
                  }}
                >
                  <Box
                    sx={{
                      width: 10,
                      height: 10,
                      borderRadius: "50%",
                      bgcolor: "#3ec1fa",
                      boxShadow: "0 0 0 3px rgba(62, 193, 250, 0.18)",
                    }}
                  />
                  {!isLast && (
                    <Box sx={{ flexGrow: 1, width: 2, mt: 0.75, bgcolor: "divider", borderRadius: 1 }} />
                  )}
                </Box>

                <Box
                  sx={{
                    flex: 1,
                    minWidth: 0,
                    pb: isLast ? 0 : 2.25,
                    display: "flex",
                    flexDirection: { xs: "column", sm: "row" },
                    alignItems: "flex-start",
                    gap: { xs: 0.75, sm: 1.5 },
                  }}
                >
                  <Chip
                    label={log.actionType}
                    size="small"
                    variant="outlined"
                    sx={{
                      minWidth: 70,
                      flexShrink: 0,
                      fontSize: 11,
                      color: "#12233b",
                      bgcolor: "#f1f5f9",
                      borderColor: "#e2e8f0",
                    }}
                  />
                  <Box sx={{ minWidth: 0 }}>
                    <Typography
                      variant="body2"
                      sx={{ fontWeight: 600, color: "text.primary", overflowWrap: "anywhere" }}
                    >
                      Modified {log.entityType} (Entity ID: {log.entityId || "N/A"})
                    </Typography>
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      component="p"
                      sx={{ mt: 0.25, overflowWrap: "anywhere" }}
                    >
                      Date: {new Date(log.createdAt).toLocaleString()} • IP: {log.ipAddress || "Localhost"}
                    </Typography>
                  </Box>
                </Box>
              </ListItem>
            );
          })}

          {logs.length === 0 && (
            <EmptyState
              message="No recent audit logs registered in system datastore."
              icon={<HistoryIcon />}
            />
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default RecentActivityCard;
