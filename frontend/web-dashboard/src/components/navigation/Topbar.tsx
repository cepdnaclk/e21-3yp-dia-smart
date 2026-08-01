import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  IconButton,
  Avatar,
  Button,
  Tooltip,
} from "@mui/material";

import NotificationsIcon from "@mui/icons-material/Notifications";
import LogoutIcon from "@mui/icons-material/Logout";
import HomeIcon from "@mui/icons-material/Home";
import SettingsIcon from "@mui/icons-material/Settings";
import MenuIcon from "@mui/icons-material/Menu";
import { useNavigate } from "react-router-dom";

import logo from "../../assets/logo/diasmart-logo.png";
import { useAuth } from "../../context/AuthContext";
import { DEFAULT_ROLE_ROUTES } from "../../config/routes/roleRoutes";

const appBarStyles = {
  backgroundColor: "#12233b",
  zIndex: (theme: any) => theme.zIndex.drawer + 1,
  paddingTop: "env(safe-area-inset-top, 0px)",
};

const logoStyles = {
  width: 32,
  height: 32,
  mr: 1,
};

const titleStyles = {
  flexGrow: 1,
  fontWeight: 700,
  letterSpacing: "0.5px",
  whiteSpace: "nowrap",
};

const homeButtonStyles = {
  mx: 0.5,
  textTransform: "none",
  fontWeight: 600,
  display: { xs: "none", md: "inline-flex" },
};

const logoutButtonStyles = {
  textTransform: "none",
  borderRadius: 2,
  borderColor: "rgba(255, 255, 255, 0.3)",
  color: "#ffffff",
  fontWeight: 600,
  "&:hover": {
    borderColor: "#ffffff",
    backgroundColor: "rgba(255, 255, 255, 0.08)",
  },
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
  const navigate = useNavigate();
  const { role, logout } = useAuth();

  const handleLogout = () => {
    logout();
    navigate("/login");
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

  const handleLogoClick = () => {
    const targetRoute = DEFAULT_ROLE_ROUTES[role] || "/dashboard";
    navigate(targetRoute);
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
          onClick={handleLogoClick}
          sx={{
            display: "flex",
            alignItems: "center",
            cursor: "pointer",
            flexGrow: 1,
            "&:hover": {
              opacity: 0.95
            }
          }}
        >
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
        </Box>

        <Tooltip title="View notifications">
          <IconButton color="inherit" sx={{ ml: 3, mr: 0.25 }}>
            <NotificationsIcon />
          </IconButton>
        </Tooltip>

        <Tooltip title="Go to home dashboard">
          <Button
            color="inherit"
            startIcon={<HomeIcon />}
            onClick={handleHome}
            sx={homeButtonStyles}
          >
            Home
          </Button>
        </Tooltip>

        <Tooltip title="Logout of account">
          <Button
            color="inherit"
            startIcon={<LogoutIcon />}
            onClick={handleLogout}
            sx={{ ...logoutButtonStyles, ml: 0.5, mr: 0.25 }}
          >
            <Box component="span" sx={{ display: { xs: "none", sm: "inline" } }}>
              Logout
            </Box>
          </Button>
        </Tooltip>

        <Tooltip title="Edit settings">
          <IconButton
            color="inherit"
            aria-label="Settings"
            onClick={handleSettings}
            sx={settingsButtonStyles}
          >
            <SettingsIcon />
          </IconButton>
        </Tooltip>

        <Tooltip title="View profile details">
          <IconButton
            color="inherit"
            aria-label="Profile"
            onClick={handleProfile}
            sx={{ ml: 0.25 }}
          >
            <Avatar sx={{ bgcolor: "#3ec1fa", color: "#12233b", fontWeight: "bold", width: 32, height: 32, fontSize: "0.9rem" }}>
              {getRoleInitial(role)}
            </Avatar>
          </IconButton>
        </Tooltip>
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;

