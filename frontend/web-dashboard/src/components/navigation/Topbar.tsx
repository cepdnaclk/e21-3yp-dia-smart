import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  IconButton,
  Avatar,
} from "@mui/material";

import NotificationsIcon from "@mui/icons-material/Notifications";

import logo from "../../assets/logo/diasmart-logo.png";

import { useAuth } from "../../context/AuthContext";

import LogoutIcon from "@mui/icons-material/Logout";
import { useNavigate } from "react-router-dom";
import HomeIcon from "@mui/icons-material/Home";
import Button from "@mui/material/Button";

const Topbar = () => {
  const { role,logout } = useAuth();
  const navigate = useNavigate();
  const handleLogout = () => {
    logout();
    navigate("/");
  };
  const handleHome = () => {
    navigate("/");
  };

  return (
    <AppBar
      position="fixed"
      sx={{
        backgroundColor: "#3B567C",
      }}
    >
      <Toolbar>
        <Box
          component="img"
          src={logo}
          alt="Dia-Smart Logo"
          sx={{
            width: 40,
            height: 40,
            mr: 2,
            borderRadius: 1,
          }}
        />

        <Typography
          variant="h6"
          sx={{
            fontWeight: 700,
            flexGrow: 1,
          }}
        >
          Dia-Smart
        </Typography>

        <IconButton color="inherit">
          <NotificationsIcon />
        </IconButton>

        <Button
          color="inherit"
          startIcon={<HomeIcon />}
          onClick={handleHome}
          sx={{
            mx: 1,
            textTransform: "none",
            fontWeight: 600,
          }}
        >
          Home
        </Button>

        <Button
        color="inherit"
        startIcon={<LogoutIcon />}
        onClick={handleLogout}
        sx={{
          mx: 1,
          textTransform: "none",
        }}
      >
        Logout
      </Button>

        <Avatar>
          {role.charAt(0)}
        </Avatar>
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;