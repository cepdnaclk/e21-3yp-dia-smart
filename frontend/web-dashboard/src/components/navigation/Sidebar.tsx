
import {
  Drawer,
  List,
  ListItemButton,
  ListItemText,
  ListItemIcon,
  Toolbar,
  useTheme,
  useMediaQuery,
  Box,
  Typography,
  Divider,
  Avatar,
} from "@mui/material";
import { alpha } from "@mui/material/styles";

import { useNavigate, useLocation } from "react-router-dom";
import SettingsIcon from "@mui/icons-material/Settings";
import LogoutIcon from "@mui/icons-material/Logout";

import { patientNavigation } from "../../config/navigation/patientNavigation";
import { doctorNavigation } from "../../config/navigation/doctorNavigation";
import { caregiverNavigation } from "../../config/navigation/caregiverNavigation";
import { adminNavigation } from "../../config/navigation/adminNavigation";
import type { NavigationItem } from "../../config/navigation/navigationTypes";
import { useAuth } from "../../context/AuthContext";
import { UserRole } from "../../types/roles";
import { BrandIcon } from "../common/BrandLogo";
import { brand, SIDEBAR_WIDTH } from "../../theme/theme";
import { getRoleDisplay } from "./roleMeta";

import { DEFAULT_ROLE_ROUTES } from "../../config/routes/roleRoutes";

const drawerWidth = SIDEBAR_WIDTH;

interface SidebarProps {
  mobileOpen: boolean;
  onClose: () => void;
}

const isActiveNavigationItem = (
  item: NavigationItem,
  pathname: string
) => pathname === item.route;

const sectionLabelStyles = {
  px: 2,
  pt: 1,
  pb: 1,
  display: "block",
  color: "rgba(255, 255, 255, 0.42)",
  fontSize: "0.68rem",
};

const navItemStyles = {
  position: "relative",
  borderRadius: 2,
  mb: 0.5,
  py: 1.1,
  px: 1.75,
  color: "rgba(255, 255, 255, 0.72)",
  transition: "background-color 0.15s ease, color 0.15s ease",
  "& .MuiListItemIcon-root": {
    color: "rgba(255, 255, 255, 0.6)",
    transition: "color 0.15s ease",
  },
  "&.Mui-selected": {
    background: `linear-gradient(90deg, ${alpha(brand.sky, 0.2)} 0%, ${alpha(brand.sky, 0.08)} 100%)`,
    color: "#ffffff",
    "& .MuiListItemIcon-root": {
      color: brand.sky,
    },
    "&::before": {
      content: '""',
      position: "absolute",
      left: -8,
      top: 8,
      bottom: 8,
      width: 3,
      borderRadius: "0 3px 3px 0",
      backgroundColor: brand.sky,
    },
    "&:hover": {
      backgroundColor: alpha(brand.sky, 0.22),
    },
  },
  "&:hover": {
    backgroundColor: "rgba(255, 255, 255, 0.06)",
    color: "#ffffff",
    "& .MuiListItemIcon-root": {
      color: "#ffffff",
    },
  },
  "&.Mui-focusVisible": {
    backgroundColor: "rgba(255, 255, 255, 0.08)",
    boxShadow: `inset 0 0 0 2px ${alpha(brand.sky, 0.6)}`,
  },
};

