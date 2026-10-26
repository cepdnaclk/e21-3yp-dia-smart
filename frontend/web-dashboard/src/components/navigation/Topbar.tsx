
import {
  AppBar,
  Toolbar,
  Typography,
  Box,
  IconButton,
  Avatar,
  Button,
  Tooltip,
  Divider,
} from "@mui/material";
import { alpha } from "@mui/material/styles";
import type { Theme } from "@mui/material/styles";

import NotificationsNoneIcon from "@mui/icons-material/NotificationsNone";
import LogoutIcon from "@mui/icons-material/Logout";
import HomeOutlinedIcon from "@mui/icons-material/HomeOutlined";
import SettingsOutlinedIcon from "@mui/icons-material/SettingsOutlined";
import MenuIcon from "@mui/icons-material/Menu";
import { useNavigate } from "react-router-dom";

import { BrandIcon } from "../common/BrandLogo";
import { useAuth } from "../../context/AuthContext";
import { DEFAULT_ROLE_ROUTES } from "../../config/routes/roleRoutes";
import { brand, SIDEBAR_WIDTH } from "../../theme/theme";
import { getRoleDisplay } from "./roleMeta";

const appBarStyles = {
  backgroundColor: alpha("#ffffff", 0.88),
  backdropFilter: "saturate(180%) blur(12px)",
  color: brand.navy,
  borderBottom: `1px solid ${brand.slate200}`,
  width: { md: `calc(100% - ${SIDEBAR_WIDTH}px)` },
  ml: { md: `${SIDEBAR_WIDTH}px` },
  zIndex: (theme: Theme) => theme.zIndex.drawer + 1,
  paddingTop: "env(safe-area-inset-top, 0px)",
};

const logoStyles = {
  mr: 1.25,
};

const titleStyles = {
  fontWeight: 800,
  letterSpacing: "0.2px",
  whiteSpace: "nowrap",
  color: brand.navy,
};

const iconButtonStyles = {
  color: brand.slate600,
  border: `1px solid ${brand.slate200}`,
  borderRadius: 2.5,
  width: 40,
  height: 40,
  "&:hover": {
    backgroundColor: brand.slate100,
    color: brand.navy,
  },
};

const homeButtonStyles = {
  mx: 0.5,
  textTransform: "none",
  fontWeight: 600,
  color: brand.slate600,
  borderRadius: 2.5,
  px: 1.5,
  height: 40,
  display: { xs: "none", md: "inline-flex" },
  "&:hover": {
    backgroundColor: brand.slate100,
    color: brand.navy,
  },
};

const logoutButtonStyles = {
  textTransform: "none",
  borderRadius: 2.5,
  borderColor: brand.slate200,
  color: brand.slate700,
  fontWeight: 600,
  height: 40,
  minWidth: 40,
  px: { xs: 1, sm: 1.75 },
  "& .MuiButton-startIcon": {
    mr: { xs: 0, sm: 1 },
    ml: { xs: 0, sm: -0.5 },
  },
  "&:hover": {
    borderColor: alpha("#ef4444", 0.4),
    color: "#b91c1c",
    backgroundColor: alpha("#ef4444", 0.05),
  },
};

const settingsButtonStyles = {
  ...iconButtonStyles,
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
  const roleDisplay = getRoleDisplay(role);

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
      elevation={0}
      sx={appBarStyles}
    >
      <Toolbar sx={{ gap: { xs: 0.75, sm: 1 }, px: { xs: 1.5, sm: 3, lg: 4 } }}>
        {/* Hamburger Menu Toggle on Mobile */}
        <IconButton
          color="inherit"
          aria-label="open drawer"
          edge="start"
          onClick={onDrawerToggle}
          sx={{ mr: 0.5, display: { md: "none" } }}
        >
          <MenuIcon />
        </IconButton>

        {/* Brand (mobile / tablet) — the sidebar carries the brand on desktop */}
        <Box
          onClick={handleLogoClick}
          sx={{
            display: { xs: "flex", md: "none" },
            alignItems: "center",
            cursor: "pointer",
            flexGrow: 1,
            minWidth: 0,
            "&:hover": {
              opacity: 0.9
            }
          }}
        >
          <BrandIcon size={34} sx={logoStyles} />

          <Typography
            variant="h6"
            sx={titleStyles}
          >
            Dia-Smart
          </Typography>
        </Box>

        {/* Role context (desktop) */}
        <Box sx={{ display: { xs: "none", md: "flex" }, flexDirection: "column", flexGrow: 1, minWidth: 0 }}>
          <Typography variant="overline" sx={{ color: brand.slate400, lineHeight: 1.4, fontSize: "0.68rem" }}>
            Diabetes Care Platform
          </Typography>
          <Typography variant="subtitle2" sx={{ color: brand.navy, fontWeight: 700, lineHeight: 1.3 }} noWrap>
            {roleDisplay.portal}
          </Typography>
        </Box>

        <Tooltip title="View notifications">
          <IconButton aria-label="View notifications" sx={{ ...iconButtonStyles, ml: 1 }}>
            <NotificationsNoneIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        <Tooltip title="Go to home dashboard">
          <Button
            color="inherit"
            startIcon={<HomeOutlinedIcon />}
            onClick={handleHome}
            sx={homeButtonStyles}
          >
            Home
          </Button>
        </Tooltip>

        <Tooltip title="Edit settings">
          <IconButton
            color="inherit"
            aria-label="Settings"
            onClick={handleSettings}
            sx={settingsButtonStyles}
          >
            <SettingsOutlinedIcon fontSize="small" />
          </IconButton>
        </Tooltip>

        <Tooltip title="Logout of account">
          <Button
            variant="outlined"
            color="inherit"
            startIcon={<LogoutIcon fontSize="small" />}
            onClick={handleLogout}
            sx={logoutButtonStyles}
          >
            <Box component="span" sx={{ display: { xs: "none", sm: "inline" } }}>
              Logout
            </Box>
          </Button>
        </Tooltip>

        <Divider orientation="vertical" flexItem sx={{ mx: 0.5, my: 1.5, display: { xs: "none", sm: "block" } }} />

        <Tooltip title="View profile details">
          <IconButton
            color="inherit"
            aria-label="Profile"
            onClick={handleProfile}
            sx={{ p: 0.5 }}
          >
            <Avatar
              sx={{
                bgcolor: brand.navy,
                color: "#ffffff",
                fontWeight: 800,
                width: 36,
                height: 36,
                fontSize: "0.9rem",
                boxShadow: `0 0 0 2px #ffffff, 0 0 0 4px ${alpha(brand.sky, 0.55)}`,
              }}
            >
              {getRoleInitial(role)}
            </Avatar>
          </IconButton>
        </Tooltip>
      </Toolbar>
    </AppBar>
  );
};

export default Topbar;
