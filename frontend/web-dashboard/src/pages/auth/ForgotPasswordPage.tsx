import {
  Box,
  Button,
  Card,
  CardContent,
  TextField,
  Typography,
} from "@mui/material";
import { useNavigate } from "react-router-dom";
import { BrandWordmark } from "../../components/common/BrandLogo";
import {
  authBackdropSx,
  authCardSx,
  authLinkButtonSx,
  authPrimaryButtonSx,
} from "../../components/common/authStyles";

const ForgotPasswordPage = () => {
  const navigate = useNavigate();

  return (
    <Box sx={authBackdropSx}>
      <Card sx={{ ...authCardSx, maxWidth: 420 }}>
        <CardContent sx={{ display: "flex", flexDirection: "column", alignItems: "center" }}>
          {/* Logo and Header */}
          <BrandWordmark width={{ xs: 180, sm: 210 }} sx={{ mb: 1, mt: -1 }} />

          <Typography
            variant="h5"
            sx={{ fontWeight: 800, color: "#12233b", mb: 0.5, textAlign: "center" }}
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
            sx={{ ...authPrimaryButtonSx, mt: 3 }}
          >
            Send Reset Link
          </Button>

          <Button
            variant="text"
            fullWidth
            sx={{ ...authLinkButtonSx, mt: 2 }}
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