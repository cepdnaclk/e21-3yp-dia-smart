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
} from "@mui/material";

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
import logo from "../../assets/logo/diasmart-logo.png";

import { DEFAULT_ROLE_ROUTES } from "../../config/routes/roleRoutes";

const drawerWidth = 260;

interface SidebarProps {
  mobileOpen: boolean;
  onClose: () => void;
}

const isActiveNavigationItem = (
  item: NavigationItem,
  pathname: string
) => pathname === item.route;

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

  const drawerContent = (
    <Box sx={{ display: "flex", flexDirection: "column", height: "100%", backgroundColor: "#12233b" }}>
      {/* Drawer Header / Logo */}
      <Toolbar
        onClick={handleLogoClick}
        sx={{
          display: "flex",
          alignItems: "center",
          gap: 1.5,
          px: 2,
          pt: "env(safe-area-inset-top, 0px)",
          cursor: "pointer",
          "&:hover": {
            opacity: 0.95
          }
        }}
      >
        <Box
          component="img"
          src={logo}
          alt="Dia-Smart Logo"
          sx={{ width: 36, height: 36, borderRadius: 1 }}
        />
        <Typography
          variant="h6"
          sx={{
            fontWeight: 700,
            color: "#ffffff",
            letterSpacing: "0.5px",
          }}
        >
          Dia-Smart
        </Typography>
      </Toolbar>
      <Divider sx={{ borderColor: "rgba(255, 255, 255, 0.08)", mb: 1 }} />

      {/* Navigation List */}
      <List sx={{ px: 1, flexGrow: 1 }}>
        {navItems.map((item) => {
          const Icon = item.icon;
          const active = isActiveNavigationItem(item, location.pathname);

          return (
            <ListItemButton
              key={item.id}
              selected={active}
              onClick={() => {
                navigate(item.route);
                if (isMobile) onClose();
              }}
              sx={{
                borderRadius: 2,
                mb: 0.5,
                py: 1.25,
                px: 2,
                color: "rgba(255, 255, 255, 0.7)",
                "&.Mui-selected": {
                  backgroundColor: "rgba(62, 193, 250, 0.12)",
                  color: "#3ec1fa",
                  "& .MuiListItemIcon-root": {
                    color: "#3ec1fa",
                  },
                  "&:hover": {
                    backgroundColor: "rgba(62, 193, 250, 0.18)",
                  },
                },
                "&:hover": {
                  backgroundColor: "rgba(255, 255, 255, 0.04)",
                  color: "#ffffff",
                  "& .MuiListItemIcon-root": {
                    color: "#ffffff",
                  },
                },
              }}
            >
              <ListItemIcon sx={{ color: "inherit", minWidth: 36 }}>
                <Icon />
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

        <Divider sx={{ borderColor: "rgba(255, 255, 255, 0.08)", my: 1.5 }} />

        <ListItemButton
          onClick={() => {
            navigate("/settings");
            if (isMobile) onClose();
          }}
          sx={{
            borderRadius: 2,
            mb: 0.5,
            py: 1.25,
            px: 2,
            color: "rgba(255, 255, 255, 0.7)",
            "&:hover": {
              backgroundColor: "rgba(255, 255, 255, 0.04)",
              color: "#ffffff",
              "& .MuiListItemIcon-root": {
                color: "#ffffff",
              },
            },
          }}
        >
          <ListItemIcon sx={{ color: "inherit", minWidth: 36 }}>
            <SettingsIcon />
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
            borderRadius: 2,
            mb: 0.5,
            py: 1.25,
            px: 2,
            color: "rgba(255, 255, 255, 0.7)",
            "&:hover": {
              backgroundColor: "rgba(255, 255, 255, 0.04)",
              color: "#ffffff",
              "& .MuiListItemIcon-root": {
                color: "#ffffff",
              },
            },
          }}
        >
          <ListItemIcon sx={{ color: "inherit", minWidth: 36 }}>
            <LogoutIcon />
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
      <Box sx={{ p: 2, textAlign: "center" }}>
        <Typography variant="caption" sx={{ color: "rgba(255,255,255,0.4)" }}>
          Version 1.0.0
        </Typography>
      </Box>
    </Box>
  );

  return (
    <Box
      component="nav"
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
            borderRight: "1px solid rgba(255, 255, 255, 0.08)",
          },
        }}
      >
        {drawerContent}
      </Drawer>
    </Box>
  );
};

export default Sidebar;

