import { Card, CardContent, Typography } from "@mui/material";

const AdministratorsSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          Administrators Directory
        </Typography>
        
        {/* TODO: Integrate Administrator users list management api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Lists all administrator accounts. Manage admin credentials, audit visibility rights, and system settings authorizations.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Table: Admin Accounts]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default AdministratorsSection;
