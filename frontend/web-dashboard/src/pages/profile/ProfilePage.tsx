import {
  Avatar,
  Box,
  Card,
  CardContent,
  Divider,
  Grid,
  Typography,
} from "@mui/material";

const ProfilePage = () => {
  const patient = {
    name: "John Silva",
    age: 68,
    gender: "Male",
    diabetesType: "Type 2",
    doctor: "Dr. Perera",
    caregiver: "Mary Silva",
    emergencyContact: "+94 77 123 4567",
  };

  return (
    <>
      <Typography variant="h4" sx={{ mb: 3 }}>
        Profile
      </Typography>

      <Card>
        <CardContent>
          <Box
            display="flex"
            flexDirection="column"
            alignItems="center"
            mb={3}
          >
            <Avatar
              sx={{
                width: 100,
                height: 100,
                fontSize: 36,
                bgcolor: "primary.main",
              }}
            >
              J
            </Avatar>

            <Typography variant="h5" mt={2}>
              {patient.name}
            </Typography>

            <Typography color="text.secondary">
              Patient
            </Typography>
          </Box>

          <Divider sx={{ mb: 3 }} />

          <Grid container spacing={3}>
            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Age:</strong> {patient.age}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Gender:</strong> {patient.gender}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Diabetes Type:</strong>{" "}
                {patient.diabetesType}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Doctor:</strong> {patient.doctor}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Caregiver:</strong>{" "}
                {patient.caregiver}
              </Typography>
            </Grid>

            <Grid size={{ xs: 12, md: 6 }}>
              <Typography>
                <strong>Emergency Contact:</strong>{" "}
                {patient.emergencyContact}
              </Typography>
            </Grid>
          </Grid>
        </CardContent>
      </Card>
    </>
  );
};

export default ProfilePage;