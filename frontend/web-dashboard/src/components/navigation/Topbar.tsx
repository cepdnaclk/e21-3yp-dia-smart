import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  IconButton,
  Avatar,
  Button,
} from "@mui/material";

import NotificationsIcon from "@mui/icons-material/Notifications";
import LogoutIcon from "@mui/icons-material/Logout";
import HomeIcon from "@mui/icons-material/Home";
import SettingsIcon from "@mui/icons-material/Settings";
import { useNavigate } from "react-router-dom";

import logo from "../../assets/logo/diasmart-logo.png";

import { useAuth } from "../../context/AuthContext";

const appBarStyles = {
  backgroundColor: "#3B567C",
};

const logoStyles = {
  width: 40,
  height: 40,
  mr: 2,
  borderRadius: 1,
};

const titleStyles = {
  fontWeight: 700,
  flexGrow: 1,
};

const homeButtonStyles = {
  mx: 1,
  textTransform: "none",
  fontWeight: 600,
};

const logoutButtonStyles = {
  mx: 1,
  textTransform: "none",
};

const getRoleInitial = (role: string) =>
  role.charAt(0);

const Topbar = () => {
  const { role, logout } = useAuth();
  const navigate = useNavigate();

  const handleLogout = () => {
    logout();
    navigate("/");
  };

  const handleHome = () => {
    navigate("/");
  };

  const handleSettings = () => {
    navigate("/settings");
  };

  const handleProfile = () => {
    navigate("/profile");
  };

  return (
    <AppBar
      position="fixed"
      sx={appBarStyles}
    >
      {/* TODO: Add mobile menu trigger here when responsive drawer navigation is implemented. */}
      <Toolbar>
        <Box
          component="img"
          src={logo}
          alt="Dia-Smart Logo"
          sx={logoStyles}
        />

        <Typography
          variant="h6"
          sx={titleStyles}
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
          sx={homeButtonStyles}
        >
          Home
        </Button>

        <Button
          color="inherit"
          startIcon={<LogoutIcon />}
          onClick={handleLogout}
          sx={logoutButtonStyles}
        >
          Logout
        </Button>

        <IconButton
          color="inherit"
          aria-label="Settings"
          onClick={handleSettings}
        >
          <SettingsIcon />
        </IconButton>

        <IconButton
          color="inherit"
          aria-label="Profile"
          onClick={handleProfile}
        >
          <Avatar>
            {getRoleInitial(role)}
          </Avatar>
        </IconButton>
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;
