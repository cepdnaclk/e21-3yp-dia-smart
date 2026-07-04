import {
  Card,
  CardContent,
  Typography,
} from "@mui/material";

const DoctorCard = () => {
  return (
    <Card>
      <CardContent>
        <Typography
          variant="h6"
          sx={{ mb: 1 }}
        >
          Doctors
        </Typography>

        {/* TODO: Display assigned doctors from careTeamService when backend endpoints are available. */}
        <Typography color="text.secondary">
          Assigned doctor information will appear here.
        </Typography>
      </CardContent>
    </Card>
  );
};

export default DoctorCard;
