import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
  Alert,
} from "@mui/material";

import { useState } from "react";
import { useNavigate } from "react-router-dom";

import { authService } from "../../services/authService";
import { useAuth } from "../../context/AuthContext";
import { UserRole } from "../../types/roles";

import { patientAccessService } from "../../services/patientAccessService";

const LoginPage = () => {
  const navigate = useNavigate();

  const { login } = useAuth();

  const [email, setEmail] = useState("");
  const [password, setPassword] =
    useState("");

  const [loading, setLoading] =
    useState(false);

  const [error, setError] =
    useState("");

  // const handleLogin = async () => {
  //   try {
  //     setLoading(true);
  //     setError("");

  //     const data =
  //       await authService.login(
  //         email,
  //         password
  //       );

  //     login(
  //       data.accessToken,
  //       data.user.role
  //     );

  //     navigate("/dashboard");
  //   } catch (err) {
  //     console.error(err);

  //     setError(
  //       "Invalid email or password"
  //     );
  //   } finally {
  //     setLoading(false);
  //   }
  // };
  const handleLogin = async () => {
  try {
    setLoading(true);
    setError("");

    console.log("Attempting login:", {
      email,
      password,
    });

    const data =
      await authService.login(
        email,
        password
      );

    console.log(
      "Login API Success:",
      data
    );

    // Save token and role
    login(
      data.accessToken,
      data.user.role
    );

    if (data.user.role === UserRole.DOCTOR) {
      navigate("/doctor/dashboard");
    } else {
      // Get patient access records for non-doctor roles
      const accesses =
        await patientAccessService.getMyPatientAccess();

      console.log(
        "Patient Access:",
        accesses
      );

      if (
        accesses &&
        accesses.length > 0
      ) {
        localStorage.setItem(
          "patientId",
          accesses[0].patientId.toString()
        );
      }

      navigate("/dashboard");
    }
  } catch (err: any) {
    console.error(
      "LOGIN ERROR:",
      err
    );

    if (err.response) {
      console.log(
        "Status:",
        err.response.status
      );

      console.log(
        "Data:",
        err.response.data
      );
    }

    setError(
      "Invalid email or password"
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
        justifyContent: "center",
        alignItems: "center",
        bgcolor: "#f4f6f8",
      }}
    >
      <Card sx={{ width: 420 }}>
        <CardContent>
          <Typography
            variant="h4"
            sx={{ textAlign: "center" }}
            gutterBottom
          >
            Dia-Smart Login
          </Typography>

          <Typography
            color="text.secondary"
            sx={{ textAlign: "center", mb: 2 }}
          >
            Sign in to continue
          </Typography>

          {error && (
            <Alert
              severity="error"
              sx={{ mb: 2 }}
            >
              {error}
            </Alert>
          )}

          <TextField
            label="Email"
            fullWidth
            margin="normal"
            value={email}
            onChange={(e) =>
              setEmail(e.target.value)
            }
          />

          <TextField
            label="Password"
            type="password"
            fullWidth
            margin="normal"
            value={password}
            onChange={(e) =>
              setPassword(
                e.target.value
              )
            }
          />

          <Button
            variant="contained"
            fullWidth
            sx={{ mt: 3 }}
            onClick={handleLogin}
            disabled={loading}
          >
            {loading
              ? "Signing In..."
              : "Login"}
          </Button>

          <Button
          fullWidth
          sx={{ mt: 1 }}
          onClick={() =>
            navigate("/register")
          }
        >
          Create Account
        </Button>

          <Button
            fullWidth
            sx={{ mt: 1 }}
          >
            Forgot Password?
          </Button>
          <Button
          fullWidth
          sx={{ mt: 1 }}
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