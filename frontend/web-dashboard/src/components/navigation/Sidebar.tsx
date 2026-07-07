import {
  Drawer,
  List,
  ListItemButton,
  ListItemText,
} from "@mui/material";

import { useNavigate, useLocation } from "react-router-dom";

import { patientNavigation } from "../../config/navigation/patientNavigation";
import { doctorNavigation } from "../../config/navigation/doctorNavigation";
import { caregiverNavigation } from "../../config/navigation/caregiverNavigation";
import { adminNavigation } from "../../config/navigation/adminNavigation";
import type { NavigationItem } from "../../config/navigation/navigationTypes";
import { useAuth } from "../../context/AuthContext";
import { UserRole } from "../../types/roles";

const drawerWidth = 240;

const drawerStyles = {
  width: drawerWidth,
  flexShrink: 0,
  "& .MuiDrawer-paper": {
    width: drawerWidth,
    boxSizing: "border-box",
  },
};

const listItemTextStyles = {
  ml: 2,
};

const isActiveNavigationItem = (
  item: NavigationItem,
  pathname: string
) => pathname === item.route;

const Sidebar = () => {
  const navigate = useNavigate();
  const location = useLocation();
  const { role } = useAuth();

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

  const navItems = getNavigationItems();

  return (
    <Drawer
      variant="permanent"
      sx={drawerStyles}
    >
      {/* TODO: Add temporary/mobile drawer behavior here when responsive navigation is implemented. */}
      <List>
        {navItems.map((item) => {
          const Icon = item.icon;

          return (
            <ListItemButton
              key={item.id}
              selected={isActiveNavigationItem(
                item,
                location.pathname
              )}
              onClick={() => navigate(item.route)}
            >
              <Icon />

              <ListItemText
                primary={item.label}
                sx={listItemTextStyles}
              />
            </ListItemButton>
          );
        })}
      </List>
    </Drawer>
  );
};

export default Sidebar;