const Sidebar = ({ mobileOpen, onClose }: SidebarProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { role, logout } = useAuth();
  const theme = useTheme();
  const isMobile = useMediaQuery(theme.breakpoints.down("md"));

  const getNavigationItems = (): NavigationItem[] => {
    switch (role) {
      case UserRole.DOCTOR:
        return doctorNavigation;
      case UserRole.CAREGIVER:
        return caregiverNavigation;
      case UserRole.ADMIN:
        return adminNavigation;
      case UserRole.PATIENT:
      default:
        return patientNavigation;
    }
  };

  const handleLogoClick = () => {
    const targetRoute = DEFAULT_ROLE_ROUTES[role] || "/dashboard";
    navigate(targetRoute);
    if (isMobile) onClose();
  };

  const navItems = getNavigationItems();
  const roleDisplay = getRoleDisplay(role);

  const drawerContent = (
    <Box
      sx={{
        display: "flex",
        flexDirection: "column",
        height: "100%",
        background: `linear-gradient(180deg, ${brand.navy} 0%, ${brand.navyDark} 100%)`,
      }}
    >
      {/* Drawer Header / Logo */}
      <Toolbar
        onClick={handleLogoClick}
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          px: 2.5,
          pt: "env(safe-area-inset-top, 0px)",
          cursor: "pointer",
          "&:hover": {
            opacity: 0.92
          }
        }}
      >
        <BrandIcon size={38} tile />
        <Box sx={{ minWidth: 0 }}>
          <Typography
            variant="h6"
            sx={{
              fontWeight: 800,
              color: "#ffffff",
              letterSpacing: "0.2px",
              lineHeight: 1.2,
            }}
          >
            Dia-Smart
          </Typography>
          <Typography
            variant="caption"
            sx={{ color: alpha(brand.sky, 0.85), fontWeight: 600, display: "block", lineHeight: 1.2 }}
          >
            Smart Insulin Care
          </Typography>
        </Box>
      </Toolbar>

      {/* Signed-in role identity */}
      <Box
        sx={{
          mx: 2,
          mt: 1,
          mb: 2,
          p: 1.5,
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          borderRadius: 2.5,
          backgroundColor: "rgba(255, 255, 255, 0.05)",
          border: "1px solid rgba(255, 255, 255, 0.08)",
        }}
      >
        <Avatar
          sx={{
            width: 34,
            height: 34,
            fontSize: "0.85rem",
            fontWeight: 800,
            bgcolor: brand.sky,
            color: brand.navy,
          }}
        >
          {roleDisplay.title.charAt(0)}
        </Avatar>
        <Box sx={{ minWidth: 0 }}>
          <Typography variant="body2" sx={{ color: "#ffffff", fontWeight: 700, lineHeight: 1.25 }} noWrap>
            {roleDisplay.portal}
          </Typography>
          <Box sx={{ display: "flex", alignItems: "center", gap: 0.75 }}>
            <Box sx={{ width: 6, height: 6, borderRadius: "50%", bgcolor: "success.main" }} />
            <Typography variant="caption" sx={{ color: "rgba(255, 255, 255, 0.55)", fontWeight: 500 }} noWrap>
              Signed in as {roleDisplay.title}
            </Typography>
          </Box>
        </Box>
      </Box>

      {/* Navigation List */}
      <List sx={{ px: 1.5, py: 0, flexGrow: 1, overflowY: "auto" }}>
        <Typography variant="overline" sx={sectionLabelStyles}>
          Menu
        </Typography>

        {navItems.map((item) => {
          const Icon = item.icon;
          const active = isActiveNavigationItem(item, location.pathname);

          return (
            <ListItemButton
              key={item.id}
              selected={active}
              aria-current={active ? "page" : undefined}
              onClick={() => {
                navigate(item.route);
                if (isMobile) onClose();
              }}
              sx={navItemStyles}
            >
              <ListItemIcon sx={{ minWidth: 36 }}>
                <Icon sx={{ fontSize: 21 }} />
              </ListItemIcon>

              <ListItemText
                primary={
                  <Typography variant="body2" sx={{ fontSize: "0.9rem", fontWeight: active ? 700 : 500 }}>
                    {item.label}
                  </Typography>
                }
              />
            </ListItemButton>
          );
        })}

        <Divider sx={{ borderColor: "rgba(255, 255, 255, 0.08)", my: 1.5, mx: 1 }} />

        <Typography variant="overline" sx={sectionLabelStyles}>
          Account
        </Typography>

        <ListItemButton
          selected={location.pathname === "/settings"}
          onClick={() => {
            navigate("/settings");
            if (isMobile) onClose();
          }}
          sx={navItemStyles}
        >
          <ListItemIcon sx={{ minWidth: 36 }}>
            <SettingsIcon sx={{ fontSize: 21 }} />
          </ListItemIcon>
          <ListItemText
            primary={
              <Typography variant="body2" sx={{ fontSize: "0.9rem", fontWeight: 500 }}>
                Settings
              </Typography>
            }
          />
        </ListItemButton>

        <ListItemButton
          onClick={() => {
            logout();
            navigate("/");
            if (isMobile) onClose();
          }}
          sx={{
            ...navItemStyles,
            "&:hover": {
              backgroundColor: alpha("#ef4444", 0.12),
              color: "#fecaca",
              "& .MuiListItemIcon-root": {
                color: "#fca5a5",
              },
            },
          }}
        >
          <ListItemIcon sx={{ minWidth: 36 }}>
            <LogoutIcon sx={{ fontSize: 21 }} />
          </ListItemIcon>
          <ListItemText
            primary={
              <Typography variant="body2" sx={{ fontSize: "0.9rem", fontWeight: 500 }}>
                Logout
              </Typography>
            }
          />
        </ListItemButton>
      </List>

      {/* Optional footer area in the drawer */}
      <Box
        sx={{
          px: 2.5,
          py: 2,
          pb: "calc(16px + env(safe-area-inset-bottom, 0px))",
          borderTop: "1px solid rgba(255, 255, 255, 0.06)",
        }}
      >
        <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.4)" }}>
          Version 1.0.0
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box
      component="nav"
      aria-label="Main navigation"
      sx={{ width: { md: drawerWidth }, flexShrink: { md: 0 } }}
    >
      {/* Mobile temporary drawer */}
      <Drawer
        variant="temporary"
        open={mobileOpen}
        onClose={onClose}
        ModalProps={{ keepMounted: true }} // Better open performance on mobile
        sx={{
          display: { xs: "block", md: "none" },
          "& .MuiDrawer-paper": {
            width: drawerWidth,
            boxSizing: "border-box",
            borderRight: "none",
            backgroundColor: brand.navy,
          },
        }}
      >
        {drawerContent}
      </Drawer>

      {/* Desktop permanent drawer */}
      <Drawer
        variant="permanent"
        open
        sx={{
          display: { xs: "none", md: "block" },
          "& .MuiDrawer-paper": {
            width: drawerWidth,
            boxSizing: "border-box",
            borderRight: "none",
            backgroundColor: brand.navy,
          },
        }}
      >
        {drawerContent}
      </Drawer>
    </Box>
  );
};

export default Sidebar;
