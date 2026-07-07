import { Card, CardContent, Typography } from "@mui/material";

const AuditLogsSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Audit Logs
        </Typography>
        
        {/* TODO: Integrate administrative audit logs list query api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Security logging tracking access attempts, settings adjustments, device registrations, and data exports. Filterable by user, role, and event type.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Table: Audit Logs List]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default AuditLogsSection;
