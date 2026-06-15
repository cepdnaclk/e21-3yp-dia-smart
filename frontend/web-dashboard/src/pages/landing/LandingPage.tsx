import {
  Box,
  Button,
  Card,
  CardContent,
  Container,
  Grid,
  Typography,
} from "@mui/material";

import {
  Favorite,
  Thermostat,
  Warning,
  Medication,
} from "@mui/icons-material";

import { Navigate, useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";

const LandingPage = () => {
  const navigate = useNavigate();
  const { isAuthenticated } = useAuth();

  if (isAuthenticated) {
  return (
    <Navigate
      to="/dashboard"
      replace
    />
  );
}

  return (
    <Box
      sx={{
        minHeight: "100vh",
        bgcolor: "#f4f6f8",
      }}
    >
      {/* Hero Section */}
      <Box
        sx={{
          py: 12,
          textAlign: "center",
          background:
            "linear-gradient(135deg,#3E5C89,#4CB5E8)",
          color: "white",
        }}
      >
        <Container maxWidth="lg">
          <Typography
            variant="h2"
            component="h1"
            sx={{ fontWeight: 700 }}
            gutterBottom
          >
            Dia Smart
          </Typography>

          <Typography
            variant="h5"
            sx={{
              mb: 4,
              maxWidth: 800,
              mx: "auto",
            }}
          >
            Smart Diabetes Monitoring and
            Insulin Management Platform
          </Typography>

          <Typography
            sx={{
              mb: 5,
              maxWidth: 900,
              mx: "auto",
              opacity: 0.95,
            }}
          >
            Monitor glucose readings,
            insulin inventory,
            storage conditions and
            medication adherence through
            a complete IoT-enabled
            healthcare ecosystem.
          </Typography>

          <Box
            sx={{
              display: "flex",
              gap: 2,
              justifyContent: "center",
              mt: 4,
            }}
          >
            <Button
              variant="contained"
              size="large"
              onClick={() =>
                navigate("/login")
              }
            >
              Login
            </Button>

            <Button
              variant="outlined"
              size="large"
              onClick={() =>
                navigate("/register")
              }
            >
              Register
            </Button>

            <Button
            variant="outlined"
            size="large"
            sx={{
              color: "white",
              borderColor: "white",
            }}
          >
            Learn More
          </Button>
          </Box>

          
        </Container>
      </Box>

      {/* Features */}
      <Container
        maxWidth="lg"
        sx={{ py: 8 }}
      >
        <Typography
          variant="h4"
          align="center"
          gutterBottom
        >
          Key Features
        </Typography>

        <Grid
          container
          spacing={3}
          sx={{ mt: 2 }}
        >
          <Grid
            size={{
              xs: 12,
              md: 6,
              lg: 3,
            }}
          >
            <Card>
              <CardContent>
                <Favorite
                  color="primary"
                  sx={{ fontSize: 50 }}
                />

                <Typography
                  variant="h6"
                  sx={{ mt: 2 }}
                >
                  Glucose Monitoring
                </Typography>

                <Typography>
                  Track glucose readings
                  from BLE-enabled
                  glucometers.
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid
            size={{
              xs: 12,
              md: 6,
              lg: 3,
            }}
          >
            <Card>
              <CardContent>
                <Thermostat
                  color="primary"
                  sx={{ fontSize: 50 }}
                />

                <Typography
                  variant="h6"
                  sx={{ mt: 2 }}
                >
                  Smart Storage
                </Typography>

                <Typography>
                  Monitor temperature,
                  humidity and storage
                  conditions in real time.
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid
            size={{
              xs: 12,
              md: 6,
              lg: 3,
            }}
          >
            <Card>
              <CardContent>
                <Warning
                  color="primary"
                  sx={{ fontSize: 50 }}
                />

                <Typography
                  variant="h6"
                  sx={{ mt: 2 }}
                >
                  Real-Time Alerts
                </Typography>

                <Typography>
                  Get notified about
                  missed doses, low
                  inventory and unsafe
                  storage conditions.
                </Typography>
              </CardContent>
            </Card>
          </Grid>

          <Grid
            size={{
              xs: 12,
              md: 6,
              lg: 3,
            }}
          >
            <Card>
              <CardContent>
                <Medication
                  color="primary"
                  sx={{ fontSize: 50 }}
                />

                <Typography
                  variant="h6"
                  sx={{ mt: 2 }}
                >
                  Prescription Management
                </Typography>

                <Typography>
                  Support doctors,
                  caregivers and patients
                  with medication tracking.
                </Typography>
              </CardContent>
            </Card>
          </Grid>
        </Grid>
      </Container>

      {/* Architecture */}
      <Box
        sx={{
          bgcolor: "white",
          py: 8,
        }}
      >
        <Container maxWidth="lg">
          <Typography
            variant="h4"
            align="center"
            gutterBottom
          >
            System Architecture
          </Typography>

          <Typography
            align="center"
            sx={{ mt: 3 }}
          >
            BLE Glucometer
          </Typography>

          <Typography
            align="center"
            variant="h5"
          >
            ↓
          </Typography>

          <Typography
            align="center"
            sx={{ mt: 1 }}
          >
            Smart Insulin Storage Unit
          </Typography>

          <Typography
            align="center"
            variant="h5"
          >
            ↓
          </Typography>

          <Typography
            align="center"
            sx={{ mt: 1 }}
          >
            Spring Boot Backend
          </Typography>

          <Typography
            align="center"
            variant="h5"
          >
            ↓
          </Typography>

          <Typography
            align="center"
            sx={{ mt: 1 }}
          >
            React Web Dashboard
          </Typography>
        </Container>
      </Box>

      {/* Footer */}
      <Box
        sx={{
          py: 4,
          textAlign: "center",
          bgcolor: "#3E5C89",
          color: "white",
        }}
      >
        <Typography>
          DiaSmart • Smart Diabetes
          Monitoring Platform
        </Typography>

        <Typography variant="body2">
          Final Year Design Project
        </Typography>
      </Box>
    </Box>
  );
};

export default LandingPage;