import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
} from "@mui/material";

const LoginPage = () => {
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

          <TextField
            label="Email"
            fullWidth
            margin="normal"
          />

          <TextField
            label="Password"
            type="password"
            fullWidth
            margin="normal"
          />

          <Button
            variant="contained"
            fullWidth
            sx={{ mt: 3 }}
          >
            Login
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