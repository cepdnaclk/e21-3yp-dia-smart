import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
} from "@mui/material";

const ForgotPasswordPage = () => {
  return (
    <Box
      sx={{
        minHeight: "100vh",
        display: "flex",
        justifyContent: "center",
        alignItems: "center",
      }}
    >
      <Card sx={{ width: 420 }}>
        <CardContent>
          <Typography
            variant="h5"
            gutterBottom
          >
            Reset Password
          </Typography>

          <TextField
            label="Email Address"
            fullWidth
            margin="normal"
          />

          <Button
            variant="contained"
            fullWidth
            sx={{ mt: 2 }}
          >
            Send Reset Link
          </Button>
        </CardContent>
      </Card>
    </Box>
  );
};

export default ForgotPasswordPage;