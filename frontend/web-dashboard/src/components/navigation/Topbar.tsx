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
import MenuIcon from "@mui/icons-material/Menu";
import { useNavigate } from "react-router-dom";

import logo from "../../assets/logo/diasmart-logo.png";
import { useAuth } from "../../context/AuthContext";

const appBarStyles = {
  backgroundColor: "#12233b",
  zIndex: (theme: any) => theme.zIndex.drawer + 1,
};

const logoStyles = {
  width: 36,
  height: 36,
  mr: 1.5,
  borderRadius: 1,
  display: { xs: "none", sm: "block" },
};

const titleStyles = {
  fontWeight: 700,
  flexGrow: 1,
};

const homeButtonStyles = {
  mx: 0.5,
  textTransform: "none",
  fontWeight: 600,
  display: { xs: "none", md: "inline-flex" },
};

const logoutButtonStyles = {
  mx: 0.5,
  textTransform: "none",
  display: { xs: "none", md: "inline-flex" },
};

const settingsButtonStyles = {
  display: { xs: "none", sm: "inline-flex" },
};

const getRoleInitial = (role: string) =>
  role.charAt(0).toUpperCase();

interface TopbarProps {
  onDrawerToggle: () => void;
}

const Topbar = ({ onDrawerToggle }: TopbarProps) => {
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
      <Toolbar>
        {/* Hamburger Menu Toggle on Mobile */}
        <IconButton
          color="inherit"
          aria-label="open drawer"
          edge="start"
          onClick={onDrawerToggle}
          sx={{ mr: 2, display: { md: "none" } }}
        >
          <MenuIcon />
        </IconButton>

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

        <IconButton color="inherit" sx={{ mr: 1 }}>
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
          sx={settingsButtonStyles}
        >
          <SettingsIcon />
        </IconButton>

        <IconButton
          color="inherit"
          aria-label="Profile"
          onClick={handleProfile}
          sx={{ ml: 1 }}
        >
          <Avatar sx={{ bgcolor: "#3ec1fa", color: "#12233b", fontWeight: "bold", width: 32, height: 32, fontSize: "0.9rem" }}>
            {getRoleInitial(role)}
          </Avatar>
        </IconButton>
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;

