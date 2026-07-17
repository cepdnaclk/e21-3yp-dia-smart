import { Card, CardContent, Typography, List, ListItem, ListItemText, Box, Divider } from "@mui/material";
import type { AuditLogRecord } from "../../types/admin";

interface RecentActivityCardProps {
  logs: AuditLogRecord[];
}

const RecentActivityCard: React.FC<RecentActivityCardProps> = ({ logs }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 2 }}>
        <Typography variant="h6" sx={{ fontWeight: "bold" }}>
          Recent Activity & Audit Logs
        </Typography>

        <List sx={{ display: "flex", flexDirection: "column", gap: 1.5, p: 0 }}>
          {logs.map((log) => (
            <Box key={log.auditLogId}>
              <ListItem sx={{ p: 0, alignItems: "flex-start", gap: 2 }}>
                <Box
                  sx={{
                    px: 1.5,
                    py: 0.5,
                    bgcolor: "action.selected",
                    borderRadius: 1,
                    fontSize: 11,
                    fontWeight: "bold",
                    color: "text.secondary",
                    minWidth: 70,
                    textAlign: "center"
                  }}
                >
                  {log.actionType}
                </Box>
                <ListItemText
                  primary={
                    <Typography variant="body2" sx={{ fontWeight: "medium" }}>
                      Modified {log.entityType} (Entity ID: {log.entityId || "N/A"})
                    </Typography>
                  }
                  secondary={
                    <Typography variant="caption" color="text.secondary">
                      Date: {new Date(log.createdAt).toLocaleString()} • IP: {log.ipAddress || "Localhost"}
                    </Typography>
                  }
                  sx={{ m: 0 }}
                />
              </ListItem>
              <Divider sx={{ mt: 1.5 }} />
            </Box>
          ))}

          {logs.length === 0 && (
            <Typography variant="body2" color="text.secondary" sx={{ py: 3, textAlign: "center" }}>
              No recent audit logs registered in system datastore.
            </Typography>
          )}
        </List>
      </CardContent>
    </Card>
  );
};

export default RecentActivityCard;
