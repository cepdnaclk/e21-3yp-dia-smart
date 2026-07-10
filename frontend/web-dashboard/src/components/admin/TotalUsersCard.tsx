import { Card, CardContent, Typography } from "@mui/material";

interface TotalUsersCardProps {
  count: number;
}

const TotalUsersCard: React.FC<TotalUsersCardProps> = ({ count }) => {
  return (
    <Card elevation={2} sx={{ height: "100%", borderRadius: 3 }}>
      <CardContent sx={{ display: "flex", flexDirection: "column", gap: 1.5 }}>
        <Typography variant="subtitle2" color="text.secondary">
          Total Registered Users
        </Typography>
        <Typography variant="h4" sx={{ fontWeight: "bold", color: "primary.main" }}>
          {count}
        </Typography>
        <Typography variant="body2" color="text.secondary">
          Total registered users across patients, clinicians, caregivers, and administrators.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default TotalUsersCard;
