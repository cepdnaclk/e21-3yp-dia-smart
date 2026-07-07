import { Card, CardContent, Typography } from "@mui/material";

const UserReportsSection = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="h6" sx={{ mb: 2, fontWeight: "medium" }}>
          User Accounts & Distribution Reports
        </Typography>
        
        {/* TODO: Integrate administrative user reports generation endpoints */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Statistical summary reports mapping active users distribution, clinician licensing audit reports, and registration timelines.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Registry: User Distribution Reports]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default UserReportsSection;
