import {
  Alert,
  Box,
  Button,
  Card,
  CardContent,
  MenuItem,
  TextField,
  Typography,
} from "@mui/material";

import { useState } from "react";
import { useNavigate } from "react-router-dom";

import {
  authService
} from "../../services/authService";

import type { RegisterRequest } from "../../services/authService";

const RegisterPage = () => {
  const navigate = useNavigate();

  const [form, setForm] =
    useState<RegisterRequest>({
      displayName: "",
      email: "",
      password: "",
      role: "PATIENT",
      contactNumber: "",
    });

  const [loading, setLoading] =
    useState(false);

  const [success, setSuccess] =
    useState("");

  const [error, setError] =
    useState("");

  const handleRegister =
    async () => {
      try {
        setLoading(true);
        setError("");
        setSuccess("");

        await authService.register(
          form
        );

        setSuccess(
          "Account created successfully"
        );

        setTimeout(() => {
          navigate("/login");
        }, 1500);
      } catch (err: any) {
        console.error(err);

        setError(
          err.response?.data?.message ??
            "Registration failed"
        );
      } finally {
        setLoading(false);
      }
    };

  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        justifyContent:
          "center",
        alignItems: "center",
        bgcolor: "#f4f6f8",
      }}
    >
      <Card sx={{ width: 500 }}>
        <CardContent>
          <Typography
            variant="h4"
            sx={{
              textAlign: "center",
              mb: 3,
            }}
          >
            Create Account
          </Typography>

          {success && (
            <Alert
              severity="success"
              sx={{ mb: 2 }}
            >
              {success}
            </Alert>
          )}

          {error && (
            <Alert
              severity="error"
              sx={{ mb: 2 }}
            >
              {error}
            </Alert>
          )}

          <TextField
            label="Display Name"
            fullWidth
            margin="normal"
            value={form.displayName}
            onChange={(e) =>
              setForm({
                ...form,
                displayName:
                  e.target.value,
              })
            }
          />

          <TextField
            label="Email"
            fullWidth
            margin="normal"
            value={form.email}
            onChange={(e) =>
              setForm({
                ...form,
                email:
                  e.target.value,
              })
            }
          />

          <TextField
            label="Password"
            type="password"
            fullWidth
            margin="normal"
            value={form.password}
            onChange={(e) =>
              setForm({
                ...form,
                password:
                  e.target.value,
              })
            }
          />

          <TextField
            select
            label="Role"
            fullWidth
            margin="normal"
            value={form.role}
            onChange={(e) =>
              setForm({
                ...form,
                role:
                  e.target.value as
                    | "PATIENT"
                    | "CAREGIVER"
                    | "DOCTOR",
              })
            }
          >
            <MenuItem value="PATIENT">
              Patient
            </MenuItem>

            <MenuItem value="CAREGIVER">
              Caregiver
            </MenuItem>

            <MenuItem value="DOCTOR">
              Doctor
            </MenuItem>
          </TextField>

          <TextField
            label="Contact Number"
            fullWidth
            margin="normal"
            value={
              form.contactNumber
            }
            onChange={(e) =>
              setForm({
                ...form,
                contactNumber:
                  e.target.value,
              })
            }
          />

          <Button
            variant="contained"
            fullWidth
            sx={{ mt: 3 }}
            onClick={
              handleRegister
            }
            disabled={loading}
          >
            {loading
              ? "Creating..."
              : "Register"}
          </Button>

          <Button
            fullWidth
            sx={{ mt: 1 }}
            onClick={() =>
              navigate("/login")
            }
          >
            Already have an account?
            Login
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default RegisterPage;