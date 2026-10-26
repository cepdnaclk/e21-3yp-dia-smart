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
import { BrandWordmark } from "../../components/common/BrandLogo";
import {
  authBackdropSx,
  authCardSx,
  authLinkButtonSx,
  authPrimaryButtonSx,
} from "../../components/common/authStyles";

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

      if (form.role === "DOCTOR") {
        setSuccess("Registered successfully and sent to admin review.");
      } else {
        setSuccess("Account created successfully");
      }

      const redirectDelay = form.role === "DOCTOR" ? 3000 : 1500;
      setTimeout(() => {
        navigate("/login");
      }, redirectDelay);
    } catch (err: any) {
      console.error(err);
      setError(err.response?.data?.message ?? "Registration failed");
    } finally {
      setLoading(false);
    }
  };

  return (
    <Box sx={authBackdropSx}>
      <Card sx={{ ...authCardSx, maxWidth: 480 }}>
        <CardContent sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
          {/* Logo and Header */}
          <BrandWordmark width={{ xs: 180, sm: 210 }} sx={{ mb: 1, mt: -1 }} />

          <Typography
            variant="h5"
            sx={{ fontWeight: 800, color: "#12233b", mb: 0.5, textAlign: "center" }}
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
            sx={{ ...authPrimaryButtonSx, mt: 3 }}
            onClick={handleRegister}
            disabled={loading}
          >
            {loading ? "Creating Account..." : "Register"}
          </Button>

          <Button
            variant="text"
            fullWidth
            sx={{ ...authLinkButtonSx, mt: 2 }}
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