import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
  Alert,
  InputAdornment,
  IconButton,
} from "@mui/material";

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";

import { authService } from "../../services/authService";
import { useAuth } from "../../context/AuthContext";
import { UserRole } from "../../types/roles";
import { DEFAULT_ROLE_ROUTES } from "../../config/routes/roleRoutes";
import { patientAccessService } from "../../services/patientAccessService";
import { BrandWordmark } from "../../components/common/BrandLogo";
import {
  authBackdropSx,
  authCardSx,
  authLinkButtonSx,
  authPrimaryButtonSx,
} from "../../components/common/authStyles";

const LoginPage = () => {
  const navigate = useNavigate();
  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  const handleLogin = async () => {
    try {
      setLoading(true);
      setError("");

      const data = await authService.login(email, password);

      login(data.accessToken, data.user.role, data.user.userId);

      if (data.user.role !== UserRole.DOCTOR && data.user.role !== UserRole.CAREGIVER) {
        const accesses = await patientAccessService.getMyPatientAccess();
        if (accesses && accesses.length > 0) {
          localStorage.setItem("patientId", accesses[0].patientId.toString());
        }
      }

      const targetRoute = DEFAULT_ROLE_ROUTES[data.user.role] || "/dashboard";
      navigate(targetRoute);
    } catch (err: any) {
      console.error("LOGIN ERROR:", err);
      setError("Invalid email or password");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={authBackdropSx}>
      <Card sx={{ ...authCardSx, maxWidth: 440 }}>
        <CardContent sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
          {/* Logo and Header */}
          <BrandWordmark width={{ xs: 180, sm: 210 }} sx={{ mb: 1, mt: -1 }} />

          <Typography
            variant="h5"
            sx={{ fontWeight: 800, color: "#12233b", mb: 0.5, textAlign: "center" }}
          >
            Welcome to Dia-Smart
          </Typography>

          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mb: 4, fontWeight: 500 }}
          >
            Sign in to access your diabetes compliance ecosystem
          </Typography>

          {error && (
            <Alert
              severity="error"
              sx={{ mb: 3, width: "100%", borderRadius: 2 }}
            >
              {error}
            </Alert>
          )}

          {/* Form Fields */}
          <TextField
            label="Email Address"
            fullWidth
            margin="normal"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
          />

          <TextField
            label="Password"
            type={showPassword ? "text" : "password"}
            fullWidth
            margin="normal"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            slotProps={{
              input: {
                endAdornment: (
                  <InputAdornment position="end">
                    <IconButton
                      aria-label="toggle password visibility"
                      onClick={() => setShowPassword(!showPassword)}
                      edge="end"
                    >
                      {showPassword ? <VisibilityOff /> : <Visibility />}
                    </IconButton>
                  </InputAdornment>
                ),
              },
            }}
          />

          <Box sx={{ alignSelf: "flex-end", mt: 0.5 }}>
            <Button
              variant="text"
              size="small"
              onClick={() => navigate("/forgot-password")}
              sx={authLinkButtonSx}
            >
              Forgot Password?
            </Button>
          </Box>

          {/* Action Buttons */}
          <Button
            variant="contained"
            fullWidth
            size="large"
            sx={{ ...authPrimaryButtonSx, mt: 3 }}
            onClick={handleLogin}
            disabled={loading}
          >
            {loading ? "Signing In..." : "Sign In"}
          </Button>

          <Button
            variant="outlined"
            fullWidth
            size="large"
            sx={{
              mt: 2,
              py: 1.5,
              borderRadius: 3,
              borderColor: "#e2e8f0",
              color: "#12233b",
              fontWeight: 700,
              fontSize: "1rem",
              textTransform: "none",
              "&:hover": {
                borderColor: "#cbd5e1",
                backgroundColor: "rgba(0,0,0,0.01)",
              },
            }}
            onClick={() => navigate("/register")}
          >
            Create Account
          </Button>

          <Button
            variant="text"
            sx={{ mt: 3, color: "text.secondary", fontWeight: 600 }}
            onClick={() => navigate("/")}
          >
            Back to Home
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default LoginPage;