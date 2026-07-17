import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import logo from "../../assets/logo/diasmart-logo.png";

const ForgotPasswordPage = () => {
  const navigate = useNavigate();

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "#f8f9fa",
        px: 2,
      }}
    >
      <Card sx={{ width: "100%", maxWidth: 420, borderRadius: 4, p: 1 }}>
        <CardContent sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
          {/* Logo and Header */}
          <Box
            component="img"
            src={logo}
            alt="Dia-Smart Logo"
            sx={{ width: 54, height: 54, borderRadius: 1.5, mb: 2 }}
          />

          <Typography
            variant="h5"
            sx={{ fontWeight: 800, color: "#12233b", mb: 0.5 }}
          >
            Reset Password
          </Typography>

          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mb: 3, fontWeight: 500, textAlign: "center" }}
          >
            Enter your email to receive a password reset link
          </Typography>

          <TextField
            label="Email Address"
            fullWidth
            margin="normal"
          />

          <Button
            variant="contained"
            fullWidth
            size="large"
            sx={{
              mt: 3,
              py: 1.5,
              borderRadius: 3,
              backgroundColor: "#12233b",
              fontWeight: 700,
              fontSize: "1rem",
              textTransform: "none",
              "&:hover": {
                backgroundColor: "#1b3559",
              },
            }}
          >
            Send Reset Link
          </Button>

          <Button
            variant="text"
            fullWidth
            sx={{ mt: 2, color: "#3ec1fa", fontWeight: 700 }}
            onClick={() => navigate("/login")}
          >
            Back to Sign In
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ForgotPasswordPage;