import { Card, CardContent, Typography } from "@mui/material";

const TotalUsersCard = () => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 2 }}>
      <CardContent>
        <Typography variant="subtitle2" color="text.secondary" gutterBottom>
          Total Users
        </Typography>
        
        {/* TODO: Integrate total users count statistics api */}
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Total registered users across all accounts including patients, clinicians, caregivers, and admins.
        </Typography>
        <Typography variant="caption" color="text.disabled" sx={{ display: "block" }}>
          [Placeholder Count: --]
        </Typography>
      </CardContent>
    </Card>
  );
};

export default TotalUsersCard;
