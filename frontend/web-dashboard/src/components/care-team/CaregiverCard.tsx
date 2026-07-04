import {
  Card,
  CardContent,
  Typography,
} from "@mui/material";

const CaregiverCard = () => {
  return (
    <Card>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 1 }}
        >
          Caregivers
        </Typography>

        {/* TODO: Display approved caregivers from careTeamService when backend endpoints are available. */}
        <Typography color="text.secondary">
          Approved caregiver information will appear here.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default CaregiverCard;
