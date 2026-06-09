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

    const data = await authService.login(
      email,
      password
    );

    console.log("Login API Success:", data);

    login(
      data.accessToken,
      data.user.role,
      data.user.patientId
    );

    console.log("AuthContext login success");

    navigate("/dashboard");
  } catch (err: any) {
  console.error("LOGIN ERROR:", err);

  if (err.response) {
    console.log("Status:", err.response.status);
    console.log("Data:", err.response.data);
  }

  setError("Login failed");
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
            textAlign="center"
            gutterBottom
          >
            Dia-Smart
          </Typography>

          <Typography
            color="text.secondary"
            textAlign="center"
            sx={{ mb: 3 }}
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
          >
            Forgot Password?
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default LoginPage;