import { BottomNavigation, BottomNavigationAction, Paper, Tooltip } from "@mui/material";
import { useNavigate, useLocation } from "react-router-dom";
import DashboardIcon from "@mui/icons-material/Dashboard";
import WarningIcon from "@mui/icons-material/Warning";
import AnalyticsIcon from "@mui/icons-material/Analytics";
import PeopleIcon from "@mui/icons-material/People";
import MenuIcon from "@mui/icons-material/Menu";
import ConstructionIcon from "@mui/icons-material/Construction";

import { useAuth } from "../../context/AuthContext";
import { UserRole } from "../../types/roles";

interface BottomNavProps {
  onDrawerToggle: () => void;
}

const BottomNav = ({ onDrawerToggle }: BottomNavProps) => {
  const navigate = useNavigate();
  const location = useLocation();
  const { role } = useAuth();

  // Define Bottom Nav actions based on user roles (max 4 icons, plus Menu for the rest)
  const getBottomNavItems = () => {
    switch (role) {
      case UserRole.CAREGIVER:
        return [
          { label: "Dashboard", icon: <DashboardIcon />, route: "/caregiver/dashboard" },
          { label: "Patients", icon: <PeopleIcon />, route: "/caregiver/patients" },
          { label: "Alerts", icon: <WarningIcon />, route: "/alerts" },
        ];
      case UserRole.DOCTOR:
        return [
          { label: "Dashboard", icon: <DashboardIcon />, route: "/doctor/dashboard" },
          { label: "Patients", icon: <PeopleIcon />, route: "/doctor/patients" },
          { label: "Reports", icon: <AnalyticsIcon />, route: "/doctor/reports" },
        ];
      case UserRole.ADMIN:
        return [
          { label: "Dashboard", icon: <DashboardIcon />, route: "/admin/dashboard" },
          { label: "Users", icon: <PeopleIcon />, route: "/admin/users" },
          { label: "Devices", icon: <ConstructionIcon />, route: "/admin/devices" },
        ];
      case UserRole.PATIENT:
      default:
        return [
          { label: "Dashboard", icon: <DashboardIcon />, route: "/dashboard" },
          { label: "Alerts", icon: <WarningIcon />, route: "/alerts" },
          { label: "Analytics", icon: <AnalyticsIcon />, route: "/analytics" },
        ];
    }
  };

  const navItems = getBottomNavItems();

  // Determine active index based on route
  const getActiveIndex = () => {
    const index = navItems.findIndex((item) => location.pathname === item.route);
    return index !== -1 ? index : 0;
  };

  const handleNavChange = (_event: React.SyntheticEvent, newValue: number) => {
    if (newValue === navItems.length) {
      // Toggle Drawer for "More"
      onDrawerToggle();
    } else {
      navigate(navItems[newValue].route);
    }
  };

  return (
    <Paper
      sx={{
        position: "fixed",
        bottom: 0,
        left: 0,
        right: 0,
        zIndex: 1000,
        borderRadius: 0,
        borderTop: "1px solid #e2e8f0",
      }}
      elevation={3}
    >
      <BottomNavigation
        showLabels
        value={getActiveIndex()}
        onChange={handleNavChange}
        sx={{
          height: 64,
          backgroundColor: "#12233b",
          "& .MuiBottomNavigationAction-root": {
            color: "rgba(255, 255, 255, 0.6)",
            minWidth: 0,
            padding: "6px 0",
          },
          "& .MuiBottomNavigationAction-root.Mui-selected": {
            color: "#3ec1fa",
          },
        }}
      >
        {navItems.map((item) => (
          <Tooltip key={item.label} title={`Navigate to ${item.label}`}>
            <BottomNavigationAction
              label={item.label}
              icon={item.icon}
            />
          </Tooltip>
        ))}
        {/* The last button triggers the sidebar drawer on mobile for less-frequently used paths */}
        <Tooltip title="Open menu drawer">
          <BottomNavigationAction
            label="Menu"
            icon={<MenuIcon />}
            onClick={onDrawerToggle}
          />
        </Tooltip>
      </BottomNavigation>
    </Paper>
  );
};

export default BottomNav;
