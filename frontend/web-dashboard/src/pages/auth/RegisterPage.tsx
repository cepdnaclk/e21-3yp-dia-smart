import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  MenuItem,
  TextField,
  Typography,
  InputAdornment,
  IconButton,
} from "@mui/material";

import { useState } from "react";
import { useNavigate } from "react-router-dom";
import Visibility from "@mui/icons-material/Visibility";
import VisibilityOff from "@mui/icons-material/VisibilityOff";

import { authService } from "../../services/authService";
import type { RegisterRequest } from "../../services/authService";
import logo from "../../assets/logo/diasmart-logo.png";

const RegisterPage = () => {
  const navigate = useNavigate();

  const [form, setForm] = useState<RegisterRequest>({
    displayName: "",
    email: "",
    password: "",
    role: "PATIENT",
    contactNumber: "",
  });

  const [showPassword, setShowPassword] = useState(false);
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState("");
  const [error, setError] = useState("");

  const handleRegister = async () => {
    try {
      setLoading(true);
      setError("");
      setSuccess("");

      await authService.register(form);

      setSuccess("Account created successfully");

      setTimeout(() => {
        navigate("/login");
      }, 1500);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message ?? "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
        backgroundColor: "#f8f9fa",
        py: 4,
        px: 2,
      }}
    >
      <Card sx={{ width: "100%", maxWidth: 480, borderRadius: 4, p: 1 }}>
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
            Create Account
          </Typography>

          <Typography
            variant="body2"
            color="text.secondary"
            sx={{ mb: 3, fontWeight: 500, textAlign: "center" }}
          >
            Join Dia-Smart to manage your compliance ecosystem
          </Typography>

          {success && (
            <Alert
              severity="success"
              sx={{ mb: 2, width: "100%", borderRadius: 2 }}
            >
              {success}
            </Alert>
          )}

          {error && (
            <Alert
              severity="error"
              sx={{ mb: 2, width: "100%", borderRadius: 2 }}
            >
              {error}
            </Alert>
          )}

          {/* Form Fields */}
          <TextField
            label="Display Name"
            fullWidth
            margin="dense"
            value={form.displayName}
            onChange={(e) =>
              setForm({
                ...form,
                displayName: e.target.value,
              })
            }
          />

          <TextField
            label="Email Address"
            fullWidth
            margin="dense"
            value={form.email}
            onChange={(e) =>
              setForm({
                ...form,
                email: e.target.value,
              })
            }
          />

          <TextField
            label="Password"
            type={showPassword ? "text" : "password"}
            fullWidth
            margin="dense"
            value={form.password}
            onChange={(e) =>
              setForm({
                ...form,
                password: e.target.value,
              })
            }
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

          <TextField
            select
            label="Role"
            fullWidth
            margin="dense"
            value={form.role}
            onChange={(e) =>
              setForm({
                ...form,
                role: e.target.value as "PATIENT" | "CAREGIVER" | "DOCTOR",
              })
            }
          >
            <MenuItem value="PATIENT">Patient</MenuItem>
            <MenuItem value="CAREGIVER">Caregiver</MenuItem>
            <MenuItem value="DOCTOR">Doctor</MenuItem>
          </TextField>

          <TextField
            label="Contact Number"
            fullWidth
            margin="dense"
            value={form.contactNumber}
            onChange={(e) =>
              setForm({
                ...form,
                contactNumber: e.target.value,
              })
            }
          />

          {/* Actions */}
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
            onClick={handleRegister}
            disabled={loading}
          >
            {loading ? "Creating Account..." : "Register"}
          </Button>

          <Button
            variant="text"
            fullWidth
            sx={{ mt: 2, color: "#3ec1fa", fontWeight: 700 }}
            onClick={() => navigate("/login")}
          >
            Already have an account? Sign In
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default RegisterPage;